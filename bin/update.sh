#!/bin/bash
echo "update expr and stmt --------------------------"
javac com/craftinginterpreters/tool/*.java
java com.craftinginterpreters.tool.GenerateAst "com/craftinginterpreters/lox"
java com.craftinginterpreters.tool.GenerateStmt "com/craftinginterpreters/lox"
echo "update source -------------------------------"
find ./ -regex ".*\.java" > source.txt
javac @source.txt -verbose
