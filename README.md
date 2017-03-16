Anvil Mapper
============

Compilation Instructions:
=========================
* This program depends on the region file code in MapWriter.
* MapWriter is included as a git submodule, so it will automatically get the correct, custom version of MapWriter.
* On Linux, you should be able to compile the program like so:
  - git clone https://github.com/maruohon/anvilmapper.git
  - cd anvilmapper
  - bash compile.sh

Usage Instructions:
===================
* An example bash script for running the program can be found in anvilmapper.sh
* You can optionally specify the paths to the world, the output image directory, the block colors file, and the biome and block ID map overrides on the command line
* Usage: `java -cp bin anvilmapper/AnvilMapper --world /path/to/minecraft_directory/world_directory --out /path/to/output_directory --block-colors /path/to/block_colors_file --block-id-map /path/to/block_id_map --biome-id-map /path/to/biome_id_map`
* All the arguments are optional, the defaults values for them are: `world/` `images/`, `MapWriterBlockColours.txt`, `block_ids.txt`, `biome_ids.txt`
* This allows you to easily use different block color mappings for different worlds/saves
* Copy the 'index.html' file from this repo to the `--out` directory (where the `images/` directory was created while runing AnvilMapper)
* Then open the index.html in a web browser to view the map

About Block and Biome IDs:
==========================
* If you are running Anvilmapper for a vanilla world and using a recent MapWriterBlockColours.txt file (from MapWriter version probably somewhere around MC 1.8 or later?), which uses string IDs for blocks and biomes instead of numerical ones, then you need to use and specify the block and biome ID map override files containing the IDs for a vanilla world.
* This is because (at least up to MC 1.11), vanilla doesn't store an ID map anywhere.
* To generate the ID map files, you can use the TellMe mod, using the `/tellme dump blocks-id-to-registryname` and `/tellme dump biomes-id-to-name` commands
* There are also ready made ID map files for vanilla in this repo: `block_ids_vanilla.txt` and `biome_ids_vanilla.txt`
* Note that those names are different than the default names the program is looking for! So if you want to use them, then either give the argument and specify the name, or rename them.
* As of this moment, they have been generated in Minecraft 1.11.2.
* To specify the files on the launch commandline, give the `--block-id-map /path/to/block_id_map` and/or the `--biome-id-map /path/to/biome_id_map` arguments.
