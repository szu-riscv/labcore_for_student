package impl

import chisel3._
import chisel3.util._

import soc._
import bus._
import device._
import config.Config._

case class ImplementationConfig(
    memAddrWidth: Int = 16,
    memDataWidth: Int = 64,
    memory_size: Int = 16 * 1024 * 1024,
    beatBytes: Int = 8
)

class Top extends Module {

    require(FPGAPlatform == true, "Top is prepare for FPGA implementation not for simulation")

    val soc = Module(new SoC())
    // io.uart <> soc.io.uart.get
}
