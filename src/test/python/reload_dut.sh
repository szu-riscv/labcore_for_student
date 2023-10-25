#!/bin/bash

curr_dir=$(pwd)
top_dir=../../../
top_v=Top.v

if [ "$1" == "-r" ]; then
    cd ../../../; make clean; make impl_rtl
fi

cd $curr_dir

cp $top_dir/build/$top_v .
python3 $curr_dir/scripts/extract_verilog_module.py -f ./$top_v -o ./Top_out

mkdir -p dut/rtl
cp Top_out/* dut/rtl/ -r