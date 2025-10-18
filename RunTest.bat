@echo off
chcp 65001
cd /d E:\Project\GitHub\PandaCoder
javac -encoding UTF-8 -cp "build\classes\java\main" -d build\classes\java\test src\test\java\com\shuyixiao\esdsl\TestRealLogProcessing.java
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)
java -cp "build\classes\java\main;build\classes\java\test" com.shuyixiao.esdsl.TestRealLogProcessing
pause

