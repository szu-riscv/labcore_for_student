#!/bin/bash

make clean
sh env.sh
make emu-run IMAGE=$(pwd)/coremark-labcore.bin
