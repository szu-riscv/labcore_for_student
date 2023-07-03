package soc

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

import bus._
import core._
import impl._
import config.Config._
import chisel3.util.Arbiter

class SoC extends Module {
    val io = IO(new Bundle() {
        val uart = if (FPGAPlatform) Some(new impl.device.UartPhyIO) else Some(new difftest.UARTIO)
    })

    val core = Module(new Core(hasPipeLine = true))

    if (FPGAPlatform) {
        val xbar = Module(new CpuLinkCrossBar1toN(deviceAddrSpace))
        val uart = Module(new impl.device.UART())
        val mem  = Module(new impl.device.MainMemory(implConfig.memory_size, implConfig.beatBytes))
        val boot = Module(new UartBoot())
        val memArb = Module(new Arbiter(chiselTypeOf(mem.io.in.dmem.req.bits), 2))

        when(~RegNext(boot.io.startWrok) && boot.io.startWrok) {
            printf("Start work!\n")
        }
        BoringUtils.addSource(boot.io.startWrok, "startWork")

        boot.io.mem.resp <> DontCare
        boot.io.uartRxData <> uart.uartRxData


        xbar.io.in <> core.io.mem.dmem
        uart.io.in <> DontCare
        uart.fifoEnable := boot.io.startWrok
        uart.io.in.dmem <> xbar.io.out(0)
        
        memArb.io.in(0) <> boot.io.mem.req
        memArb.io.in(1) <> xbar.io.out(1).req
        memArb.io.in(1).valid := xbar.io.out(1).req.valid & boot.io.startWrok
        mem.io.in.dmem.req <> memArb.io.out

        mem.io.in.dmem.resp <> xbar.io.out(1).resp
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
