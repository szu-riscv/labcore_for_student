# sim
BUILD_DIR = ./build
SIM_TOP = SimTop
MillTarget = sim.GenVerilog
MillModule = SZU_RISCV

SIM_TOP_V = $(BUILD_DIR)/$(SIM_TOP).v


COLOR_RED   = \033[1;31m
COLOR_GREEN = \033[1;32m
COLOR_NONE  = \033[0m

# scala files
SCALA_SRC_DIR = ./src/main/scala/soc ./src/main/scala/simulator ./src/main/scala/bus ./src/main/scala/utils
SCALA_FILE = $(shell find $(SCALA_SRC_DIR) -name '*.scala')

# outstream
TIMELOG = $(BUILD_DIR)/time.log


$(SIM_TOP_V): $(SCALA_FILE)
	mkdir -p $(@D)
	@echo "\n[mill] Generating Verilog files..." > $(TIMELOG)
	mill -i ${MillModule}.runMain $(MillTarget) -td $(@D) --emission-options disableRegisterRandomization

sim-verilog: $(SIM_TOP_V)
default: sim-verilog
# verilator simulation
emu: $(SIM_TOP_V)
	$(MAKE) -C ./difftest emu SIM_TOP=$(SIM_TOP) DESIGN_DIR=$(NOOP_HOME) EMU_TRACE=1

emu-run:
	$(MAKE) -C ./difftest emu-run SIM_TOP=$(SIM_TOP) DESIGN_DIR=$(NOOP_HOME) EMU_TRACE=0

emu-clean:
	$(MAKE) -C ./difftest emu-clean SIM_TOP=$(SIM_TOP) DESIGN_DIR=$(NOOP_HOME) 

clean:
	$(MAKE) -C ./difftest clean
	rm -rf ./build

init:
	git submodule update --init

idea:
	mill -i mill.scalalib.GenIdea/idea

TEST_DIR ?= /home/lin/workspace/lab-test/tests/performance/coremark/build/labcore
#TEST_DIR ?= /home/lin/workspace/lab-test/riscv-tests/isa/build/p/labcore
#TEST_DIR ?= /home/lin/workspace/lab-test/tests/futest/cputest/build/labcore
#TEST_DIR ?= /home/lin/workspace/lab-test/tests/performance/dhrystone/build/labcore
test_file := $(wildcard $(test_dir)/*.bin)

RESULT = .result

all:
	for files in $(wildcard $(TEST_DIR)/*.bin); do \
  		if make emu-run IMAGE=$$files; then \
  			printf "$(COLOR_GREEN)PASS\t" >> $(RESULT); \
  			echo $$files >> $(RESULT); \
		else \
		  	printf "$(COLOR_RED)ERROR\t" >> $(RESULT) ; \
		  	echo $$files >> $(RESULT); \
	  	fi \
	done
	@cat $(RESULT)
	@rm $(RESULT)


format:
	mill $(MillModule).reformat

.PHONY: emu emu-run sim-verilog clean emu-clean init idea
