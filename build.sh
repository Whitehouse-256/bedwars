#!/bin/bash
javac -Xlint:deprecation -cp ../../spigot-1.15.2.jar -d build src/*.java && cd build && jar -cf BedWars.jar * && cd .. && echo "DONE"
