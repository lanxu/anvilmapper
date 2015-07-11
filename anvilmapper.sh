#!/bin/bash

# This script was made for a case where it can be used to render multiple
# different Minecraft servers/worlds. The server/world in question is given as
# the first argument to this script. All the server instances are located inside the
# MINECRAFT_BASE_DIR directory, like MINECRAFT_BASE_DIR/myservername.
# The output is written to OUTPUT_BASE_DIR/myservername

# The BlockColours file is given as the second argument. It can either be
# the "semi-extension" in the name, like: MapWriterBlockColours_thenamegoeshere.txt
# if the different block colour files are stored in the directory above the anvilmapper.git
# directory. Or it can be the full path to the block colours file.

ANVILMAPPER_DIR=/path/to/anvilmapper.git
MINECRAFT_BASE_DIR=/path/to/game_servers/minecraft/servers
OUTPUT_BASE_DIR=/path/to/webserver/anvilmapper

if [ $# -ne 2 ]
then
	echo "Usage: $0 <server instance name> <block colours file type>"
	echo "Example 1: $0 vanillaserver vanilla"
	echo "Example 2: $0 ftb_dw20_164 modded_164"
	exit 1
fi

PWD=`pwd`
cd ${ANVILMAPPER_DIR}
BLOCK_COLOURS_FILE="../MapWriterBlockColours_${2}.txt"

if [ -f "${2}" ]
then
	BLOCK_COLOURS_FILE=${2}
fi

java -cp bin anvilmapper/AnvilMapper "${MINECRAFT_BASE_DIR}/${1}/world" "${OUTPUT_BASE_DIR}/${1}" "${BLOCK_COLOURS_FILE}"

cd ${PWD}
