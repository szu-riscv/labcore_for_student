#!/bin/bash

curr_dir=$(pwd)

if [ "$1" == "-r" ]; then
    cd ../../../; make clean; make impl_rtl
fi

cd $curr_dir

cp ../../../build/Top.v .
python3 ./scripts/extract_verilog_module.py -f ./Top.v -o ./Top_out

mkdir dut/rtl
cp Top_out/* dut/rtl/ -r