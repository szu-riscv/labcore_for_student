package core

import chisel3._
import chisel3.util._

object SelfDefineTrap {
    def TRAP = BitPat("b000000000000?????000000001101011")

  // format: off
  val table = Array(
    TRAP -> List(InstrType.trap, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.N)
  )
}
