/** ************************************************************************************ Copyright (c) 2020 Institute of Computing Technology, CAS Copyright (c) 2020 University of Chinese Academy of
  * Sciences
  *
  * NutShell is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at:
  * http://license.coscl.org.cn/MulanPSL2
  *
  * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR
  * PURPOSE.
  *
  * See the Mulan PSL v2 for more details.
  */

package utils

import chisel3._
import chisel3.util._
import config.Config._

object RegMap {
    def Unwritable = null

    def apply(addr: Int, reg: UInt, wfn: UInt => UInt = (x => x)): (Int, UInt, UInt => UInt) = {
        (addr, reg, wfn)
    }
    def fullMask = (~(0.U(XLEN.W))).asUInt

    def generate(
        mapping: Seq[(Int, UInt, UInt => UInt)],
        raddr: UInt,
        rdata: UInt,
        waddr: UInt,
        wen: Bool,
        wdata: UInt,
        wmask: UInt = fullMask
    ) = {
        // write
        mapping.map { case (a, r, wfn) =>
            if (wfn != null) when(wen && waddr === a.U) { r := MaskData(r, wdata, wmask) }
        }
        // read
        rdata := MuxLookup(raddr, 0.U, mapping.map { case (a, r, wfn) => (a.U === raddr) -> r })
    }

}

object LookupTree {
    def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
        Mux1H(mapping.map(p => (p._1 === key, p._2)))
}

object LookupTreeDefault {
    def apply[T <: Data](key: UInt, default: T, mapping: Iterable[(UInt, T)]): T =
        MuxLookup(key, default, mapping.toSeq)
}

object MaskedRegMap {
    def Unwritable                 = null
    def NoSideEffect: UInt => UInt = (x => x)
    def WritableMask               = Fill(64, true.B)
    def UnwritableMask             = 0.U(64.W)
    def apply(
        addr: Int,
        reg: UInt,
        wmask: UInt = WritableMask,
        wfn: UInt => UInt = (x => x),
        rmask: UInt = WritableMask
    ) = (addr, (reg, wmask, wfn, rmask))
    def generate(
        mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)],
        raddr: UInt,
        rdata: UInt,
        waddr: UInt,
        wen: Bool,
        wdata: UInt
    ): Unit = {
        val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) }
        rdata := LookupTree(raddr, chiselMapping.map { case (a, r, wm, w, rm) => (a, r & rm) })
        chiselMapping.map { case (a, r, wm, w, rm) =>
            if (w != null && wm != UnwritableMask) when(wen && waddr === a) {
                r := w(MaskData(r, wdata, wm))
            }
        }
    }
    def isIllegalAddr(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt): Bool = {
        val illegalAddr   = Wire(Bool())
        val chiselMapping = mapping.map { case (a, (r, wm, w, rm)) => (a.U, r, wm, w, rm) }
        illegalAddr := LookupTreeDefault(
            addr,
            true.B,
            chiselMapping.map { case (a, r, wm, w, rm) => (a, false.B) }
        )
        illegalAddr
    }
//  def generate(mapping: Map[Int, (UInt, UInt, UInt => UInt, UInt)], addr: UInt, rdata: UInt,
//               wen: Bool, wdata: UInt):Unit = generate(mapping, addr, rdata, addr, wen, wdata)
}
