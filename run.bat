@echo off
cd /d %~dp0
javac -encoding UTF-8 src\*.java
java -cp src Main
pause
