#!/bin/bash

if [ "$1" == "-r" ]; then # remake
    bash ./clean.sh
fi

make gen_sim_files

make -j 16