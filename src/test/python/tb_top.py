import cocotb
from cocotb.clock import Clock
from cocotb.triggers import FallingEdge, RisingEdge, Combine


async def dut_reset(dut, cycles = 1):
    posedge = RisingEdge(dut.clock)
    dut.reset.value = 1
    for _ in range(0, cycles):
        await posedge
    dut.reset.value = 0
    await posedge

async def dut_posedge(dut, cycles = 1):
    # ! takes more time than barely use "posedge = RisingEdge(dut.clock)"
    posedge = RisingEdge(dut.clock)
    for _ in range(0, cycles):
        await posedge

async def main(dut):
    posedge = RisingEdge(dut.clock)

    clock = Clock(dut.clock, 2, units="us")  # Create a 10us period clock on port clk
    cocotb.start_soon(clock.start())  # Start the clock
    await posedge  # Wait for at least one clock cycle
    
    await dut_reset(dut, 10)
    await dut_posedge(dut, 10)
    
    for i in range(10):
        dut.txd.value = 1
        await posedge
    
    for i in range(5):
        dut.txd.value = 0
        await posedge
        
    dut.txd.value = 1
    
    for i in range(30):
        dut.txd.value = 1
        await posedge


@cocotb.test()
async def run_test(dut):
    await main(dut)