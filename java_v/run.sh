#!/bin/bash
echo $1
if [ $1 -eq '1' ]
then
	java com/craftinginterpreters/lox/Lox
else
	java com/craftinginterpreters/lox/Lox ./test.lox
fi
