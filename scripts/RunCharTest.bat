@echo off
chcp 65001 > nul
cd /d E:\Project\GitHub\PandaCoder

echo 编译字符分析类...
javac -encoding UTF-8 -d build\classes\java\test src\test\java\com\shuyixiao\esdsl\CharAnalysis.java
if %ERRORLEVEL% NEQ 0 (
    echo 编译失败!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo 运行字符分析...
echo.
java -Dfile.encoding=UTF-8 -cp "build\classes\java\test" com.shuyixiao.esdsl.CharAnalysis

echo.
pause

