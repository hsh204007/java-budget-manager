#!/usr/bin/env bash
set -e
cd "$(dirname "$0")"
javac -encoding UTF-8 src/*.java
java -cp src Main
