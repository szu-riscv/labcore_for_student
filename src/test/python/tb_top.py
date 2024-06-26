import logging

import cocotb
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge, Timer
from cocotbext.uart import UartSource, UartSink


class TestBench:
    def __init__(self, dut):
        self._clock = Clock(dut.clock, 20, "ns") # Create a 50MHz clock
        self.log = logging.getLogger("cocotb.tb")
        self.log.setLevel(logging.DEBUG)

        self.source = UartSource(dut.rxd, baud=115200*10) # times 10 is only for simulation
        self.sink = UartSink(dut.txd, baud=115200*10)
    
async def dut_reset(dut, time_ns):
    dut.reset.value = 1
    await Timer(time_ns, units="ns")
    # await RisingEdge(dut.clock)
    dut.reset.value = 0

async def main(dut):
    tb = TestBench(dut)
    
    # cocotb.start_soon(tb._clock.start()) # Start the clock
    # reset_thread = cocotb.start_soon(dut_reset(dut, 500))
    
    posedge = RisingEdge(dut.clock)
    
    dut.reset = 1
    for i in range(10):
        await posedge
    dut.reset = 0
    await posedge
    
    assert dut.reset.value == 0, f"reset failed: dut.reset.value == {dut.reset.value} =/= 0"
    
    await tb.source.write([0xDE, 0xED, 0xBE, 0xEF, 0xDE, 0xED, 0xBE, 0xEF]) # Magic number
    await tb.source.write([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10]) # Init size
    await tb.source.write([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]) # Init mem addr
    await tb.source.wait()
    
    await tb.source.write([0xA0, 0xB0, 0xC0, 0xD0, 0xE0, 0xF0, 0x00, 0x01]) # Init data 1
    await tb.source.write([0xAA, 0xBB, 0xCC, 0xDD, 0xEE, 0xFF, 0xFD, 0x02]) # Init data 2
    await tb.source.wait()
        
    MILLIONS = 10000
    for i in range(10 * MILLIONS):
        # cycles = dut.cycles.value.integer
        # try:
        #     v = dut.top.io_startWork.value.integer
        #     print(f"[{cycles}] {i} startWork is {v}")
        # except:
        #     print("error")
        await posedge


@cocotb.test()
async def run_test(dut):
    await main(dut)
