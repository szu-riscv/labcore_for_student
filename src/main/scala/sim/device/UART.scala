package sim.device

import chisel3._

import difftest.UARTIO

import bus._
import utils._

class UART() extends BaseDevice {
    val uartio = IO(new UARTIO)

    val rxfifo = RegInit(0.U(32.W))
    val txfifo = Reg(UInt(32.W))
    val stat   = RegInit(1.U(32.W))
    val ctrl   = RegInit(0.U(32.W))

    uartio.out.valid := (waddr(3, 0) === 4.U && wen)
    uartio.out.ch    := wdata(7, 0)
    uartio.in.valid  := (raddr(3, 0) === 0.U && ren)

    val mapping = Seq(
        // RegMap(0x0, uartio.in.ch, RegMap.Unwritable),
        RegMap(0x4, txfifo),
        RegMap(0x8, stat),
        RegMap(0xc, ctrl)
    )

    RegMap.generate(
        mapping,
        raddr(3, 0),
        rdata,
        waddr(3, 0),
        wen,
        wdata,
        MaskExpand(wstrb >> waddr(2, 0))
    )
}
