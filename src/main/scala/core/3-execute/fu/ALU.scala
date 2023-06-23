package core

import chisel3._
import chisel3.util._

import util._
import utils.SignExt

object ALUOpType {
    // 7 bits width
    // ALU Operation Signal
    def ALUTypeWidth = 7

    val ADD        = "b0000000".U
    val SUB        = "b0000001".U
    val LESSTHAN_U = "b0000010".U
    val SLLI       = "b0000011".U
    val SHIFTRIGHT = "b0000100".U
    val AND        = "b0000101".U
    val OR         = "b0000110".U
    val XOR        = "b0000111".U
    val MUL        = "b0001000".U
    val DIV        = "b0001001".U
    val REM        = "b0001010".U
    val SLTI       = "b0001011".U
    val SRAI       = "b0001100".U
    val SLL        = "b0001101".U
    val SLT        = "b0001110".U
    val SRL        = "b0001111".U
    val SRA        = "b0010000".U
    val REMU       = "b0010001".U
    val DIVU       = "b0010010".U

    // word op need signExt
    val ADDW  = "b1000000".U
    val ADDIW = "b1000001".U
    val SUBW  = "b1000010".U
    val SLLW  = "b1000011".U
    val SRAIW = "b1000100".U
    val MULW  = "b1000101".U
    val DIVW  = "b1000110".U
    val REMW  = "b1000111".U
    val SLLIW = "b1001000".U
    val SRLIW = "b1001001".U
    val SRAW  = "b1001010".U
    val SRLW  = "b1001011".U
    val DIVUW = "b1001100".U
    val REMUW = "b1001101".U

    def OpSignExt(ALUop: UInt) = ALUop(6)

}

class ALU extends FunctionUnit(hasRedirect = false) {
    val (src1, src2, func) = (io.in.bits.src1, io.in.bits.src2, io.in.bits.func)

    io.in.ready := true.B

    // TODO : later use adder and multiplying unit
    // alu operation
    val alu_res = MuxCase(
        0.U,
        Array(
            (func === ALUOpType.ADD) -> (src1 + src2).asUInt(),
            (func === ALUOpType.SUB) -> (src1 - src2).asUInt(),
            (func === ALUOpType.LESSTHAN_U) -> (src1 < src2).asUInt(),
            (func === ALUOpType.SLTI) -> (src1.asSInt < src2.asSInt).asUInt(),
            (func === ALUOpType.SLT) -> (src1.asSInt < src2.asSInt).asUInt(),
            (func === ALUOpType.SHIFTRIGHT) -> (src1 >> src2(5, 0)).asUInt(),
            (func === ALUOpType.AND) -> (src1 & src2).asUInt(),
            (func === ALUOpType.ADDW) -> (src1 + src2).asUInt(),
            (func === ALUOpType.ADDIW) -> (src1 + src2).asUInt(),
            (func === ALUOpType.SUBW) -> (src1 - src2).asUInt(),
            (func === ALUOpType.OR) -> (src1 | src2).asUInt(),
            (func === ALUOpType.XOR) -> (src1 ^ src2).asUInt(), // FIXME
            (func === ALUOpType.MUL) -> (src1 * src2).asUInt(),
            (func === ALUOpType.MULW) -> (src1 * src2).asUInt(),
            (func === ALUOpType.DIV) -> (src1 / src2).asUInt(),
            (func === ALUOpType.DIVW) -> (src1 / src2).asUInt(),
            (func === ALUOpType.DIVU) -> (src1 / src2).asUInt(),
            (func === ALUOpType.DIVUW && src2(31, 0) =/= 0.U) -> (src1(31, 0).asUInt / src2(
                31,
                0
            ).asUInt).asUInt, // todo
            (func === ALUOpType.DIVUW && src2(31, 0) === 0.U) -> ("hffff_ffff_ffff_ffff".U),
            (func === ALUOpType.REM) -> (src1(31, 0) % src2(31, 0)).asUInt(),
            (func === ALUOpType.REMW) -> (src1(31, 0) % src2(31, 0)).asUInt(),
            (func === ALUOpType.REMU) -> (src1 % src2).asUInt(),
            (func === ALUOpType.REMUW) -> (src1(31, 0) % src2(31, 0)).asUInt(),
            (func === ALUOpType.SLLW) -> (src1 << src2(4, 0)).asUInt(), // TODO
            (func === ALUOpType.SLLIW) -> (src1 << src2(5, 0)).asUInt(),
            (func === ALUOpType.SLLI) -> (src1 << src2(5, 0)).asUInt(), // TODO
            (func === ALUOpType.SLLW) -> (src1 << src2(5, 0)).asUInt(), // FIXME
            (func === ALUOpType.SLL) -> (src1 << src2(5, 0)).asUInt(),  // FIXME

            (func === ALUOpType.SRAIW) -> (src1(31, 0).asSInt >> src2(5, 0)).asUInt(), // FIXME
            (func === ALUOpType.SRAI) -> (src1.asSInt >> src2(5, 0)).asUInt(),         // FIXME
            (func === ALUOpType.SRL) -> (src1 >> src2(5, 0)).asUInt(),                 // FIXME
            (func === ALUOpType.SRA) -> (src1.asSInt >> src2(5, 0)).asUInt(),          // FIXME

            (func === ALUOpType.SRLIW) -> (src1(31, 0) >> src2(5, 0)).asUInt(),       // FIXME
            (func === ALUOpType.SRAW) -> (src1(31, 0).asSInt >> src2(4, 0)).asUInt(), // FIXME
            (func === ALUOpType.SRLW) -> (src1(31, 0) >> src2(4, 0)).asUInt()         // FIXME

        )
    )

    val alu_out = Mux(ALUOpType.OpSignExt(func), SignExt(alu_res(31, 0), 64), alu_res)

    io.out.valid     := io.in.valid
    io.out.bits.data := alu_out
}
