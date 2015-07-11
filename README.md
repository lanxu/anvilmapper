Anvil Mapper
============

Compilation Instructions:
=========================
* This program depends on the region file code in MapWriter. A copy of MapWriter will be cloned via the compile.sh script.
* On Linux, you should be able to compile the program like so:
  - git clone https://github.com/maruohon/anvilmapper.git
  - cd anvilmapper
  - bash compile.sh

Usage Instructions:
===================
* An example bash script for running the program can be found in anvilmapper.sh
* You can optionally specify the output image directory and the block colours file on the command line for the program, like so:
* Usage: java -cp bin anvilmapper/AnvilMapper </path/to/minecraftdirectory/world> [/path/to/outputdirectory] [/path/to/blockcoloursfile]
* This allows you to easily use different block colour mappings for different worlds/saves
* Open 'index.html' in a web browser to view the map.
