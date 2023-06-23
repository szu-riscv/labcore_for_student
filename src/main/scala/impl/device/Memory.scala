package impl.device

import chisel3._
import chisel3.util._

import config.Config._
import impl._
import utils._
import sim.device._

// This Module is represent for a memory IP core of your FPGA platform(e.g. xilinx)
class RealMemory extends BlackBox {
    val cfg   = implConfig
    val clka  = IO(Input(Clock()))
    val wea   = IO(Input(Bool()))
    val addra = IO(Input(UInt(cfg.memAddrWidth.W)))
    val dina  = IO(Input(UInt(cfg.memDataWidth.W)))
    val douta = IO(Output(UInt(cfg.memDataWidth.W)))

    val clkb  = IO(Input(Clock()))
    val web   = IO(Input(Bool()))
    val addrb = IO(Input(UInt(cfg.memAddrWidth.W)))
    val dinb  = IO(Input(UInt(cfg.memDataWidth.W)))
    val doutb = IO(Output(UInt(cfg.memDataWidth.W)))
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

    val mems = (0 until split).map { _ => Module(new RealMemory()) }
    mems.zipWithIndex map { case (mem, i) =>
        mem.clka  := clock
        mem.clkb  := clock
        mem.addra := (rIdx << log2Ceil(split)) + i.U // addra == raddr == waddr
        mem.addrb := (rIdx_1 << log2Ceil(split)) + i.U
        mem.dina  := wdata((i + 1) * 64 - 1, i * 64)
        mem.dinb  := DontCare
        mem.wea   := wstrb((i + 1) * 8 - 1, i * 8) & wen
        mem.web   := Fill(beatBytes, 0.U(1.W))
    }
    val rXLEN_0 = mems.map { mem => mem.douta }
    val rXLEN_1 = mems.map { mem => mem.doutb }
    rdata  := Cat(rXLEN_0.reverse)
    rdata2 := Cat(rXLEN_1.reverse)

}
