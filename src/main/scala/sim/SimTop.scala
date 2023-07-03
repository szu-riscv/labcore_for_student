package sim

import chisel3._

import difftest._

import soc._
import bus._
import device._
// import config._
import config.Config._

case class SimulatorConfig(
    memory_type: String = "2r1w",
    memory_size: Int = 256 * 1024 * 1024,
    beatBytes: Int = 8
)

class SimTop extends Module {
    val io = IO(new Bundle() {
        val logCtrl  = new LogCtrlIO
        val perfInfo = new PerfInfoIO
        val uart     = new UARTIO
    })

    require(FPGAPlatform == false, "SimTop is not prepare for FPGA implementation")
    require(DiffTest == true, "If you want to simulate with difftest, please enable difftest. (DiffTest == true)")

    val soc = Module(new SoC())
    io.uart <> soc.io.uart.get

}

object GenVerilog extends App {
    println("Generating the verilog file...")
    (new chisel3.stage.ChiselStage).emitVerilog(new SimTop(), args)
    println("The verilog was successfully generated")
}
