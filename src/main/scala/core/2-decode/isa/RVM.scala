package core

import chisel3.util._
import config._
object RV32M {
    def DIV    = BitPat("b0000001_?????_?????_100_?????_0110011")
    def DIVU   = BitPat("b0000001_?????_?????_101_?????_0110011")
    def MUL    = BitPat("b0000001_?????_?????_000_?????_0110011")
    def MULH   = BitPat("b0000001_?????_?????_001_?????_0110011")
    def MULHSU = BitPat("b0000001_?????_?????_010_?????_0110011")
    def MULHU  = BitPat("b0000001_?????_?????_011_?????_0110011")
    def REM    = BitPat("b0000001_?????_?????_110_?????_0110011")
    def REMU   = BitPat("b0000001_?????_?????_111_?????_0110011")

  // format: off
  val table = Array(
    MUL  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.MUL, MemType.N, MemOpType.no, WBCtrl.Y),
    DIV  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.DIV, MemType.N, MemOpType.no, WBCtrl.Y),
    DIVU -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.DIVU, MemType.N, MemOpType.no, WBCtrl.Y),
    REM -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.REM, MemType.N, MemOpType.no, WBCtrl.Y),
    REMU -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.REMU, MemType.N, MemOpType.no, WBCtrl.Y),
  )
}

object RV64M{
  def DIVUW  = BitPat("b0000001_?????_?????_101_?????_0111011")
  def DIVW  = BitPat("b0000001_?????_?????_100_?????_0111011")
  def MULW  = BitPat("b0000001_?????_?????_000_?????_0111011")
  def REMUW = BitPat("b0000001_?????_?????_111_?????_0111011")
  def REMW  = BitPat("b0000001_?????_?????_110_?????_0111011")

  // format: off
  val table = RV32M.table ++ Array(
    MULW -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.MULW, MemType.N, MemOpType.no, WBCtrl.Y),
    DIVW -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.DIVW, MemType.N, MemOpType.no, WBCtrl.Y),
    REMW -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.REMW, MemType.N, MemOpType.no, WBCtrl.Y),

    DIVUW -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.DIVUW, MemType.N, MemOpType.no, WBCtrl.Y),
    REMUW -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.REMUW, MemType.N, MemOpType.no, WBCtrl.Y),


  )

}