#!/bin/bash


if [ ! -d "mapwriter" ] || [ ! -d "mapwriter/region/" ]
then
    git clone https://github.com/maruohon/mapwriter.git mapwriter
    cd mapwriter
    git checkout -b for_anvilmapper facb64e39fd070de8de4bcaed17a9945f7cb479b
    cd ..
fi

if [ ! -d "bin" ]
then
    mkdir -p bin
fi

javac -sourcepath src:. -d bin src/anvilmapper/AnvilMapper.java
