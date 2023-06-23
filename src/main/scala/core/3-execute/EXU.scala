package core

import chisel3._
import chisel3.util._

import utils.GTimer
import config.Config.EnableDebug

/** Execute Unit
  */
class EXU extends Module {
    val io = IO(new Bundle() {
        val in  = Flipped(DecoupledIO(new DecodeCtrlSignal))
        val out = DecoupledIO(new ExuOutput)
    })

    val (op1_data, op2_data, fu_type, fu_func) =
        (io.in.bits.op1_data, io.in.bits.op2_data, io.in.bits.fu_type, io.in.bits.fu_func)

    io.in.ready := io.out.ready

    val isALU = FuType.isALU(fu_type)
    val isJBU = FuType.isJBU(fu_type)
    val isCSR = FuType.isCSR(fu_type)

    val alu = Module(new ALU)
    alu.io.in.valid     := io.in.valid && isALU
    alu.io.in.bits.src1 := op1_data
    alu.io.in.bits.src2 := op2_data
    alu.io.in.bits.func := fu_func
    alu.io.out.ready    := io.out.ready

    val jbu = Module(new JumpBranchUnit)
    jbu.io.in.valid     := io.in.valid && isJBU
    jbu.io.in.bits.func := fu_func
    jbu.io.in.bits.src1 := op1_data
    jbu.io.in.bits.src2 := op2_data
    jbu.br_io.pc        := io.in.bits.pc
    jbu.br_io.rs2_data  := io.in.bits.rs2_data
    jbu.io.out.ready    := io.out.ready

    val csr = Module(new CsrUnit)
    csr.io.in.valid     := io.in.valid && isCSR
    csr.io.in.bits.func := fu_func
    csr.io.in.bits.src1 := io.in.bits.csr_rdata
    csr.io.in.bits.src2 := op1_data
    csr.io.out.ready    := io.out.ready

    val out_valid =
        (isALU && alu.io.out.valid) || (isJBU && jbu.io.out.valid) || (isCSR && csr.io.out.valid)

    // TODO: add more fu such as MDU(mul/div unit)、CSR...

    val exe_result =
        Mux(isALU, alu.io.out.bits.data, Mux(isCSR, csr.toreg.data.bits, jbu.io.out.bits.data))

    val time = GTimer()
    when(time < 0.U && EnableDebug.B) {
        printf("\ntime: %d -----EXU------\nio.in.valid = 0x%x\n", time, io.in.valid)
        printf("ready = %d\n", io.in.ready)
        printf("out_valid = 0x%x\n", io.out.valid)
        printf("pc = 0x%x\n", io.in.bits.pc)
        when(io.in.valid) {
            printf("decodeIn: instr = 0x%x,pc = 0x%x\n", io.in.bits.instr, io.in.bits.pc)
            printf("out_data = 0x%x\n", exe_result)
        }
        when(jbu.io.in.valid) {
            printf("jbu.io.in.valid------\n")
            printf("jbu.in.src1 = 0x%x,src2 = 0x%x\n", op1_data, op2_data)
            printf("jbu.out.redirect.target = 0x%x\n", jbu.io.redirect.target)
            printf("jbu.br_io.pc = 0x%x\n", jbu.br_io.pc)
        }
        when(csr.io.in.valid) {
            printf("src1 = 0x%x,src2 = 0x%x\n", csr.io.in.bits.src1, csr.io.in.bits.src2)
            printf("csr.toreg.data.bits = 0x%x\n", csr.toreg.data.bits)
        }
    }

    // output to next stage
    io.out.valid           := out_valid
    io.out.bits.pc         := io.in.bits.pc
    io.out.bits.instr      := io.in.bits.instr
    io.out.bits.exception  := io.in.bits.exception
    io.out.bits.instr_type := io.in.bits.instr_type
    io.out.bits.rs2_data   := io.in.bits.rs2_data
    io.out.bits.exe_result := exe_result
    io.out.bits.mem_en     := io.in.bits.mem_en
    io.out.bits.mem_op     := io.in.bits.mem_op
    io.out.bits.rf_wen     := io.in.bits.rf_wen
    io.out.bits.wb_addr    := io.in.bits.wb_addr
    // redirect
    io.out.bits.redirect := jbu.io.redirect
    io.out.bits.fu_type  := io.in.bits.fu_type
    io.out.bits.wb_csr   := csr.io.out.bits.data
}
