#!/bin/bash
javac -cp ../../spigot-1.15.2.jar -d build *.java && cd build && jar -cf BedWars.jar * && cd .. && echo "DONE"
