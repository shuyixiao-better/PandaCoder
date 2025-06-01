-ignorewarnings  # 忽略警告信息，防止因警告而中断混淆过程

# 允许 com.shuyixiao 包下的所有类进行混淆，但保留类的结构
-keep class com.shuyixiao.** { *; }

# 保留 META-INF 文件夹及其内容不被混淆
-keepdirectories META-INF/

# 优化和混淆规则
-optimizationpasses 500  # 设置优化运行的次数（默认为1），增加次数可能增加混淆效果
-allowaccessmodification  # 允许修改访问修饰符来实现更多优化
-mergeinterfacesaggressively  # 激进地合并接口，减少类的数量
-overloadaggressively  # 激进地重载方法，减少可识别的名称
-dontpreverify  # 不进行预校验，对于一些老版本的 Java 来说，这可以提高兼容性
-printmapping out.map  # 输出映射文件，记录混淆前后的类名和方法名映射关系

# 保留注解
-keepattributes *Annotation*  # 保留所有注解信息，防止因为注解丢失导致的反射或框架使用问题

# 保留 IntelliJ IDEA 插件所需的关键类和接口
-keep class * extends com.intellij.openapi.actionSystem.AnAction {
    public <init>(...);
    public void actionPerformed(...);
}

-keep public class * extends com.intellij.openapi.components.ProjectComponent {
    public <init>(...);
    public void initComponent();
    public void disposeComponent();
    public void getComponentName();
}

-keep class com.intellij.** { *; }
-keep class org.jetbrains.** { *; }
-keep interface org.jetbrains.annotations.* { *; }


# 创建无用代码和调试信息
-dontnote ** # 不输出 ProGuard 的注释信息
-dontwarn **  # 忽略所有警告信息
