@echo off
chcp 65001
echo 编译并运行新解析器测试...
echo.

call gradlew.bat compileJava compileTestJava

if errorlevel 1 (
    echo 编译失败！
    pause
    exit /b 1
)

echo.
echo 运行测试...
echo.

java -cp "build\classes\java\main;build\classes\java\test" com.shuyixiao.esdsl.TestNewParser

pause

