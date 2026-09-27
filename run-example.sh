#!/bin/sh
set -eu
mkdir -p out
javac -d out src/main/java/*.java src/test/java/*.java
java -cp out AccountDeletionServiceTest
