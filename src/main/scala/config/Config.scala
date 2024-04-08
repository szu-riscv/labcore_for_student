package config

import chisel3._

import sim.SimulatorConfig
import impl.ImplementationConfig
import bus._

object Config {
    val XLEN: Int      = 64
    val AddrBits: Int  = 32
    val VAddrBits: Int = 32
    val PAddrBits: Int = 32
    val ResetPC        = "h8000_0000"

    val DiffTest: Boolean     = true // Simulation Enable
    val FPGAPlatform: Boolean = false 
    val EnableDebug: Boolean  = false
    
    // AddrSpace: (begin, size)
    val deviceAddrSpace = List(
        (0x40600000L, 0x10L),
        (0x80000000L, 0x80000000L)
    )

    val simConfig: SimulatorConfig = SimulatorConfig(
        memory_type = "2r1w",
        memory_size = 256 * 1024 * 1024, // 256 MB
        beatBytes = 8
    )

    val implConfig: ImplementationConfig = ImplementationConfig(
        memAddrWidth = 16,
        memory_size = 16 * 1024 * 1024, // 16 MB
        beatBytes = 8,                  // 64-bit
        memDataWidth = XLEN
    )

}
