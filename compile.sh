#!/bin/bash

javac\
 -sourcepath src:mapwriter_src\
 -d bin\
 src/anvilmapper/AnvilMapper.java

# -sourcepath src:../mapwriter.git/src/main/java\
# -classpath ../apache-log4j-2.0-rc1-bin/log4j-api-2.0-rc1.jar\
# -sourcepath src\
# -sourcepath ../apache-log4j-1.2.17/src/main/java\
# -sourcepath ../apache-log4j-2.0-rc1-src/log4j-api/src/main/java\
