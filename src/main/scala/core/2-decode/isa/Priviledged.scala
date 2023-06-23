package core

import chisel3._
import chisel3.util._

object Priviledged {
    def MRET    = BitPat("b001100000010_00000_000_00000_1110011")
    def SRET    = BitPat("b000100000010_00000_000_00000_1110011")
    def FENCE   = BitPat("b????????????_?????_000_?????_0001111")
    def FENCE_I = BitPat("b000000000000_00000_001_00000_0001111")

    def ECALL = BitPat("b000000000000_00000_000_00000_1110011")

  // format: off
  val table = Array(
    MRET  -> List(InstrType.r, SrcType.none, SrcType.none, FuType.csr, CSROpType.N, MemType.N, MemOpType.no, WBCtrl.CSR_PRIV),
    FENCE -> List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, CSROpType.N, MemType.N, MemOpType.no, WBCtrl.N), //nop ,don't writeback
    FENCE_I -> List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, CSROpType.N, MemType.N, MemOpType.no, WBCtrl.N), //nop ,don't writeback
    ECALL -> List(InstrType.i, SrcType.none, SrcType.none, FuType.csr, CSROpType.N, MemType.N, MemOpType.no, WBCtrl.CSR_PRIV),
    SRET -> List(InstrType.r, SrcType.none, SrcType.none, FuType.csr, CSROpType.N, MemType.N, MemOpType.no, WBCtrl.CSR_PRIV),
  )

}
