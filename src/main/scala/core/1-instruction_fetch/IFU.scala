package core

import chisel3._
import chisel3.util._

import bus._
import utils._
import config.Config._

class FetchInstrInfo extends Bundle {
    val instr     = UInt(32.W)
    val pc        = UInt(XLEN.W)
    val exception = Vec(ExceptionCode.total, Bool())
}

/** Instruction Fetch Unit (IFU) This module is responsible for fetching instructions and sending them to the decode unit
  */
class IFU(isPipeLine: Boolean = false) extends Module {
    val io = IO(new Bundle() {
        val out       = DecoupledIO(new FetchInstrInfo) // out to next-stage, decode
        val update_pc = Flipped(ValidIO(new UpdatePC))  // update the PC register
        val imem      = MasterCpuLinkBus()
    })

    // PC Register
    val pc = RegInit(ResetPC.U(XLEN.W))

    if (!isPipeLine) { // Single Stage Processor
        val next_pc = io.update_pc.bits.target

        // update pc
        when(io.update_pc.valid) {
            pc := next_pc
        }
    } else { // Five Stage Processor
        val next_pc = Mux(io.update_pc.valid, io.update_pc.bits.target, pc + 4.U)

        // update pc
        when(io.update_pc.valid || io.imem.req.fire) {
            pc := next_pc
        }
    }

    // request to memory for fetching instruction
    io.imem.req.valid      := !reset.asBool && io.out.ready
    io.imem.req.bits.addr  := pc // pc is the address of instruction
    io.imem.req.bits.size  := "b10".U // 2^2 = 4 byte == 32-bit
    io.imem.req.bits.cmd   := CpuLinkCmd.inst_req
    io.imem.req.bits.wdata := DontCare
    io.imem.req.bits.strb  := DontCare

    io.imem.resp.ready := true.B

    // TODO: now just support 32 bits length of instruction
    // Every instruction is 32-bit wide so we need to seperate the resp data for XLEN=64-bit
    val instr = Mux(pc(2).asBool, io.imem.resp.bits.data(XLEN - 1, 32), io.imem.resp.bits.data(31, 0))

    io.out.valid          := io.imem.resp.valid
    io.out.bits.pc        := pc
    io.out.bits.instr     := instr
    io.out.bits.exception := io.imem.resp.bits.exception
}
