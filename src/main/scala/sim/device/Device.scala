package sim.device

import chisel3._
import chisel3.util._

import difftest.UARTIO

import bus._
import utils._
import config.Config._

abstract class BaseDevice() extends Module {
    val io = IO(new Bundle() {
        val in = Flipped(new DoubleCpuLink)
    })

    def HoldUnless(v: UInt, en: Bool) = Mux(en, v, RegEnable(next = v, enable = en))

    val raddr = Wire(UInt(PAddrBits.W))
    val rdata = Wire(UInt(XLEN.W))
    val waddr = Wire(UInt(PAddrBits.W))
    val wdata = Wire(UInt(XLEN.W))
    val wstrb = Wire(UInt((XLEN / 8).W))
    val ren   = Wire(Bool())
    val wen   = Wire(Bool())

    val raddr2 = Wire(UInt(PAddrBits.W))
    val rdata2 = Wire(UInt(XLEN.W))

    raddr2 := DontCare
    rdata2 := DontCare

    val in = io.in
    in.dmem.req.ready  := true.B
    in.imem.req.ready  := true.B
    in.dmem.resp.valid := in.dmem.req.valid
    in.imem.resp.valid := in.imem.req.valid

    raddr                  := in.dmem.req.bits.addr
    in.dmem.resp.bits.data := rdata

    raddr2                 := in.imem.req.bits.addr
    in.imem.resp.bits.data := rdata2

    in.imem.resp.bits.cmd       := CpuLinkCmd.req2resp(in.imem.req.bits.cmd)
    in.dmem.resp.bits.cmd       := CpuLinkCmd.req2resp(in.dmem.req.bits.cmd)
    in.imem.resp.bits.exception := DontCare
    in.dmem.resp.bits.exception := DontCare

    // write event
    waddr := in.dmem.req.bits.addr
    wdata := in.dmem.req.bits.wdata
    wstrb := in.dmem.req.bits.strb
    ren   := in.dmem.resp.fire
    wen   := CpuLinkCmd.isWriteReq(cmd = in.dmem.req.bits.cmd) && in.dmem.req.valid

}
