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

        self.source = UartSource(dut.rxd, baud=115200)
        self.sink = UartSink(dut.txd, baud=115200)
    
async def dut_reset(dut, time_ns):
    dut.reset.value = 1
    await Timer(time_ns, units="ns")
    dut.reset.value = 0

async def main(dut):
    tb = TestBench(dut)
    
    cocotb.start_soon(tb._clock.start()) # Start the clock
    
    reset_thread = cocotb.start_soon(dut_reset(dut, 500))
    
    posedge = RisingEdge(dut.clock)
    await posedge
    
    await Timer(100, 'ns')
    tb.log.info("start reset!")
    await reset_thread
    tb.log.info("reset done!")

    assert dut.reset.value == 0, f"reset failed: dut.reset.value == {dut.reset.value} =/= 0"
    
    await tb.source.write([0xDE, 0xED, 0xBE, 0xEF, 0xDE, 0xED, 0xBE, 0xEF]) # Magic number
    await tb.source.wait()

    await tb.source.write([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08]) # Init size
    await tb.source.wait()
    
    await tb.source.write([0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]) # Init mem addr
    await tb.source.wait()
    
    await tb.source.write([0xA0, 0xB0, 0xC0, 0xD0, 0xE0, 0xF0, 0x00, 0x01]) # Init data
    await tb.source.wait()
    
    await Timer(1, units='us')


@cocotb.test()
async def run_test(dut):
    await main(dut)
