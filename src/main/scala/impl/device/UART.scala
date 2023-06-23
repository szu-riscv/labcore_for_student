package impl.device

import chisel3._
import chisel3.util._

import config.Config._
import sim.device._

class UartPhysicalIO() extends Bundle {
    val txd = Output(Bool())
    val rxd = Input(Bool())
}

class UART() extends BaseDevice {
    val phyIO = IO(new UartPhysicalIO)
    // TODO:

}
