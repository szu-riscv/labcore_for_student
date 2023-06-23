package core

import chisel3._
import chisel3.util._

import config._
import config.Config.EnableDebug
import utils.GTimer

trait CsrUnitIO {
    this: FunctionUnit =>
    val toreg = IO(new Bundle {
        val data = ValidIO(UInt(Config.XLEN.W))
    })
}

class CsrUnit extends FunctionUnit(hasRedirect = false) with CsrUnitIO {
    val (src1, src2, func) = (io.in.bits.src1, io.in.bits.src2, io.in.bits.func)
    io.in.ready := true.B

    // src1 : from csr(t, writeback to int reg)
    // src2 : from reg
    // csr_res is writeback to csr
    val csr_res = MuxCase(
        0.U,
        Array(
            (func === CSROpType.W) -> src2.asUInt,
            (func === CSROpType.S) -> (src1 | src2).asUInt,
            (func === CSROpType.C) -> (src1 & ((~src2).asUInt)).asUInt
        )
    )
//  printf("func = 0x%x,csr_res = 0x%x\n",func,csr_res)
    io.out.valid     := io.in.valid
    io.out.bits.data := csr_res

    toreg.data.valid := io.in.valid
    toreg.data.bits  := src1

    when(GTimer() < 0.U && EnableDebug.B) {
        printf("\ntime: %d------CsrUnit--------\n", GTimer())
        printf("io.in.valid = %d\n", io.in.valid)
        printf("src1 = 0x%x,src2 = 0x%x\n", src1, src2)
        printf("io.out.valid = 0x%x,toreg.data.valid = 0x%x\n", io.out.valid, toreg.data.valid)
        printf(
            "io.out.bits.data = 0x%x,toreg.data.bits = 0x%x\n",
            io.out.bits.data,
            toreg.data.bits
        )
    }

}
