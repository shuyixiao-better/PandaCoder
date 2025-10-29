@echo off
echo Testing ES DSL Parser Fix...
echo.

java -cp "build\classes\java\main;build\classes\java\test" com.shuyixiao.esdsl.TestNewParser > test-output.txt 2>&1

type test-output.txt

echo.
echo Output saved to test-output.txt
pause

