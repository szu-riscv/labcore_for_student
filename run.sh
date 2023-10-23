#!/bin/bash

make clean
export NOOP_HOME=$(pwd); make emu-run IMAGE=$(pwd)/coremark-labcore.bin
