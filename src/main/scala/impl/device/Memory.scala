package impl.device

import chisel3._
import chisel3.util._

import config.Config._
import impl._
import utils._
import sim.device._

// This Module is represent for a memory IP core of your FPGA platform(e.g. xilinx)
class RealMemory(memByte: Long, beatBytes: Int) extends BlackBox {
    val cfg   = implConfig
    val io = IO(new Bundle{
        val clka  = Input(Clock())
        val wea   = Input(Bool())
        val addra = Input(UInt(cfg.memAddrWidth.W))
        val dina  = Input(UInt(cfg.memDataWidth.W))
        val douta = Output(UInt(cfg.memDataWidth.W))

        val clkb  = Input(Clock())
        val web   = Input(Bool())
        val addrb = Input(UInt(cfg.memAddrWidth.W))
        val dinb  = Input(UInt(cfg.memDataWidth.W))
        val doutb = Output(UInt(cfg.memDataWidth.W))
    })
}

// Only for simulation
class RealMemory_1(memByte: Long, beatBytes: Int) extends Module { 
    val cfg   = implConfig
    val io = IO(new Bundle{
        // Port A for DMem (rw)
        val clka  = Input(Clock())
        val wea   = Input(Bool())
        val addra = Input(UInt(cfg.memAddrWidth.W))
        val dina  = Input(UInt(cfg.memDataWidth.W))
        val douta = Output(UInt(cfg.memDataWidth.W))

        // Port B for IMem (r)
        val clkb  = Input(Clock())
        val web   = Input(Bool())
        val addrb = Input(UInt(cfg.memAddrWidth.W))
        val dinb  = Input(UInt(cfg.memDataWidth.W))
        val doutb = Output(UInt(cfg.memDataWidth.W))
    })

    val mem = RegInit(VecInit(Seq.fill((memByte / beatBytes).toInt)(0.U((beatBytes * 8).W))))

    when(io.wea) {
        mem(io.addra) := io.dina
    }

    // SyncReadMem
    io.douta := RegNext(mem(io.addra))
    io.doutb := RegNext(mem(io.addrb))
}

class MainMemory(
    memByte: Long = 256 * 1024 * 1024,
    beatBytes: Int = 8
) extends BaseDevice {

    val offsetBits = log2Up(memByte)
    val offsetMask = (1 << offsetBits) - 1
    val split      = beatBytes / 8
    val bankByte   = memByte / split

    def index(addr: UInt)  = ((addr & offsetMask.U) >> log2Ceil(beatBytes)).asUInt()
    def inRange(idx: UInt) = idx < (memByte / 8).U

    val rIdx = index(raddr)
    val wIdx = index(waddr)

    val rIdx_1 = index(raddr2)

    // val mems = (0 until split).map { _ => Module(new RealMemory_1(1024, beatBytes)) }
    val mems = (0 until split).map { _ => Module(new RealMemory(1024, beatBytes)) }
    mems.zipWithIndex map { case (mem, i) =>
        mem.io.clka  := clock
        mem.io.clkb  := clock
        mem.io.addra := (rIdx << log2Ceil(split)) + i.U // addra == raddr == waddr
        mem.io.addrb := (rIdx_1 << log2Ceil(split)) + i.U
        mem.io.dina  := wdata((i + 1) * 64 - 1, i * 64)
        mem.io.dinb  := DontCare
        mem.io.wea   := wstrb((i + 1) * 8 - 1, i * 8) & wen
        mem.io.web   := Fill(beatBytes, 0.U(1.W))
    }
    val rXLEN_0 = mems.map { mem => mem.io.douta }
    val rXLEN_1 = mems.map { mem => mem.io.doutb }
    rdata  := Cat(rXLEN_0.reverse)
    rdata2 := Cat(rXLEN_1.reverse)

    io.in.dmem.resp.valid := RegNext(io.in.dmem.req.valid)
    io.in.imem.resp.valid := RegNext(io.in.imem.req.valid)

}
