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
    beatBytes: Int = 8,

    rxfifoEntries: Int = 64,
    txfifoEntries: Int = 64,
    frequency: Int = 50 * 1000000, // 50MHz
    uartBaudRate: Int = 115200     // Uart baud rate configuration
)

class Top extends Module {
    val io = IO(new Bundle{
        val uart = new UartPhyIO
    })

    require(FPGAPlatform == true, "Top is prepare for FPGA implementation not for simulation")

    val soc = Module(new SoC())
    io.uart <> soc.io.uart.get

}

object GenVerilog extends App {
    println("Generating the verilog file...")
    (new chisel3.stage.ChiselStage).emitVerilog(new Top(), args)
    println("The verilog was successfully generated")
}