@echo off
chcp 65001 > nul
echo 编译完整场景测试类...
javac -encoding UTF-8 -cp "build\classes\java\main;build\classes\java\test;D:\Program_Files\gradle-8.7-bin\gradle-8.7\caches\modules-2\files-2.1\com.google.code.gson\gson\2.10.1\b3add478d4382b78ea20b1671390a858002feb6c\gson-2.10.1.jar" -d build\classes\java\test src\test\java\com\shuyixiao\esdsl\TestFullScenario.java

echo.
echo 运行完整场景测试...
echo.
java -cp "build\classes\java\test;build\classes\java\main;D:\Program_Files\gradle-8.7-bin\gradle-8.7\caches\modules-2\files-2.1\com.google.code.gson\gson\2.10.1\b3add478d4382b78ea20b1671390a858002feb6c\gson-2.10.1.jar" com.shuyixiao.esdsl.TestFullScenario

pause

