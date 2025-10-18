@echo off
chcp 65001 > nul
cd /d E:\Project\GitHub\PandaCoder

echo 编译调试测试类...
javac -encoding UTF-8 -cp "build\classes\java\main" -d build\classes\java\test src\test\java\com\shuyixiao\esdsl\DebugLogTest.java
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo 运行调试测试...
echo.
java -Dfile.encoding=UTF-8 -cp "build\classes\java\main;build\classes\java\test" com.shuyixiao.esdsl.DebugLogTest

echo.
pause

