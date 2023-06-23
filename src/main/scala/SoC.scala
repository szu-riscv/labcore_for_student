package soc

import chisel3._
import bus._
import core._
import config.Config._

class SoC extends Module {
    val io = IO(new Bundle() {
        val uart = if (FPGAPlatform) Some(new impl.device.UartPhysicalIO) else Some(new difftest.UARTIO)
    })

    val core = Module(new Core(hasPipeLine = true))

    if (FPGAPlatform) {
        val xbar = Module(new CpuLinkCrossBar1toN(deviceAddrSpace))
        val uart = Module(new impl.device.UART())
        val mem  = Module(new impl.device.MainMemory(implConfig.memory_size, implConfig.beatBytes))

        xbar.io.in <> core.io.mem.dmem

        uart.io.in.imem := DontCare
        uart.io.in.dmem <> xbar.io.out(0)
        mem.io.in.dmem  <> xbar.io.out(1)
        mem.io.in.imem  <> core.io.mem.imem

        io.uart.get <> uart.phyIO
    } else {
        val xbar = Module(new CpuLinkCrossBar1toN(deviceAddrSpace))
        val uart = Module(new sim.device.UART())
        val mem  = Module(new sim.device.MainMemory(simConfig.memory_size, simConfig.beatBytes))

        xbar.io.in <> core.io.mem.dmem

        uart.io.in.imem := DontCare
        uart.io.in.dmem <> xbar.io.out(0)
        mem.io.in.dmem  <> xbar.io.out(1)
        mem.io.in.imem  <> core.io.mem.imem

        io.uart.get <> uart.uartio
    }

}
