#!/bin/sh
echo "##########################################"
echo "  Gradle startup script for Linux"
echo "##########################################"

tar xvfz
chmod +x gradlew
./gradlew --stop
#cd -