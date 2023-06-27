#!/bin/bash

if [ "$1" == "-r" ]; then # remake
    bash ./clean.sh
fi

make -j 16