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
    frequency: Int = 50 * 1000000,   // 50 * 1000000, // 50MHz
    uartBaudRate: Int = 115200*10, // 115200    // Uart baud rate configuration
)

class Top extends RawModule {
    val io = IO(new Bundle{
        val clock = Input(Clock())
        val rst_n = Input(Bool())
        val uart = new UartPhyIO
        val statusLED = Output(Bool())
    })

    require(FPGAPlatform == true, "Top is prepare for FPGA implementation not for simulation")

    val soc = withClockAndReset(io.clock, !io.rst_n) { Module(new SoC()) }
    val startWork = soc.io.startWork
    io.uart <> soc.io.uart.get
    
    withClockAndReset(io.clock, ~io.rst_n) {
        // Status LED
        val led = RegInit(false.B)
        val ledBlinkCount = RegInit(0.U(64.W))
        ledBlinkCount := ledBlinkCount + 1.U
        io.statusLED := led
        when(~startWork) {
            when(ledBlinkCount >= (50 * 1000000).U) { // 1s
                led := ~led
                ledBlinkCount := 0.U
            }
        }.otherwise{
            when(ledBlinkCount >= (25 * 1000000).U) { // 0.5s
                led := ~led
                ledBlinkCount := 0.U
            }
        }

    }
}

object GenVerilog extends App {
    println("Generating the verilog file...")
    (new chisel3.stage.ChiselStage).emitVerilog(new Top(), args)
    println("The verilog was successfully generated")
}