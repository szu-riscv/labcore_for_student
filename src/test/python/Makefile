# Makefile for cocotb
# The following variables are makefile variables:

# Makefile-based Test Scripts
# ---------------------------
# GUI                       Set this to 1 to enable the GUI mode in the simulator
# SIM                       Selects which simulator Makefile to use
# WAVES                     Enable wave traces dump for Riviera-PRO and Questa
# VERILOG_SOURCES           A list of the Verilog source files to include
# VHDL_SOURCES              A list of the VHDL source files to include
# VHDL_SOURCES_<lib>        VHDL source files to include in *lib* (GHDL/ModelSim/Questa/Xcelium only)
# VHDL_LIB_ORDER            Compilation order of VHDL libraries (needed for ModelSim/Questa/Xcelium)
# SIM_CMD_PREFIX            Prefix for simulation command invocations
# COMPILE_ARGS              Arguments to pass to compile stage of simulation
# SIM_ARGS                  Arguments to pass to execution of compiled simulation
# EXTRA_ARGS                Arguments for compile and execute phases
# PLUSARGS                  Plusargs to pass to the simulator
# COCOTB_HDL_TIMEUNIT       Default time unit for simulation
# COCOTB_HDL_TIMEPRECISION  Default time precision for simulation
# CUSTOM_COMPILE_DEPS       Add additional dependencies to the compilation target
# CUSTOM_SIM_DEPS           Add additional dependencies to the simulation target
# SIM_BUILD                 Define a scratch directory for use by the simulator
# SCRIPT_FILE               Simulator script to run (for e.g. wave traces)


TOPLEVEL_LANG = verilog
TOPLEVEL = tb_top
MODULE = tb_top

SIM ?= vcs
# SIM ?= icarus
# SIM ?= verilator

PRJ_DIR = $(shell pwd)
DUT_DIR = ${PRJ_DIR}/dut
TOP_PRJ_DIR = $(realpath ../../../)
DIFFTEST_DIR = $(realpath ../../../difftest)
DUT_FILES = $(DUT_DIR)/dut_files.f
SIM_FILES = $(DUT_DIR)/sim_files.f

include $(shell cocotb-config --makefiles)/Makefile.sim

DIFFTEST_CSRC_DIR = $(DIFFTEST_DIR)/src/test/csrc
CFLAGS += $(addprefix -I,$(wildcard $(DIFFTEST_CSRC_DIR)/*)) $(addprefix -I,$(wildcard $(DIFFTEST_CSRC_DIR)/*/include))
CFLAGS += -std=c++11 -static -Wall -DNUM_CORES=1
LDFLAGS += -lpthread -lSDL2 -ldl -lz -lsqlite3


CSRCS += $(shell find $(DIFFTEST_DIR)/src/test/csrc/difftest -name "*.cpp")
CSRCS += $(shell find $(DIFFTEST_DIR)/src/test/csrc/plugin -name "*.cpp")
CSRCS += $(shell find $(DIFFTEST_DIR)/src/test/csrc/common -name "*.cpp")
CSRCS += $(shell find $(DIFFTEST_DIR)/src/test/csrc/vcs -name "*.cpp")


$(DUT_FILES):
	$(shell find $(DUT_DIR) -name "*.v" -or -name "*.sv" > $@)

$(SIM_FILES): $(DUT_FILES)
	-rm $@
	@cat $(DUT_FILES) >> $@
	@echo $(CSRCS) >> $@

.PHONY: gen_sim_files $(SIM_FILES) $(DUT_FILES)
gen_sim_files: $(SIM_FILES)

ifeq ($(SIM), verilator)
VERILOG_SOURCES = $(shell find ${DUT_DIR} -name "*.v" -or -name "*.sv")
EXTRA_ARGS += --trace --trace-structs --timescale-override 1ns/1ns  # enable verilator logging waveform
EXTRA_ARGS += --threads 4 --no-timing
endif

ifeq ($(SIM), icarus)
VERILOG_SOURCES = $(shell find ${DUT_DIR} -name "*.v" -or -name "*.sv")
COMPILE_ARGS += -g2012
endif

ifeq ($(SIM), vcs)
COMPILE_ARGS += -debug_all -debug_access+all
COMPILE_ARGS += -CFLAGS "$(CFLAGS)" -LDFLAGS "$(LDFLAGS)" -f $(SIM_FILES)
PLUSARGS += +dump_enable
PLUSARGS += +workload=$(TOP_PRJ_DIR)/coremark-labcore.bin
endif
