#!/bin/bash
find ./ -regex ".*\.java" > source.txt
javac @source.txt -verbose
