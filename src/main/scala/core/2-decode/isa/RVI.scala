package core

import chisel3._
import chisel3.util._
import config._

object RV32I {

    // RV32 instructions
    def ADDI  = BitPat("b????????????_?????_000_?????_0010011")
    def SLLI  = if (Config.XLEN == 32) BitPat("b0000000?????_?????_001_?????_0010011") else BitPat("b000000??????_?????_001_?????_0010011")
    def SLTI  = BitPat("b????????????_?????_010_?????_0010011")
    def SLTIU = BitPat("b????????????_?????_011_?????_0010011")
    def XORI  = BitPat("b????????????_?????_100_?????_0010011")
    def SRLI  = if (Config.XLEN == 32) BitPat("b0000000?????_?????_101_?????_0010011") else BitPat("b000000??????_?????_101_?????_0010011")
    def ORI   = BitPat("b????????????_?????_110_?????_0010011")
    def ANDI  = BitPat("b????????????_?????_111_?????_0010011")
    def SRAI  = if (Config.XLEN == 32) BitPat("b0100000?????_?????_101_?????_0010011") else BitPat("b010000??????_?????_101_?????_0010011")

    def ADD  = BitPat("b0000000_?????_?????_000_?????_0110011")
    def SLL  = BitPat("b0000000_?????_?????_001_?????_0110011")
    def SLT  = BitPat("b0000000_?????_?????_010_?????_0110011")
    def SLTU = BitPat("b0000000_?????_?????_011_?????_0110011")
    def XOR  = BitPat("b0000000_?????_?????_100_?????_0110011")
    def SRL  = BitPat("b0000000_?????_?????_101_?????_0110011")
    def OR   = BitPat("b0000000_?????_?????_110_?????_0110011")
    def AND  = BitPat("b0000000_?????_?????_111_?????_0110011")
    def SUB  = BitPat("b0100000_?????_?????_000_?????_0110011")
    def SRA  = BitPat("b0100000_?????_?????_101_?????_0110011")

    def AUIPC = BitPat("b????????????????????_?????_0010111")
    def LUI   = BitPat("b????????????????????_?????_0110111")

    def JAL  = BitPat("b????????????????????_?????_1101111")
    def JALR = BitPat("b????????????_?????_000_?????_1100111")

    def BNE  = BitPat("b???????_?????_?????_001_?????_1100011")
    def BEQ  = BitPat("b???????_?????_?????_000_?????_1100011")
    def BLT  = BitPat("b???????_?????_?????_100_?????_1100011")
    def BGE  = BitPat("b???????_?????_?????_101_?????_1100011")
    def BLTU = BitPat("b???????_?????_?????_110_?????_1100011")
    def BGEU = BitPat("b???????_?????_?????_111_?????_1100011")

    def LB  = BitPat("b????????????_?????_000_?????_0000011")
    def LH  = BitPat("b????????????_?????_001_?????_0000011")
    def LW  = BitPat("b????????????_?????_010_?????_0000011")
    def LBU = BitPat("b????????????_?????_100_?????_0000011")
    def LHU = BitPat("b????????????_?????_101_?????_0000011")
    def SB  = BitPat("b???????_?????_?????_000_?????_0100011")
    def SH  = BitPat("b???????_?????_?????_001_?????_0100011")
    def SW  = BitPat("b???????_?????_?????_010_?????_0100011")

    def ECALL = BitPat("b00000000000000000000000001110011")

    // decode info table
  // format: off
  val table = Array(
    AUIPC -> List(InstrType.u, SrcType.pc,  SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.Y),

    ADDI  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.Y),

    LUI   -> List(InstrType.u, SrcType.none, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.Y),

    // Control Transfer Instructions
    JAL   -> List(InstrType.j, SrcType.pc,  SrcType.imm, FuType.jbu, JBUType.jal,   MemType.N, MemOpType.no, WBCtrl.Y),
    JALR  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.jalr,  MemType.N, MemOpType.no, WBCtrl.Y),

    //branch                                   ///imm !
    BEQ   -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.beq,  MemType.N, MemOpType.no, WBCtrl.N),
    BGE   -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.bge,  MemType.N, MemOpType.no, WBCtrl.N),
    BNE   -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.bne,  MemType.N, MemOpType.no, WBCtrl.N),
    BGEU  -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.bgeu,  MemType.N, MemOpType.no, WBCtrl.N),
    BLT   -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.blt,  MemType.N, MemOpType.no, WBCtrl.N),
    BLTU  -> List(InstrType.b, SrcType.reg, SrcType.imm, FuType.jbu, JBUType.bltu,  MemType.N, MemOpType.no, WBCtrl.N),


    // load and store
    LW    -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lw, WBCtrl.Y),
    LBU   -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lbu,WBCtrl.Y),
    LB    -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lb,WBCtrl.Y),
    LHU   -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lhu, WBCtrl.Y),
    LH    -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lh, WBCtrl.Y),

    SW    -> List(InstrType.s, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.sw, WBCtrl.N),
    SB    -> List(InstrType.s, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.sb, WBCtrl.N),
    SH    -> List(InstrType.s, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.sh, WBCtrl.N),

    ANDI  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.AND, MemType.N, MemOpType.no, WBCtrl.Y),
    AND   -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.AND, MemType.N, MemOpType.no, WBCtrl.Y),
    SLLI -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SLLI, MemType.N, MemOpType.no, WBCtrl.Y),
    SRLI -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SHIFTRIGHT, MemType.N, MemOpType.no, WBCtrl.Y),
    SRAI -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SRAI, MemType.N, MemOpType.no, WBCtrl.Y),

    //
    ADD    -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.Y),

    OR     -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.OR, MemType.N, MemOpType.no, WBCtrl.Y),
    ORI    -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.OR, MemType.N, MemOpType.no, WBCtrl.Y),

    XOR    -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.XOR, MemType.N, MemOpType.no, WBCtrl.Y),
    XORI   -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.XOR, MemType.N, MemOpType.no, WBCtrl.Y),

    SUB    -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SUB, MemType.N, MemOpType.no, WBCtrl.Y),
    SLTIU  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.LESSTHAN_U, MemType.N, MemOpType.no, WBCtrl.Y),
    SLTU  -> List(InstrType.i, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.LESSTHAN_U, MemType.N, MemOpType.no, WBCtrl.Y),
    SLTI  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SLTI, MemType.N, MemOpType.no, WBCtrl.Y),

    SLL  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SLL, MemType.N, MemOpType.no, WBCtrl.Y),
    SLT  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SLT, MemType.N, MemOpType.no, WBCtrl.Y),

    SRL  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SRL, MemType.N, MemOpType.no, WBCtrl.Y),
    SRA  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SRA, MemType.N, MemOpType.no, WBCtrl.Y),


    // Environment Call and Breakpoints
//    ECALL ->  List(InstrType.i, SrcType.none, SrcType.none, FuType.alu, ALUOpType.ADD, MemType.N, MemOpType.no, WBCtrl.CSR_PRIV)
  )
}

object RV64I {
  // RV64 instructions
  def ADDIW   = BitPat("b???????_?????_?????_000_?????_0011011")
  def SLLIW   = BitPat("b0000000_?????_?????_001_?????_0011011")
  def SRLIW   = BitPat("b0000000_?????_?????_101_?????_0011011")
  def SRAIW   = BitPat("b0100000_?????_?????_101_?????_0011011")
  def SLLW    = BitPat("b0000000_?????_?????_001_?????_0111011")
  def SRLW    = BitPat("b0000000_?????_?????_101_?????_0111011")
  def SRAW    = BitPat("b0100000_?????_?????_101_?????_0111011")
  def ADDW    = BitPat("b0000000_?????_?????_000_?????_0111011")
  def SUBW    = BitPat("b0100000_?????_?????_000_?????_0111011")

  def LWU     = BitPat("b???????_?????_?????_110_?????_0000011")
  def LD      = BitPat("b???????_?????_?????_011_?????_0000011")
  def SD      = BitPat("b???????_?????_?????_011_?????_0100011")

  // format: off
  val table = RV32I.table ++ Array(
    ADDW   -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.ADDW,   MemType.N, MemOpType.no, WBCtrl.Y),
    ADDIW  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADDIW,   MemType.N, MemOpType.no, WBCtrl.Y),
    SUBW  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SUBW,   MemType.N, MemOpType.no, WBCtrl.Y),

    SLLIW  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SLLIW, MemType.N, MemOpType.no, WBCtrl.Y),
    SLLW  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SLLW, MemType.N, MemOpType.no, WBCtrl.Y),
    SRAIW  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SRAIW, MemType.N, MemOpType.no, WBCtrl.Y),
    SRLIW  -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.SRLIW, MemType.N, MemOpType.no, WBCtrl.Y),
    SRAW  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SRAW, MemType.N, MemOpType.no, WBCtrl.Y),
    SRLW  -> List(InstrType.r, SrcType.reg, SrcType.reg, FuType.alu, ALUOpType.SRLW, MemType.N, MemOpType.no, WBCtrl.Y),

    LD     -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.ld, WBCtrl.Y),
    LWU   -> List(InstrType.i, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD, MemType.Y, MemOpType.lwu, WBCtrl.Y),

    SD     -> List(InstrType.s, SrcType.reg, SrcType.imm, FuType.alu, ALUOpType.ADD,   MemType.Y, MemOpType.sd, WBCtrl.N)
  )
}

