package core

import chisel3._
import chisel3.util._

import difftest.DifftestCSRState

import utils._
import config.Config._

object CSROpType {
    val N = 0.U
    val R = 1.U
    val S = 2.U
    val C = 3.U
    val W = 4.U
}

trait CSRConst {
    val XLEN         = 64
    val ADDRLEN: Int = 12

    // mstatus MPP
    def U_Mode = 0x0.U
    def S_Mode = 0x1.U
    def M_Mode = 0x3.U
}

object Priv {
    val M = "b11".U
    val S = "b01".U
    val U = "b00".U
}

object ExceptionCode {
    def InstrAddrMisaligned = 0
    def InstrAccessFault    = 1
    def IllegalInstr        = 2
    def BreakPoint          = 3
    def LoadAddrMisaligned  = 4
    def LoadAccessFault     = 5
    def StoreAddrMisaligned = 6
    def StoreAccessFault    = 7
    def EcallU              = 8
    def EcallVU             = 8
    def EcallS              = 9
    def EcallHS             = 9
    def EcallVS             = 10
    def EcallM              = 11
    def InstrPageFault      = 12
    def LoadPageFault       = 13
    def StorePageFault      = 15
    def InstrGuestPageFault = 20
    def LoadGuestPageFault  = 21
    def VirtualInstruction  = 22
    def StoreGuestPageFault = 23

    val total = 24

    def Priority = Seq(
        BreakPoint,
        InstrPageFault,
        InstrGuestPageFault,
        InstrAccessFault,
        IllegalInstr,
        VirtualInstruction,
        InstrAddrMisaligned,
        EcallM,
        EcallS,
        EcallVS,
        EcallU,
        StorePageFault,
        LoadPageFault,
        StoreGuestPageFault,
        LoadGuestPageFault,
        StoreAccessFault,
        LoadAccessFault,
        StoreAddrMisaligned,
        LoadAddrMisaligned
    )
}

object CSRAddr {
    // Machine Information Registers
    val mvendorid: Int  = 0xf11
    val marchid: Int    = 0xf12
    val mimpid: Int     = 0xf13
    val mhartid: Int    = 0xf14
    val mconfigptr: Int = 0xf15

    // Machine Trap Setup
    val mstatus: Int    = 0x300
    val misa: Int       = 0x301
    val medeleg: Int    = 0x302
    val mideleg: Int    = 0x303
    val mie: Int        = 0x304
    val mtvec: Int      = 0x305
    val mcounteren: Int = 0x306

    // Machine Trap Handling
    val mscratch: Int = 0x340
    val mepc: Int     = 0x341
    val mcause: Int   = 0x342
    val mtval: Int    = 0x343
    val mip: Int      = 0x344
    val mtinst: Int   = 0x34a
    val mtval2: Int   = 0x34b
}

class MstatusBundle extends Bundle with CSRConst {
    val sd    = UInt(1.W)
    val wpri4 = if (XLEN == 64) UInt(25.W) else null
    val mbe   = if (XLEN == 64) UInt(1.W) else null
    val sbe   = if (XLEN == 64) UInt(1.W) else null
    val sxl   = if (XLEN == 64) UInt(2.W) else null
    val uxl   = if (XLEN == 64) UInt(2.W) else null
    val wpri3 = if (XLEN == 64) UInt(9.W) else UInt(8.W)
    val tsr   = UInt(1.W)
    val tw    = UInt(1.W)
    val tvm   = UInt(1.W)
    val mxr   = UInt(1.W)
    val sum   = UInt(1.W)
    val mprv  = UInt(1.W)
    val xs    = UInt(2.W)
    val fs    = UInt(2.W)
    val mpp   = UInt(2.W)
    val vs    = UInt(2.W)
    val spp   = UInt(1.W)
    val mpie  = UInt(1.W)
    val ube   = UInt(1.W)
    val spie  = UInt(1.W)
    val wpri2 = UInt(1.W)
    val mie   = UInt(1.W)
    val wpri1 = UInt(1.W)
    val sie   = UInt(1.W)
    val wpri0 = UInt(1.W)

    assert(this.getWidth == XLEN)
}

class MtvecBundle extends Bundle {
    val base = UInt((XLEN - 2).W)
    val mode = UInt(2.W)
}

class SatpBundle extends Bundle {
    val mode = UInt(4.W)
    val asid = UInt(16.W)
    val zero = UInt(2.W)
    val ppn  = UInt(44.W)

    def apply(satp: UInt): Unit = {
        require(satp.getWidth == XLEN)
        val s = satp.asTypeOf(new SatpBundle)
        mode := s.mode
        asid := s.asid
        ppn  := s.ppn
    }
}

class CSRtoMMUBundle extends Bundle {
    val i_mode = UInt(2.W)
    val d_mode = UInt(2.W)
    val satp   = new SatpBundle
}

class WBUtoCSRBundle extends Bundle {
    val pc         = UInt(VAddrBits.W)
    val instr      = UInt(32.W)
    val exceptions = Vec(ExceptionCode.total, Bool())
    val wdata      = UInt(XLEN.W)
    val csr_cmd    = WBCtrl()
    val rs1        = UInt(XLEN.W)
}

class CSRtoDecodeBundle extends Bundle {
    val csr_addr = Input(UInt(12.W))
    val csr_data = Output(UInt(XLEN.W))
}

class CSR extends Module with CSRConst {
    val io = IO(new Bundle() {
        val wb = Flipped(ValidIO(new WBUtoCSRBundle))
        val read = new CSRtoDecodeBundle()
        val redirect = Output(new RedirectIO)
    })

    val (valid, cmd, pc, wdata) = (io.wb.valid, io.wb.bits.csr_cmd, io.wb.bits.pc, io.wb.bits.wdata)
    
    val priviledgeMode = RegInit(Priv.M) // default in M mode

    // M-mode CSR
    val mstatus = RegInit(UInt(XLEN.W), "ha00001800".U)
    val mtvec   = RegInit(UInt(XLEN.W), 0.U)
    val mcause  = RegInit(UInt(XLEN.W), 0.U)

    val mepc = Reg(UInt(XLEN.W))

    val mie     = RegInit(0.U(XLEN.W))
    val mip     = RegInit(0.U(XLEN.W))
    val medeleg = RegInit(0.U(XLEN.W))
    val mideleg = RegInit(0.U(XLEN.W))

    // address : [0xf11,0xf15]
    val mvendorid = RegInit(UInt(XLEN.W), 0.U) // Vendor ID
    val marchid   = RegInit(UInt(XLEN.W), 0.U) // Architecture ID
    val mimpid    = RegInit(UInt(XLEN.W), 0.U) // Implementation ID
    val mhartid   = RegInit(UInt(XLEN.W), 0.U) // Hardwwware thread ID

    
    // CSR Map
    val mstatusWmask = "h0000_0000_0000_ffff".U(64.W)
    val csr_mapping = Map(
        MaskedRegMap(CSRAddr.mstatus, mstatus.asUInt, mstatusWmask),
        MaskedRegMap(CSRAddr.mtvec, mtvec),
        MaskedRegMap(CSRAddr.mcause, mcause),
        MaskedRegMap(CSRAddr.mepc, mepc),
        MaskedRegMap(CSRAddr.medeleg, medeleg, "hbbff".U(XLEN.W)),
        MaskedRegMap(CSRAddr.mideleg, mideleg)
    )

    
    val (waddr, wen)   = (io.wb.bits.instr(31, 20), io.wb.valid)
    val (raddr, rdata) = (io.read.csr_addr, io.read.csr_data)

    MaskedRegMap.generate(csr_mapping, raddr, rdata, waddr, wen, wdata)

    when(GTimer() > 0.U && EnableDebug.B) {
        printf("\ntime: %d -----CSR------\n", GTimer())
        printf("wen= %d\n", wen)
        printf("waddr = 0x%x\t wdata = 0x%x\n", waddr, wdata)
        printf("mstatus = 0x%x\n", mstatus)
        printf("mtvec = 0x%x\n", mtvec)
        printf("mcause = 0x%x\n", mcause)
        printf("mepc = 0x%x\n", mepc)
        printf("medeleg = 0x%x\n", medeleg)
    }


    // Privileged Instruction mret sret ecall
    val inst    = io.wb.bits.instr
    val isMret  = valid && (inst === Priviledged.MRET)
    val isEcall = valid && (inst === Priviledged.ECALL)
    val illegalMret = valid && isMret && priviledgeMode < Priv.M

    // mret
    val retTarget    = WireInit(0.U)
    val mstatusValue = WireInit(mstatus.asTypeOf(new MstatusBundle))
    when(isMret && !illegalMret) {
        val mstatusNew = WireInit(mstatusValue)
        mstatusNew.mpp  := Priv.U
        mstatusNew.mie  := mstatusValue.mpie
        mstatusNew.mpie := true.B
        priviledgeMode  := mstatusValue.mpp

        mstatus   := mstatusNew.asUInt
        retTarget := mepc(VAddrBits - 1, 0)
    }


    // Exception
    val exceptionVec = WireInit(io.wb.bits.exceptions)
    exceptionVec(ExceptionCode.EcallU) := isEcall && priviledgeMode === Priv.U
    exceptionVec(ExceptionCode.EcallS) := isEcall && priviledgeMode === Priv.S
    exceptionVec(ExceptionCode.EcallM) := isEcall && priviledgeMode === Priv.M

    val hasException = valid && exceptionVec.asUInt.orR
    val exceptionNo  = ExceptionCode.Priority.foldRight(0.U)((i: Int, sum: UInt) => Mux(exceptionVec(i), i.U, sum))


    // TODO: Interrupt
    val hasIntr            = WireInit(false.B)
    val intrNo             = WireInit(0.U)
    val hasIntrOrException = hasException || hasIntr
    val cause              = Mux(hasIntr, intrNo, exceptionNo)
    val epc                = Mux(hasIntr, pc + 4.U, pc)


    when(hasIntrOrException) {
        val mstatusNew   = WireInit(mstatusValue)
        priviledgeMode  := Priv.M
        mstatusNew.mpp  := priviledgeMode
        mstatusNew.mpie := mstatusNew.mie
        mstatusNew.mie  := false.B
        mepc            := epc
        mstatus         := mstatusNew.asUInt
        mcause          := cause
    }


    // Redirect
    val mtvecStruct = WireInit(mtvec.asTypeOf(new MtvecBundle))
    val trapTarget = Mux(
        mtvecStruct.mode === 0.U,
        Cat(mtvecStruct.base, Fill(2, 0.U)),
        Cat(mtvecStruct.base, Fill(2, 0.U)) + mcause << 2.U
    )
    io.redirect.valid  := isMret || hasIntrOrException
    io.redirect.target := Mux(hasIntrOrException, trapTarget, retTarget)


    // MMU Ctrl Signals
    // io.toMMU := DontCare  // TODO:


    // Difftest csr
    if (DiffTest) {
        val difftest_csr = Module(new DifftestCSRState)
        difftest_csr.suggestName("difftest_csr")

        difftest_csr.io.clock  := clock
        difftest_csr.io.coreid := 0.U
//    difftest_csr.io.priviledgeMode  := "b11".U
        difftest_csr.io.priviledgeMode := priviledgeMode
        difftest_csr.io.mstatus        := mstatus.asUInt
        difftest_csr.io.sstatus        := "h0000000200000000".U
        difftest_csr.io.mepc           := mepc
        difftest_csr.io.sepc           := 0.U
        difftest_csr.io.mtval          := 0.U
        difftest_csr.io.stval          := 0.U
        difftest_csr.io.mtvec          := mtvec
        difftest_csr.io.stvec          := 0.U
        difftest_csr.io.mcause         := mcause
        difftest_csr.io.scause         := 0.U
        difftest_csr.io.satp           := 0.U
        difftest_csr.io.mip            := 0.U
        difftest_csr.io.mie            := mie
        difftest_csr.io.mscratch       := 0.U
        difftest_csr.io.sscratch       := 0.U
        difftest_csr.io.mideleg        := 0.U
        difftest_csr.io.medeleg        := medeleg
    }

}
