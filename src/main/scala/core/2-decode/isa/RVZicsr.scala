package core

import chisel3._
import chisel3.util._

object RVZicsr {
    // Control and Status Register Read and Write
    def CSRRW = BitPat("b?????????????????001?????1110011")
    // Control and Status Register Read and Set
    def CSRRS = BitPat("b?????????????????010?????1110011")
    // Control and Status Register Read and Clear
    def CSRRC = BitPat("b?????????????????011?????1110011")

    def CSRRWI = BitPat("b?????????????????101?????1110011")
    def CSRRSI = BitPat("b?????????????????110?????1110011")
    def CSRRCI = BitPat("b?????????????????111?????1110011")

  // format: off
  val table = Array(
    CSRRW  -> List(InstrType.z, SrcType.reg, SrcType.none, FuType.csr, CSROpType.W, MemType.N, MemOpType.no, WBCtrl.Y),
    CSRRS  -> List(InstrType.z, SrcType.reg, SrcType.none, FuType.csr, CSROpType.S, MemType.N, MemOpType.no, WBCtrl.Y),
//    CSRRC  -> List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.CSR_W),
    CSRRWI  -> List(InstrType.z, SrcType.imm, SrcType.none, FuType.csr, CSROpType.W, MemType.N, MemOpType.no, WBCtrl.Y),
//    CSRRSI -> List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.CSR_W),
//    CSRRCI  -> List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.CSR_W),

  )
}
