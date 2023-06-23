package bus

import chisel3._
import chisel3.util._
import core._
import config.Config._

object CpuLinkCmd {

    // bits(0) is read or write, bits(1) is req or resp, bits(3, 2) is instr、load or store
    def inst_req  = "b0000".U
    def load_req  = "b0100".U
    def store_req = "b1001".U
    // def ptw_req   = "b1100".U

    def inst_resp  = "b0010".U
    def load_resp  = "b0110".U
    def store_resp = "b1011".U
    // def ptw_resp   = "b1110".U

    def len: Int = 4
    def apply()  = UInt(4.W)

    def isWriteReq(cmd: UInt) = cmd(1, 0) === "b01".U

    def isInstr(cmd: UInt) = cmd(3, 2) === "b00".U
    def isLoad(cmd: UInt)  = cmd(3, 2) === "b01".U
    def isStore(cmd: UInt) = cmd(3, 2) === "b10".U
    def isPtw(cmd: UInt)   = cmd(3, 2) === "b11".U

    def req2resp(req_cmd: UInt): UInt = {
        require(req_cmd.getWidth == len)
        Cat(Cat(req_cmd(3, 2), "b1".U), req_cmd(0))
    }
}

trait CpuLinkConst {
    val idBits   = 4
    val typeBits = 2
}

class CpuLinkBundle extends Bundle with CpuLinkConst

class CpuLinkReq extends CpuLinkBundle {
    val addr  = UInt(XLEN.W)
    val cmd   = CpuLinkCmd()
    val wdata = UInt(XLEN.W)
    val strb  = UInt(8.W)
    val size  = UInt(2.W)

    def apply(addr: UInt, cmd: UInt, size: UInt, wdata: UInt = 0.U, strb: UInt = 0.U) = {
        this.addr  := addr
        this.cmd   := cmd
        this.size  := size
        this.wdata := wdata
        this.strb  := strb
    }
}

class CpuLinkResp extends CpuLinkBundle {
    val data      = UInt(XLEN.W)
    val cmd       = CpuLinkCmd()
    val exception = Vec(ExceptionCode.total, Bool())

    def apply(data: UInt, cmd: UInt, exceptions: UInt) = {
        require(exceptions.getWidth == ExceptionCode.total)

        this.data      := data
        this.cmd       := cmd
        this.exception := exceptions.asTypeOf(chiselTypeOf(this.exception))
    }
}

class CpuLinkBus extends CpuLinkBundle {
    val req  = DecoupledIO(new CpuLinkReq)
    val resp = Flipped(DecoupledIO(new CpuLinkResp))
}

object MasterCpuLinkBus {
    def apply() = new CpuLinkBus
}

object SlaveCpuLinkBus {
    def apply() = Flipped(new CpuLinkBus)
}

class DoubleCpuLink extends Bundle {
    val imem = MasterCpuLinkBus()
    val dmem = MasterCpuLinkBus()
}

class CpuLinkCrossBar1toN(addrSpace: List[(Long, Long)]) extends Module with CpuLinkConst {
    val numOut = addrSpace.length

    val io = IO(new Bundle() {
        val in  = SlaveCpuLinkBus()
        val out = Vec(numOut, MasterCpuLinkBus())
    })

    val req = io.in.req

    val addr = req.bits.addr
    val outMatchVec = VecInit(
        addrSpace.map(range => (addr >= range._1.U && addr < (range._1 + range._2).U))
    )
    val outSelVec = VecInit(PriorityEncoderOH(outMatchVec))

    req.ready := io.out.zip(outSelVec).map { case (o, m) => o.req.ready && m }.reduce(_ | _)

    for (i <- 0 until numOut) {
        val out = io.out(i)

        out.req.valid := req.valid && outSelVec(i)
        out.req.bits.apply(
            addr = req.bits.addr,
            cmd = req.bits.cmd,
            size = req.bits.size,
            wdata = req.bits.wdata,
            strb = req.bits.strb
        )
    }

    // resp
    val respArb = Module(new Arbiter(new CpuLinkResp, numOut))
    respArb.io.in.zip(io.out).map { case (in, out) => in <> out.resp }
    io.in.resp <> respArb.io.out
}
