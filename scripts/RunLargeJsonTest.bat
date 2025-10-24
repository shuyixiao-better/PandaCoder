@echo off
chcp 65001 >nul
cd /d "%~dp0.."

echo ========================================
echo 测试大JSON SQL解析功能
echo ========================================
echo.

REM 编译项目
echo [1/2] 编译项目...
call gradle jar -q
if %errorlevel% neq 0 (
    echo 编译失败
    exit /b 1
)

echo.
echo [2/2] 运行测试...
echo.

REM 使用Groovy脚本运行测试（避免编码问题）
java -cp "build/libs/PandaCoder-2.1.0.jar" groovy.lang.GroovyShell scripts/test-large-json.groovy

pause

