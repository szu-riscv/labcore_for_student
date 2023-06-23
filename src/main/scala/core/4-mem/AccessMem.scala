package core

import chisel3._
import chisel3.util._

import bus._
import utils._
import config.Config._

object MemOpType {
    // bits(3): 0 load, 1 store
    // bits(2): 0 Signed, 1 Unsigned
    // bit(1, 0): size
    def lb  = "b0000".U
    def lh  = "b0001".U
    def lw  = "b0010".U
    def ld  = "b0011".U
    def lbu = "b0100".U
    def lhu = "b0101".U
    def lwu = "b0110".U
    def sb  = "b1000".U
    def sh  = "b1001".U
    def sw  = "b1010".U
    def sd  = "b1011".U

    def no                      = "b0000".U
    def apply()                 = UInt(4.W)
    def isLoad(op: UInt): Bool  = !op(3)
    def isStore(op: UInt): Bool = op(3)
    def genSize(op: UInt): UInt = op(1, 0)
    def genMask(addr: UInt, size: UInt): UInt = {
        val wmask    = Wire(Vec(8, Bool()))
        val numValid = 1.U << size
        wmask.zipWithIndex.map { case (m, i) =>
            m := addr(2, 0) <= i.U && addr(2, 0) + numValid.asUInt() > i.U
        }
        wmask.asUInt()
    }
    def WriteData(addr: UInt, rs2: UInt): UInt = {
        val shiftwdata = Wire(UInt(XLEN.W))
        shiftwdata := MuxLookup(
            addr(2, 0),
            rs2,
            List(
                "b001".U -> Cat(rs2(XLEN - 9, 0), Fill(8, "b0".U)),
                "b010".U -> Cat(rs2(XLEN - 17, 0), Fill(16, "b0".U)),
                "b011".U -> Cat(rs2(XLEN - 25, 0), Fill(24, "b0".U)),
                "b100".U -> Cat(rs2(XLEN - 33, 0), Fill(32, "b0".U)),
                "b101".U -> Cat(rs2(XLEN - 41, 0), Fill(40, "b0".U)),
                "b110".U -> Cat(rs2(XLEN - 49, 0), Fill(48, "b0".U)),
                "b111".U -> Cat(rs2(XLEN - 57, 0), Fill(56, "b0".U))
            )
        )
        // shiftwdata := wdata << (addr(2, 0) << 3)
        shiftwdata
    }
    def genWdata(addr: UInt, wdata: UInt, size: UInt): UInt = {
        val partdata = MuxCase(
            wdata(XLEN - 1, 0),
            Array(
                (size === "b11".U) -> wdata(63, 0),
                (size === "b10".U) -> wdata(31, 0),
                (size === "b01".U) -> wdata(15, 0),
                (size === "b00".U) -> wdata(7, 0)
            )
        ).asUInt()
        val shiftwdata = Wire(UInt(XLEN.W))
        shiftwdata := partdata << (addr(2, 0) << 3)
        shiftwdata
    }

    def genWriteBackData(read_words: UInt, mem_op: UInt, addr_offset: UInt): UInt = {
        val data_sel  = read_words >> (addr_offset << 3)
        val load_data = WireDefault(0.U(XLEN.W))
        MuxCase(
            load_data,
            Array(
                (mem_op === lb) -> Cat(Fill(XLEN - 8, data_sel(7)), data_sel(7, 0)),
                (mem_op === lh) -> Cat(Fill(XLEN - 16, data_sel(15)), data_sel(15, 0)),
                (mem_op === lw) -> Cat(Fill(XLEN - 32, data_sel(31)), data_sel(31, 0)),
                (mem_op === lbu) -> Cat(Fill(XLEN - 8, 0.U), data_sel(7, 0)),
                (mem_op === lhu) -> Cat(Fill(XLEN - 16, 0.U), data_sel(15, 0)),
                (mem_op === lwu) -> Cat(Fill(XLEN - 32, 0.U), data_sel(31, 0))
            )
        )
    }
}

class AccessMem extends Module {
    val io = IO(new Bundle() {
        val in       = Flipped(DecoupledIO(new ExuOutput))
        val out      = DecoupledIO(new WriteBackIO)
        val dmem     = MasterCpuLinkBus()
        val redirect = Input(Bool())
    })

    val (valid, mem_en, mem_op, exe_result) =
        (io.in.valid, io.in.bits.mem_en.asBool, io.in.bits.mem_op, io.in.bits.exe_result)

    io.in.ready := (!mem_en) || (mem_en && io.dmem.resp.valid)

    val req_addr = exe_result
    val isStore  = MemOpType.isStore(mem_op)
    val addrAligned = LookupTree(
        mem_op(1, 0),
        List(
            "b00".U -> true.B,                   // sb,lb
            "b01".U -> (req_addr(0) === 0.U),    // sh,lh
            "b10".U -> (req_addr(1, 0) === 0.U), // sw,lw
            "b11".U -> (req_addr(2, 0) === 0.U)  // sd,ld
        )
    )

    io.out.bits.exception(ExceptionCode.LoadAddrMisaligned)  := !isStore && !addrAligned
    io.out.bits.exception(ExceptionCode.StoreAddrMisaligned) := isStore && !addrAligned

    // load or store req
    io.dmem.req.valid     := valid && mem_en.asBool() && !io.redirect
    io.dmem.req.bits.addr := req_addr
    io.dmem.req.bits.cmd  := Mux(isStore, CpuLinkCmd.store_req, CpuLinkCmd.load_req)
    io.dmem.req.bits.wdata := Mux(
        isStore,
        MemOpType.WriteData(addr = req_addr, rs2 = io.in.bits.rs2_data),
        0.U
    )
    io.dmem.req.bits.strb := MemOpType.genMask(addr = req_addr, size = MemOpType.genSize(mem_op))
    io.dmem.req.bits.size := MemOpType.genSize(op = mem_op)

    //  resp
    io.dmem.resp.ready := true.B
    val load_data_raw = io.dmem.resp.bits.data

    val load_data_sel = MemOpType.genWriteBackData(
        read_words = load_data_raw,
        mem_op = mem_op,
        addr_offset = req_addr(2, 0)
    )
    val wb_data =
        Mux(mem_en, Mux(mem_op === MemOpType.ld, load_data_raw, load_data_sel), exe_result)

    // todo: to add mmu
    val isMMIO = WireInit(false.B)
    isMMIO := (req_addr < "h8000_0000".U) && (req_addr >= "h4060_0000".U) && io.dmem.req.valid

    io.in.ready := io.out.ready

    io.out.valid           := valid && ((!mem_en) || (mem_en && io.dmem.resp.valid))
    io.out.bits.pc         := io.in.bits.pc
    io.out.bits.instr      := io.in.bits.instr
    io.out.bits.exception  := io.in.bits.exception
    io.out.bits.instr_type := io.in.bits.instr_type
    io.out.bits.wb_addr    := io.in.bits.wb_addr
    io.out.bits.rf_wen     := io.in.bits.rf_wen
    io.out.bits.wb_data    := wb_data
    io.out.bits.redirect   := io.in.bits.redirect
    io.out.bits.fu_type    := io.in.bits.fu_type
    io.out.bits.csr_data   := io.in.bits.wb_csr
    io.out.bits.isMMIO     := isMMIO
}
