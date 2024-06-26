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

object GTimer {
    def apply() = {
        val c = RegInit(0.U(64.W))
        c := c + 1.U
        c
    }
}
