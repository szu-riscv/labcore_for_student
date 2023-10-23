package core

import chisel3._
import chisel3.util._

import utils.GTimer
import config.Config._

object InstrType {
    val illegal = "b0000".U
    val i       = "b0001".U
    val s       = "b0010".U
    val u       = "b0011".U
    val b       = "b0100".U
    val j       = "b0101".U
    val r       = "b0110".U
    val z       = "b0111".U
    val trap    = "b1000".U

    def width   = 4
    def apply() = UInt(width.W)

    def genImm(instr: UInt, instr_type: UInt): UInt = {
        // imm
        val imm_i = instr(31, 20)
        val imm_s = Cat(instr(31, 25), instr(11, 7))
        val imm_b = Cat(instr(31), instr(7), instr(30, 25), instr(11, 8))
        val imm_u = instr(31, 12)
        val imm_j = Cat(instr(31), instr(19, 12), instr(20), instr(30, 21))
        val imm_z = Cat(Fill(XLEN - 5, 0.U), instr(19, 15))
        // output sign-extend immediates
        val imm_i_sext = Cat(Fill(XLEN - 12, imm_i(11)), imm_i)
        val imm_s_sext = Cat(Fill(XLEN - 12, imm_s(11)), imm_s)
        val imm_b_sext = Cat(Fill(XLEN - 13, imm_b(11)), imm_b, 0.U)
        val imm_u_sext = Cat(Fill(XLEN - 32, imm_u(19)), imm_u, Fill(12, 0.U))
        val imm_j_sext = Cat(Fill(XLEN - 21, instr(31)), imm_j, 0.U)

        MuxCase(
            0.U,
            Array(
                (instr_type === InstrType.i) -> imm_i_sext,
                (instr_type === InstrType.s) -> imm_s_sext,
                (instr_type === InstrType.b) -> imm_b_sext,
                (instr_type === InstrType.u) -> imm_u_sext,
                (instr_type === InstrType.j) -> imm_j_sext
            )
        )
    }
}

object JBUType {
    // Branch Type
    def none = "b0000".U // Next
    def bne  = "b0001".U // Branch on NotEqual
    def beq  = "b0010".U // Branch on Equal
    def bge  = "b0011".U // Branch on Greater/Equal
    def bgeu = "b0100".U // Branch on Greater/Equal Unsigned
    def blt  = "b0101".U // Branch on Less Than
    def bltu = "b0110".U // Branch on Less Than Unsigned
    def jal  = "b1000".U // Jump
    def jalr = "b1001".U // Jump Register

    def apply()                 = UInt(4.W)
    def isJump(jb_type: UInt)   = jb_type(3)
    def isJalr(jb_type: UInt)   = jb_type(0)
    def isBranch(jb_type: UInt) = !jb_type(3) && jb_type =/= JBUType.none
}

object SrcType {
    def width = 2
    val none = 0.U(width.W)
    val reg  = 1.U(width.W)
    val pc   = 2.U(width.W)
    val imm  = 3.U(width.W)

    def apply() = UInt(width.W)
}

object InstValid {
    val N = 0.U(1.W)
    val Y = 1.U(1.W)
}

object FuType {
    def width = 2
    val alu = 0.U
    val jbu = 1.U
    val csr = 2.U
    val mdu = 3.U

    def apply()              = UInt(width.W)
    def isALU(fu_type: UInt) = fu_type === FuType.alu
    def isJBU(fu_type: UInt) = fu_type === FuType.jbu
    def isMDU(fu_type: UInt) = fu_type === FuType.mdu
    def isCSR(fu_type: UInt) = fu_type === FuType.csr
}

object FuOpType {
    def width   = 7
    def apply() = UInt(width.W)
}

object MemType {
    def N = 0.U(1.W)
    def Y = 1.U(1.W)
}

object WBCtrl {
    def apply() = UInt(4.W)

    val N        = 0.U
    val Y        = 1.U // write executing data to Int Regfile
    val CSR_R    = 2.U // write csr data to Int-Regfile
    val CSR_S    = 3.U
    val CSR_C    = 4.U
    val CSR_W    = 5.U
    val CSR_PRIV = 6.U
}

object Decode {
    val idx_rs1_type = 1
    val idx_rs2_type = 2
    val idx_fu_type  = 3
    val idx_fu_func  = 4
    val idx_mem_en   = 5
    val idx_mem_op   = 6
    val idx_rf_wen   = 7

    // format: off
    val decodeDefault = List( InstrType.illegal, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.N )

    def DecodeTable =
        RV64I.table ++ RV64M.table ++ RVZicsr.table ++ Priviledged.table ++ SelfDefineTrap.table
}

trait RISCVConstants {
    // abstract out instruction decode magic numbers
    val RD_MSB  = 11
    val RD_LSB  = 7
    val RS1_MSB = 19
    val RS1_LSB = 15
    val RS2_MSB = 24
    val RS2_LSB = 20

    val CSR_ADDR_MSB = 31
    val CSR_ADDR_LSB = 20
}

class ScoreBoard extends Module {
    val io = IO(new Bundle() {
        val set   = Input(UInt(32.W))
        val clear = Input(UInt(32.W))
        val raddr = Vec(2, Input(UInt(log2Up(32).W)))
        val rdata = Vec(2, Output(Bool()))
    })
    val regBusy = RegInit(0.U(32.W))

    regBusy := Cat(((regBusy & (~io.clear)) | io.set)(32 - 1, 1), 0.U(1.W))

    for (i <- 0 until 2) {
        io.rdata(i) := regBusy(io.raddr(i))
    }

    when(GTimer() < 0.U && GTimer() > 0.U) {
        printf("\ntime: %d-------ScoreBoard-------\n", GTimer())
        printf("regBusy = 0x%x\n", regBusy)
        printf("io.set = 0x%x\n", io.set)
        printf("io.clear = 0x%x\n", io.clear)
    }
}

class DecodeUnit extends Module with RISCVConstants {
    val io = IO(new Bundle() {
        val in  = Flipped(DecoupledIO(new FetchInstrInfo))
        val out = DecoupledIO(new DecodeCtrlSignal)
        // read reg file
        val read     = Vec(2, Flipped(new RegfileReadIO))
        val wbByPass = Flipped(ValidIO(new WBtoSB))
        val flush    = Input(Bool())
        // read csr
        val fromCSR = Flipped(new CSRtoDecodeBundle())
    })

    val (valid, instr, pc) = (io.in.valid, io.in.bits.instr, io.in.bits.pc)
    // decode instruction
    val ctrlsignals = ListLookup(instr, Decode.decodeDefault, Decode.DecodeTable)

    // ctrl signals
    val instr_type = ctrlsignals(0)
    val op1_sel    = ctrlsignals(Decode.idx_rs1_type)
    val op2_sel    = ctrlsignals(Decode.idx_rs2_type)
    val fu_type    = ctrlsignals(Decode.idx_fu_type)
    val fu_func    = ctrlsignals(Decode.idx_fu_func)
    val mem_en     = ctrlsignals(Decode.idx_mem_en)
    val mem_op     = ctrlsignals(Decode.idx_mem_op)
    val rf_wen     = ctrlsignals(Decode.idx_rf_wen)

    val isreg1 = op1_sel === SrcType.reg
    val isreg2 =
                (op2_sel === SrcType.reg) || 
                (instr_type === InstrType.s && op2_sel === SrcType.imm) ||
                (instr_type === InstrType.b && op2_sel === SrcType.imm)
    // src reg
    val rs1_addr = Mux(isreg1 && io.in.valid, instr(RS1_MSB, RS1_LSB), 0.U) // rs1
    val rs2_addr = Mux(isreg2 && io.in.valid, instr(RS2_MSB, RS2_LSB), 0.U) // rs2
    val wb_addr  = instr(RD_MSB, RD_LSB)                                    // rd

    val sb = Module(new ScoreBoard)
    def sb_mask(idx: UInt) = (1.U(32.W) << idx)(32 - 1, 0)
    val wbClearMask = Mux(io.wbByPass.valid, sb_mask(io.wbByPass.bits.wb_addr), 0.U(32.W))
    val setMask     = Mux(io.out.fire() && io.in.valid && rf_wen === WBCtrl.Y, sb_mask(wb_addr), 0.U)

    sb.io.set   := Mux(io.flush, 0.U, setMask)
    sb.io.clear := Mux(io.flush, Fill(32, 1.U(1.W)), wbClearMask)

    sb.io.raddr(0) := rs1_addr
    sb.io.raddr(1) := rs2_addr

    val iscsr = FuType.isCSR(fu_type)
    def isDepend(rfSrc: UInt, rfDest: UInt, wen: Bool): Bool =
        (rfSrc =/= 0.U) && (rfSrc === rfDest) && wen

    val src1DependWB = Mux(
        io.wbByPass.valid && isreg1,
        isDepend(rs1_addr, io.wbByPass.bits.wb_addr, rf_wen === WBCtrl.Y),
        false.B
    )
    val src2DependWB = Mux(
        io.wbByPass.valid && isreg2,
        isDepend(rs2_addr, io.wbByPass.bits.wb_addr, rf_wen === WBCtrl.Y),
        false.B
    )

    val src1forward_reg = RegInit(false.B)
    val src2forward_reg = RegInit(false.B)

    when(src1DependWB) {
        src1forward_reg := true.B
    }
    when(src2DependWB) {
        src2forward_reg := true.B
    }

    when(io.out.fire) {
        src1forward_reg := false.B
        src2forward_reg := false.B
    }

    when(io.flush) {
        src1forward_reg := false.B
        src2forward_reg := false.B
    }

    val src1forward = src1forward_reg
    val src2forward = src2forward_reg

    val src1Ready = Mux(isreg1, !sb.io.rdata(0), true.B) || src1forward
    val src2Ready = Mux(isreg2, !sb.io.rdata(1), true.B) || src2forward

    io.in.ready := io.out.ready && src1Ready && src2Ready

    // TODO: bypass
    val rs1_data = io.read(0).data
    val rs2_data = io.read(1).data

    when(GTimer() < 0.U && EnableDebug.B) {
        printf(
            "\ntime: %d -----decode------\n io.in.valid = %d,io.out.valid = %d\n",
            GTimer(),
            valid,
            io.out.valid
        )
        printf("io.in.ready = %d,io.out.ready = %d\n", io.in.ready, io.out.ready)
        when(valid) {
            printf("decodeIn: instr = 0x%x,pc = 0x%x\n", instr, pc)
        }
        printf("clearvalid = 0x%x,clear = 0x%x\n", io.wbByPass.valid, io.wbByPass.bits.wb_addr)
        printf("addr1 = %d,src1Ready = %d\n", rs1_addr, src1Ready)
        printf("addr2 = %d,src2Ready = %d\n", rs2_addr, src2Ready)
        printf(
            "wbclear_valid = %d,SetMask_valid = %d\n",
            io.wbByPass.valid,
            io.in.valid && rf_wen === WBCtrl.Y
        )
        printf("isReg1 = %d, isReg2 = %d\n", isreg1, isreg2)
        printf("src1DependWB = %d,src2DependWB = %d\n", src1DependWB, src2DependWB)
        printf("rs1_data = 0x%x,rs2_data = 0x%x\n", rs1_data, rs2_data)
        printf("src1forward = %d,src1forward= %d\n", src1forward, src2forward)
    }

    // read the regfile
    io.read(0).addr := rs1_addr
    io.read(1).addr := rs2_addr

    val imm = InstrType.genImm(instr, instr_type)

    // select operand
    val op1_data = MuxCase(
        0.U,
        Array(
            (op1_sel === SrcType.reg) -> rs1_data,
            (op1_sel === SrcType.imm) -> imm,
            (op1_sel === SrcType.pc)  -> pc
        )
    ).asUInt
    val op2_data = MuxCase(
        0.U,
        Array(
            (op2_sel === SrcType.reg) -> rs2_data,
            (op2_sel === SrcType.imm) -> imm,
            (op2_sel === SrcType.pc)  -> pc
        )
    ).asUInt

    // CSR Read
    io.fromCSR.csr_addr := io.in.bits.instr(31, 20)
    val csr_data = io.fromCSR.csr_data

    // output to next stage
    io.out.valid           := io.out.ready && src1Ready && src2Ready && RegNext(!io.flush)
    io.out.bits.pc         := pc
    io.out.bits.instr_type := instr_type
    io.out.bits.exception  := io.in.bits.exception
    io.out.bits.instr      := instr
    io.out.bits.op1_data   := op1_data
    io.out.bits.op2_data   := op2_data
    io.out.bits.rs2_data   := io.read(1).data
    io.out.bits.fu_type    := fu_type
    io.out.bits.fu_func    := fu_func
    io.out.bits.mem_en     := mem_en
    io.out.bits.mem_op     := mem_op
    io.out.bits.wb_addr    := wb_addr
    io.out.bits.rf_wen     := rf_wen
    io.out.bits.csr_rdata  := csr_data
}
