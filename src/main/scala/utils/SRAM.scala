package utils

import chisel3._
import chisel3.util._

class SRAMSinglePort[T <: Data](val entry: T, set: Int) extends Module {

    val entry_width: Int = entry.getWidth
    val setBits: Int     = log2Up(set)

    val io = IO(new Bundle() {
        val wen   = Input(Bool()) // write/read enable signal
        val addr  = Input(UInt(setBits.W))
        val wdata = Input(UInt(entry_width.W))
        val rdata = Output(UInt(entry_width.W))
    })

    val wordType = UInt(entry_width.W)
    // sram array
    val ram = SyncReadMem(set, wordType)

    val readEn  = !io.wen
    val writeEn = io.wen

    when(writeEn) {
        ram.write(io.addr, io.wdata)
    }
    io.rdata := ram.read(io.addr, readEn)

}
