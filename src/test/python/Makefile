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

SIM ?= verilator

PRJ_DIR = $(shell pwd)
DUT_DIR = ${PRJ_DIR}/dut
VERILOG_SOURCES = $(shell find ${DUT_DIR} -name "*.v" -or -name "*.sv")
EXTRA_ARGS += --trace --trace-structs --timescale-override 1ns/1ns  # enable verilator logging waveform
EXTRA_ARGS += --threads 4 --timing

include $(shell cocotb-config --makefiles)/Makefile.sim

PLUSARGS += +dump_enable