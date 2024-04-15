package demo

import chisel3._
import chisel3.util._
import config.Config._

class RegfileReadIO extends Bundle {
  val addr = Input(UInt(5.W))
  val data = Output(UInt(XLEN.W))
}

class RegfileWriteIO extends Bundle {
  val addr = Input(UInt(5.W))
  val data = Input(UInt(XLEN.W))
  val wen = Input(Bool())
}

class Regfile extends Module {
  val io = IO(new Bundle() {
    val read = Vec(2, new RegfileReadIO)
    val write = new RegfileWriteIO
  })

  val regfiles = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))

  // read
  for (i <- 0 until 2) {
    io.read(i).data := Mux(io.read(i).addr =/= 0.U, regfiles(io.read(i).addr), 0.U)
  }

  // write
  when (io.write.addr =/= 0.U && io.write.wen) {
    regfiles(io.write.addr) := io.write.data
  }
}

// Generate the Verilog code
object Regfile extends App {

  println("Generating the Regfile")
  (new chisel3.stage.ChiselStage).emitVerilog(new Regfile(), Array("--target-dir", "build"))
}