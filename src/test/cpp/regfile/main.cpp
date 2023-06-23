#include <cstdio>
#include <cstdint>
// verilator
#include <VRegfile.h>
#include <verilated_vcd_c.h>  // Trace file format header

uint64_t vcd_times = 0;

void step(uint64_t n) {
  while (n--) {
    dut->clock = 0;
    dut->eval();
    tfp->dump(vcd_times++);

    dut->clock = 1;
    dut->eval();
    tfp->dump(vcd_times++);
  }
}

void reset(uint64_t n) {
  while (n--) {
    dut->reset = 1;
    dut->clock = 0;
    dut->eval();
    tfp->dump(vcd_times++);

    dut->clock = 1;
    dut->eval();
    dut->reset = 0;
    tfp->dump(vcd_times++);
  }
}

int main() {
  VRegfile *dut = new VRegfile;
  
  Verilated::traceEverOn(true);	// Verilator must compute traced signals
  VL_PRINTF("Enabling waves...\n");
  tfp = new VerilatedVcdC;
  dut->trace(tfp, 99);	// Trace 99 levels of hierarchy
  tfp->open("build/wave.vcd");	// Open the dump file

  // set state
  state = RUN;
  printf("\t simulation: start ...\t\n");
  reset(2);
  for (int i = 0; i < 32; i++) {
    // TODO:
    // run a cycle
    step(1);
  }
  tfp->close();
  dut->final();
  printf("\t simulation: end ...\t\n");
  return 0;
}