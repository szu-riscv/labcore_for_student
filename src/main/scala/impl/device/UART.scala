package impl.device

import chisel3._
import chisel3.util._

import config.Config._
import sim.device._
import bus._
import utils._


trait HasUartCst {
    val frequency = implConfig.frequency
    val baudRate  = implConfig.uartBaudRate
}

class UartTX extends Module with HasUartCst {
    val io = IO(new Bundle {
        val txd     = Output(Bool())
        val channel = Flipped(DecoupledIO(UInt(8.W)))
    })

    // val BIT_CNT = ((frequency + baudRate / 2) / baudRate - 1).asUInt()
    val BIT_CNT = (frequency / baudRate).U - 1.U

    val shiftReg = RegInit(0x7ff.U)
    val cntReg   = RegInit(0.U(20.W))
    val bitsReg  = RegInit(0.U(4.W))

    io.channel.ready := (cntReg === 0.U) && (bitsReg === 0.U)
    io.txd           := shiftReg(0)

    when(cntReg === 0.U) {

        cntReg := BIT_CNT
        when(bitsReg =/= 0.U) {
            val shift = shiftReg >> 1
            shiftReg := Cat(1.U, shift(9, 0))
            bitsReg  := bitsReg - 1.U
        }.otherwise {
            when(io.channel.valid) {
                shiftReg := Cat(Cat(3.U, io.channel.bits), 0.U) // two stop bits, data, one start bit
                bitsReg  := 11.U
            }.otherwise {
                shiftReg := 0x7ff.U
            }
        }

    }.otherwise {
        cntReg := cntReg - 1.U
    }
}

/** Receive part of the UART. A minimal version without any additional buffering. Use a ready/valid handshaking.
  *
  * The following code is inspired by Tommy's receive code at: https://github.com/tommythorn/yarvi
  */
class UartRX extends Module with HasUartCst {
    val io = IO(new Bundle {
        val rxd     = Input(Bool())
        val channel = DecoupledIO(UInt(8.W))
    })

    val BIT_CNT   = (frequency / baudRate).U - 1.U
    val START_CNT = ((3 * frequency / baudRate) / 2).U

    // Sync in the asynchronous RX data, reset to 1 to not start reading after a reset
    val rxReg = RegNext(RegNext(io.rxd, 1.U), 1.U)

    val shiftReg = RegInit(0.U(8.W))
    val cntReg   = RegInit(0.U(20.W))
    val bitsReg  = RegInit(0.U(4.W))
    val valReg   = RegInit(false.B)

    when(cntReg =/= 0.U) {
        cntReg := cntReg - 1.U
    }.elsewhen(bitsReg =/= 0.U) {
        cntReg   := BIT_CNT
        shiftReg := Cat(rxReg, shiftReg >> 1)
        bitsReg  := bitsReg - 1.U
        // the last shifted in
        when(bitsReg === 1.U) {
            valReg := true.B
        }
    }.elsewhen(rxReg === 0.U) { // wait 1.5 bits after falling edge of start
        cntReg  := START_CNT
        bitsReg := 8.U
    }

    when(valReg && io.channel.ready) {
        valReg := false.B
    }

    io.channel.bits  := shiftReg
    io.channel.valid := valReg
}

/** A single byte buffer with a ready/valid interface
  */
class Buffer extends Module {
    val io = IO(new Bundle {
        val in  = Flipped(DecoupledIO(UInt(8.W)))
        val out = DecoupledIO(UInt(8.W))
    })

    val empty :: full :: Nil = Enum(2)
    val stateReg             = RegInit(empty)
    val dataReg              = RegInit(0.U(8.W))

    io.in.ready  := stateReg === empty
    io.out.valid := stateReg === full

    when(stateReg === empty) {
        when(io.in.valid) {
            dataReg  := io.in.bits
            stateReg := full
        }
    }.otherwise { // full
        when(io.out.ready) {
            stateReg := empty
        }
    }
    io.out.bits := dataReg
}

/** A transmitter with a single buffer.
  */
class BufferedUartTX extends Module {
    val io = IO(new Bundle {
        val txd     = Output(Bool())
        val channel = Flipped(DecoupledIO(UInt(8.W)))
    })
    val tx  = Module(new UartTX)
    val buf = Module(new Buffer())

    buf.io.in     <> io.channel
    tx.io.channel <> buf.io.out
    io.txd        <> tx.io.txd
}

/** Send a string.
  */
class Sender extends Module {
    val io = IO(new Bundle {
        val txd = Output(Bool())
    })

    val tx = Module(new BufferedUartTX)

    io.txd := tx.io.txd

    val msg  = "Hello World!"
    val text = VecInit(msg.map(_.U))
    val len  = msg.length.U

    val cntReg = RegInit(0.U(8.W))

    tx.io.channel.bits  := text(cntReg)
    tx.io.channel.valid := cntReg =/= len

    when(tx.io.channel.ready && cntReg =/= len) {
        when(cntReg === len - 1.U) {
            cntReg := 0.U
        }.otherwise {
            cntReg := cntReg + 1.U
        }
    }
}

class Echo extends Module {
    val io = IO(new Bundle {
        val txd = Output(Bool())
        val rxd = Input(Bool())
    })
    // io.txd := RegNext(io.rxd)
    val tx = Module(new BufferedUartTX)
    val rx = Module(new UartRX)
    io.txd        := tx.io.txd
    rx.io.rxd     := io.rxd
    tx.io.channel <> rx.io.channel
}


class UartPhyIO() extends Bundle {
    val txd = Output(Bool())
    val rxd = Input(Bool())
}

class UART() extends BaseDevice {
    val phyIO = IO(new UartPhyIO)
    val fifoEnable = IO(Input(Bool()))
    val uartRxData = IO(ValidIO(UInt(8.W)))

    // TODO:
    phyIO <> DontCare
    rdata := DontCare

    val uartRx = Module(new UartRX)
    val uartTx = Module(new BufferedUartTX)

    uartRx.io.rxd := phyIO.rxd
    phyIO.txd := uartTx.io.txd

    // to boot module
    uartRxData.valid := uartRx.io.channel.valid && !fifoEnable
    uartRxData.bits := uartRx.io.channel.bits


    val rxfifo = Module(new Queue(UInt(8.W), implConfig.rxfifoEntries))
    val txfifo = Module(new Queue(UInt(8.W), implConfig.txfifoEntries))

    rxfifo.io.enq.valid := uartRx.io.channel.valid && fifoEnable
    rxfifo.io.enq.bits := uartRx.io.channel.bits
    uartRx.io.channel.ready := rxfifo.io.enq.ready

    io.in.dmem.resp.valid := rxfifo.io.deq.valid
    io.in.dmem.resp.bits.data := rxfifo.io.deq.bits
    rxfifo.io.deq.ready := io.in.dmem.resp.ready
    

    txfifo.io.enq.valid := wen && waddr(3, 0) === 4.U
    txfifo.io.enq.bits := wdata(7, 0)
    io.in.dmem.req.ready := txfifo.io.enq.ready

    uartTx.io.channel <> txfifo.io.deq


}