package core

import chisel3._
import chisel3.util._

import difftest._

import utils._
import config.Config._

class WBtoSB extends Bundle {
    val wb_addr = UInt(log2Up(32).W)
    val data    = UInt(64.W)
}

// Write Back Unit
class WBU(isPipeLine: Boolean = false) extends Module {
    val io = IO(new Bundle() {
        val in        = Flipped(DecoupledIO(new WriteBackIO))
        val writeback = Flipped(new RegfileWriteIO)
        val update_pc = ValidIO(new UpdatePC)
        // csr
        val toCSR       = ValidIO(new WBUtoCSRBundle)
        val csrRedirect = Input(new RedirectIO)
        val toSB        = ValidIO(new WBtoSB)
    })

    io.in.ready := true.B

    // write the result of instruction into regfile
    io.writeback.addr := io.in.bits.wb_addr
    io.writeback.data := io.in.bits.wb_data
    io.writeback.wen  := io.in.valid && io.in.bits.rf_wen

    io.toSB.valid        := io.in.valid && io.in.bits.rf_wen
    io.toSB.bits.wb_addr := io.writeback.addr
    io.toSB.bits.data    := io.in.bits.wb_data

    // update pc to pointer the next instruction
    if (!isPipeLine) {
        io.update_pc.valid := io.in.valid
        io.update_pc.bits.target := Mux(
            io.in.bits.redirect.valid,
            io.in.bits.redirect.target,
            io.in.bits.pc + 4.U
        )
    } else {
        io.update_pc.valid := (io.in.bits.redirect.valid || io.csrRedirect.valid) && io.in.valid

        when(GTimer() > 0.U && EnableDebug.B) {
            printf("\ntime: %d------WBU--------\n", GTimer())
            printf(
                "\nio.writeback.wen = %d,write back addr = %d; write back data = %x,pc = 0x%x\n",
                io.writeback.wen,
                io.in.bits.wb_addr,
                io.in.bits.wb_data,
                io.in.bits.pc
            )
            printf("io.in.valid = 0x%x\n", io.in.valid)
            printf("pc = 0x%x\n", io.in.bits.pc)
            printf("instr = 0x%x\n", io.in.bits.instr)
            printf("io.redirect.valid = 0x%x\n", io.in.bits.redirect.valid)
            printf("io.in.bits.redirect.target = 0x%x\n", io.in.bits.redirect.target)
            printf("io.update_pc.valid   = 0x%x\n", io.update_pc.valid)
            printf("io.update_pc.bits.target  = 0x%x\n", io.update_pc.bits.target)
            printf("tocsr.valid = %d\n", io.toCSR.valid)
            printf("toCSR.bits.wdata = 0x%x\n", io.toCSR.bits.wdata)
        }

        io.update_pc.bits.target := Mux(
            io.in.bits.redirect.valid,
            io.in.bits.redirect.target,
            io.csrRedirect.target
        )
    }

    io.toCSR.valid           := io.in.valid && FuType.isCSR(io.in.bits.fu_type)
    io.toCSR.bits.pc         := io.in.bits.pc
    io.toCSR.bits.instr      := io.in.bits.instr
    io.toCSR.bits.exceptions := io.in.bits.exception
    io.toCSR.bits.csr_cmd    := DontCare
    io.toCSR.bits.rs1        := DontCare
    io.toCSR.bits.wdata      := io.in.bits.csr_data

    if (DiffTest) {
        val difftest_instr = Module(new DifftestInstrCommit)
        difftest_instr.io.clock    := clock
        difftest_instr.io.coreid   := 0.U
        difftest_instr.io.index    := 0.U
        difftest_instr.io.instr    := RegNext(io.in.bits.instr)
        difftest_instr.io.dec_type := RegNext(io.in.bits.instr_type)
        difftest_instr.io.valid    := RegNext(io.in.valid) && !reset.asBool
        difftest_instr.io.pc       := RegNext(io.in.bits.pc)
        difftest_instr.io.special  := 0.U
//    difftest_instr.io.skip      := false.B  // TODO: RegNext(isMMIO)
        difftest_instr.io.skip  := RegNext(io.in.bits.isMMIO)
        difftest_instr.io.isRVC := false.B
        difftest_instr.io.wen := RegNext(
            io.in.valid && io.in.bits.rf_wen && io.in.bits.wb_addr =/= 0.U
        )
        difftest_instr.io.wpdest := 0.U // useless
        difftest_instr.io.wdest  := RegNext(io.in.bits.wb_addr)
        difftest_instr.io.wdata  := RegNext(io.in.bits.wb_data)

        val instrCnt = RegInit(0.U(64.W))
        val cycleCnt = RegInit(0.U(64.W))
        when(!reset.asBool) {
            cycleCnt := cycleCnt + 1.U
        }

        when(io.in.fire) {
            instrCnt := instrCnt + 1.U
        }
        val hitTrap  = io.in.valid && io.in.bits.instr === SelfDefineTrap.TRAP
        val trapCode = io.in.bits.wb_data
        val trapPC   = io.in.bits.pc

        val difftest_trap = Module(new DifftestTrapEvent)
        difftest_trap.io.clock    := clock
        difftest_trap.io.coreid   := 0.U
        difftest_trap.io.valid    := hitTrap
        difftest_trap.io.code     := trapCode
        difftest_trap.io.pc       := trapPC
        difftest_trap.io.cycleCnt := cycleCnt
        difftest_trap.io.instrCnt := instrCnt
    }

}
