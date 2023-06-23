/** ************************************************************************************* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences Copyright (c) 2020-2021
  * Peng Cheng Laboratory
  *
  * XiangShan is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at:
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

object MaskExpand {
    def apply(m: UInt) = Cat(m.asBools.map(Fill(8, _)).reverse)
}

object MaskData {
    def apply(oldData: UInt, newData: UInt, fullmask: UInt) = {
        require(
            oldData.getWidth <= fullmask.getWidth,
            s"${oldData.getWidth} < ${fullmask.getWidth}"
        )
        require(
            newData.getWidth <= fullmask.getWidth,
            s"${newData.getWidth} < ${fullmask.getWidth}"
        )
        (newData & fullmask) | (oldData & ~fullmask)
    }
}

object SignExt {
    def apply(a: UInt, len: Int) = {
        val aLen    = a.getWidth
        val signBit = a(aLen - 1)
        if (aLen >= len) a(len - 1, 0) else Cat(Fill(len - aLen, signBit), a)
    }
}
