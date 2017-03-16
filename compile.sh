#!/bin/bash

if [ ! -d "bin" ]
then
    mkdir -p bin
fi

javac -sourcepath src:. -d bin src/anvilmapper/AnvilMapper.java
