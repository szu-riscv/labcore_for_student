package core

import chisel3._
import chisel3.util._

import config._

trait PcOffsetIO {
    this: FunctionUnit =>
    val br_io = IO(new Bundle {
        val pc       = Input(UInt(Config.AddrBits.W))
        val rs2_data = Input(UInt(Config.XLEN.W))
    })
}

class JumpBranchUnit extends FunctionUnit(hasRedirect = true) with PcOffsetIO {

    val (src1, src2, func) = (io.in.bits.src1, io.in.bits.src2, io.in.bits.func)

    io.in.ready := true.B
    val taken = WireInit(false.B)

    val base_addr = src1
    val offset    = src2

    val jmp_target = base_addr + offset
    //  val snpc = base_addr + 4.U

    val snpc = br_io.pc + 4.U  // jarl
    val dnpc = base_addr + 4.U // jal

//  printf("jmp_target = 0x%x\n",jmp_target)

    // for jal jalr
    val isJump = JBUType.isJump(func)

    // TODO: for branch
    val isBranch = JBUType.isBranch(func)
    val br_taken = WireInit(false.B)

    val rs1 = src1
    val rs2 = br_io.rs2_data
    /// more: pc + offset
    val br_target = br_io.pc + offset
//  printf("pc = 0x%x, offset = 0x%x,br_target = 0x%x\n",br_io.pc,offset,br_target)

    // todo operate br_taken
    br_taken := MuxCase(
        false.B,
        Array(
            (func === JBUType.bne) -> (rs1 =/= rs2),
            (func === JBUType.beq) -> (rs1 === rs2),
            (func === JBUType.bge) -> (rs1.asSInt >= rs2.asSInt),
            (func === JBUType.bgeu) -> (rs1 >= rs2),
            (func === JBUType.blt) -> (rs1.asSInt < rs2.asSInt),
            (func === JBUType.bltu) -> (rs1 < rs2)
        )
    )

    taken := isJump || (isBranch && br_taken)

    io.out.valid := io.in.valid
//  io.out.bits.data := snpc
    io.out.bits.data := Mux(isJump, Mux(JBUType.isJalr(func), snpc, dnpc), snpc)

//  printf("snpc = 0x%x\n",snpc);

    io.redirect.valid  := io.in.valid && taken
    io.redirect.target := Mux(isJump, jmp_target, br_target)

}
