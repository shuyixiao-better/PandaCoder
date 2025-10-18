@echo off
chcp 65001 > nul
echo 编译API路径提取测试类...
javac -encoding UTF-8 -cp "build\classes\java\main;build\classes\java\test;D:\Program_Files\gradle-8.7-bin\gradle-8.7\caches\modules-2\files-2.1\com.google.code.gson\gson\2.10.1\b3add478d4382b78ea20b1671390a858002feb6c\gson-2.10.1.jar" -d build\classes\java\test src\test\java\com\shuyixiao\esdsl\TestApiPathExtraction.java

echo.
echo 运行API路径提取测试...
java -cp "build\classes\java\test;build\classes\java\main;D:\Program_Files\gradle-8.7-bin\gradle-8.7\caches\modules-2\files-2.1\com.google.code.gson\gson\2.10.1\b3add478d4382b78ea20b1671390a858002feb6c\gson-2.10.1.jar" com.shuyixiao.esdsl.TestApiPathExtraction

pause

