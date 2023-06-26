package impl

import chisel3._
import chisel3.util._

import  bus._
import config.Config._
import firrtl.options.DoNotTerminateOnExit


class UartDeserializer(from: Int = 8, to: Int = 64) extends Module {
    val io = IO(new Bundle{
        val input = Flipped(ValidIO(UInt(from.W)))
        val output = ValidIO(UInt(to.W))
    })

    require(to % from == 0)

    val multiple = to / from
    val counter = RegInit(0.U(log2Ceil(to/from).W))
    val outputReg = RegInit(0.U(to.W)).asTypeOf(Vec(multiple, UInt(from.W)))
    val finish = io.input.fire && counter === (multiple - 1).U

    when(finish) {
        counter := 0.U
    }.elsewhen(io.input.fire) {
        counter := counter + 1.U
    }
    
    when(io.input.fire) {
        outputReg(counter) := io.input.bits
    }

    io.output.valid := finish
    io.output.bits := outputReg.asUInt
}


// This module is used for receive boot data(code) from uart and boot the whole SoC
class UartBoot extends Module {
    val io = IO(new Bundle{
        val uartRxData = Flipped(ValidIO(UInt(8.W)))
        val startWrok = Output(Bool())
        val mem = MasterCpuLinkBus() // output
    })

    val blockFire = WireInit(false.B) // receive one block of data from uart, here one block == 4byte == 64-bit of data
    val blockData = RegInit(0.U(XLEN.W))
    val uartDes = Module(new UartDeserializer(8, XLEN))
    uartDes.io.input <> io.uartRxData
    blockFire := uartDes.io.output.fire
    blockData := uartDes.io.output.bits


    val recvMagicNum :: recvInitSize :: initMemPtr :: initMemData :: startWork :: Nil = Enum(5)
    val bootState = RegInit(recvMagicNum)
    when(reset.asBool()) {
        bootState := recvMagicNum
    }


    val initMemAddr = RegInit(0.U(XLEN.W))
    val memAddr = RegInit(0.U(XLEN.W))
    val totalInitSize = RegInit(0.U(XLEN.W)) // size of the received uart data (bytes)
    
    val magicNum = RegInit(0.U(XLEN.W))
    def MAGIC_NUM = "hdeed_beef".U(XLEN.W)

    io.mem <> DontCare
    io.startWrok := false.B

    when(bootState === recvMagicNum) {
        when(blockFire) {
            magicNum := blockData
        }

        // TODO: need a LED for status indicate (breathing led)
        when(RegNext(blockFire)) {
            when(magicNum === MAGIC_NUM) {
                bootState := recvInitSize
            }.otherwise{ // unknown magic number
                bootState := recvMagicNum
                magicNum := 0.U
            }
        }
    }.elsewhen(bootState === recvInitSize) {
        when(blockFire) {
            totalInitSize := blockData
            bootState := initMemPtr
        }
    }.elsewhen(bootState === initMemPtr) {
        when(blockFire) {
            memAddr := blockData
            initMemAddr := blockData
            bootState := initMemData
        }
    }.elsewhen(bootState === initMemData) {
        io.mem.req.valid := blockFire
        io.mem.req.bits.addr := memAddr
        io.mem.req.bits.size := log2Ceil(XLEN / 8).U
        io.mem.req.bits.cmd := CpuLinkCmd.store_req
        io.mem.req.bits.wdata := blockData
        io.mem.req.bits.strb := Fill(XLEN / 8, 1.U)
        memAddr := memAddr + (XLEN / 8).U

        // TODO: timeout ?
        when(memAddr - initMemAddr === totalInitSize) {
            bootState := startWork
        }
    }.elsewhen(bootState === startWork) {

        io.startWrok := true.B
    }

    

}