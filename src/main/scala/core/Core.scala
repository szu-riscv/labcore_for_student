package core

import chisel3._
import chisel3.util._

import difftest._

import bus._
import utils._
import config.Config._

class Core(hasPipeLine: Boolean = false) extends Module {
    val io = IO(new Bundle {
        val mem = new DoubleCpuLink
    })

    // five stage module
    val ifu = Module(new IFU(isPipeLine = hasPipeLine))
    val dec = Module(new DecodeUnit)
    val exe = Module(new EXU)
    val mem = Module(new AccessMem)
    val wbu = Module(new WBU(isPipeLine = hasPipeLine))

    val gpr = Module(new Regfile(debug_port = DiffTest)) // gpr: General Purpose Register
    val csr = Module(new CSR) // csr: Control and Status Register

    // connect these stages
    if (!hasPipeLine) {
        ifu.io.out <> dec.io.in
        dec.io.out <> exe.io.in
        exe.io.out <> mem.io.in
        mem.io.out <> wbu.io.in
    } else {
        val flush = wbu.io.update_pc.valid

        PipelineConnect(
            left = ifu.io.out,
            right = dec.io.in,
            rightOutFire = dec.io.out.fire,
            isFlush = flush
        )
        PipelineConnect(
            left = dec.io.out,
            right = exe.io.in,
            rightOutFire = exe.io.out.fire,
            isFlush = flush
        )
        PipelineConnect(
            left = exe.io.out,
            right = mem.io.in,
            rightOutFire = mem.io.out.fire,
            isFlush = flush
        )
        PipelineConnect(
            left = mem.io.out,
            right = wbu.io.in,
            rightOutFire = true.B,
            isFlush = flush
        )

        dec.io.wbByPass := wbu.io.toSB
        dec.io.flush    := flush
        mem.io.redirect := flush
    }

    // RegFile Read and Write
    gpr.io.read  <> dec.io.read
    gpr.io.write <> wbu.io.writeback

    // redirect
    ifu.io.update_pc <> wbu.io.update_pc

    // access memory
    io.mem.imem <> ifu.io.imem
    io.mem.dmem <> mem.io.dmem

    // csr
    dec.io.fromCSR  <> csr.io.read
    csr.io.wb       <> wbu.io.toCSR
    csr.io.redirect <> wbu.io.csrRedirect

    // difftest debug
    if (DiffTest) {
        val debug_gpr    = gpr.io.debug_gpr
        val difftest_int = Module(new DifftestArchIntRegState)
        difftest_int.io.clock  := clock
        difftest_int.io.coreid := 0.U
        difftest_int.io.gpr    := debug_gpr

        val difftest_fp = Module(new DifftestArchFpRegState)
        difftest_fp.io.clock  := clock
        difftest_fp.io.coreid := 0.U
        difftest_fp.io.fpr    := VecInit((0 until 32).map(i => 0.U))
    }
}
