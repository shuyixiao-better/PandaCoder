@echo off
echo 编译主代码...
javac -encoding UTF-8 -d target/classes src/main/java/com/shuyixiao/esdsl/model/*.java src/main/java/com/shuyixiao/esdsl/parser/*.java

echo 编译测试代码...
javac -encoding UTF-8 -cp target/classes -d target/test-classes src/test/java/com/shuyixiao/esdsl/TestNewParser.java

echo 运行测试...
java -cp "target/classes;target/test-classes" com.shuyixiao.esdsl.TestNewParser

pause

