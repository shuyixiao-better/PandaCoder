package com.shuyixiao.version;

/**
 * PandaCoder 插件版本信息管理
 * 统一管理插件版本号、版本历史和功能说明
 *
 * @author 舒一笑不秃头
 * @version 1.1.7
 * @since 2024-01-01
 */
public final class VersionInfo {
    
    // ==================== 当前版本信息 ====================
    
    /**
     * 当前版本号
     */
    public static final String CURRENT_VERSION = "1.1.8";
    
    /**
     * 当前版本类型
     */
    public static final String VERSION_TYPE = "内测版本";
    
    /**
     * 当前版本发布日期
     */
    public static final String RELEASE_DATE = "2024-12-21";
    
    /**
     * 当前版本主要功能
     */
    public static final String CURRENT_FEATURES = "新增Bug记录功能本地文件启用禁用功能";
    
    // ==================== 版本历史 ====================
    
    /**
     * 获取版本历史信息（用于Gradle构建）
     * @return 版本历史字符串
     */
    public static String getVersionHistory() {
        return """
            <ul>
                <li><b>1.1.8</b> - 🧪 <b>内测版本</b>：新增Bug记录功能本地文件启用禁用功能
                    <ul>
                        <li>🐛 <b>Bug记录工具窗口</b>：新增Bug记录功能本地文件启用禁用功能</li>
                    </ul>
                </li>
                <li><b>1.1.7</b> - 🧪 <b>内测版本</b>：新增Bug记录功能 (2024-12-21)
                    <ul>
                        <li>🐛 <b>Bug记录工具窗口</b>：新增专门的Bug记录工具窗口，方便开发过程中记录和管理问题</li>
                        <li>📝 <b>智能错误解析</b>：自动解析控制台输出的错误信息，提取关键错误信息</li>
                        <li>🔍 <b>错误类型识别</b>：自动识别不同类型的错误（编译错误、运行时错误、警告等）</li>
                        <li>📊 <b>Bug状态管理</b>：支持标记Bug状态（新建、处理中、已解决、已关闭）</li>
                        <li>⏰ <b>时间戳记录</b>：自动记录Bug发现时间和处理时间</li>
                        <li>💾 <b>本地存储</b>：Bug记录保存在本地，确保数据安全</li>
                        <li>🔧 <b>控制台监控</b>：实时监控控制台输出，自动捕获错误信息</li>
                        <li>⚙️ <b>存储配置</b>：支持配置是否启用本地文件存储</li>
                        <li>⚠️ <b>内测功能</b>：此功能目前处于内测阶段，可能存在不稳定性，请谨慎使用</li>
                    </ul>
                </li>
                <li><b>1.1.6</b> - 用户体验全面升级 (2024-11-15)
                    <ul>
                        <li>🎨 <b>现代化欢迎界面</b>：全新设计的欢迎对话框，提供更美观的用户体验</li>
                        <li>📱 <b>微信公众号集成</b>：一键关注公众号，获取最新功能更新和技术分享</li>
                        <li>💬 <b>问题反馈优化</b>：提供更便捷的问题反馈渠道，支持微信直接联系</li>
                        <li>🏢 <b>作者信息展示</b>：显示作者所在公司信息，增强用户信任度</li>
                        <li>🔧 <b>界面布局优化</b>：重新设计界面布局，信息展示更加清晰</li>
                        <li>📊 <b>功能特性展示</b>：详细展示插件核心功能，帮助用户快速了解</li>
                        <li>🎯 <b>一键开始使用</b>：简化用户操作流程，快速进入工作状态</li>
                    </ul>
                </li>
                <li><b>1.1.5</b> - 重大功能升级：新增SpringBoot配置文件图标显示功能 (2024-10-20)
                    <ul>
                        <li>🍃 <b>SpringBoot配置文件图标</b>：自动识别配置文件中的技术栈并显示对应图标</li>
                        <li>🎯 <b>技术栈识别</b>：支持MySQL、PostgreSQL、Oracle、SQL Server、Redis、Kafka、RabbitMQ、Elasticsearch等</li>
                        <li>📁 <b>多格式支持</b>：支持YAML和Properties格式的配置文件</li>
                        <li>🎨 <b>智能图标显示</b>：在编辑器左侧gutter区域显示彩色技术栈图标</li>
                        <li>🧠 <b>优先级匹配</b>：特定技术栈图标优先于通用配置图标</li>
                        <li>🖱️ <b>鼠标悬停提示</b>：显示技术栈名称和详细信息</li>
                    </ul>
                </li>
                <li><b>1.1.4</b> - 多引擎翻译系统重大升级 (2024-09-25)
                    <ul>
                        <li>🤖 <b>国内大模型支持</b>：新增通义千问、文心一言、智谱AI三大国内大模型</li>
                        <li>🌐 <b>Google Cloud Translation</b>：新增Google翻译API支持</li>
                        <li>🔄 <b>三级翻译引擎</b>：国内大模型 > Google翻译 > 百度翻译智能切换</li>
                        <li>⚙️ <b>自定义翻译提示词</b>：支持用户自定义翻译prompt，适配不同技术领域</li>
                        <li>🔧 <b>API配置验证</b>：实时验证各翻译引擎的API配置</li>
                        <li>🛡️ <b>智能错误处理</b>：优雅降级，确保功能可用性</li>
                        <li>📊 <b>配置页面优化</b>：分为4个标签页，界面更加清晰易用</li>
                    </ul>
                </li>
                <li><b>1.1.3</b> - 中文编程助手功能完善 (2024-08-30)
                    <ul>
                        <li>🎯 <b>类名前缀识别</b>：支持"Service:用户管理"格式，自动生成ServiceUserManagement等规范类名</li>
                        <li>📝 <b>自定义文件模板</b>：支持用户自定义Java文件注释模板</li>
                        <li>🔧 <b>智能精简转换</b>：自动提取核心技术词汇，去除无用词</li>
                        <li>⚙️ <b>类名前缀配置</b>：支持自定义类名前缀列表</li>
                    </ul>
                </li>
                <li><b>1.1.2</b> - 新增完整Jenkins Pipeline支持 (2024-08-15)
                    <ul>
                        <li>✨ <b>自定义Jenkins文件类型和图标显示</b></li>
                        <li>🎨 <b>11种鲜艳颜色的语法高亮(VS Code风格)</b></li>
                        <li>🔧 <b>智能补全</b>：pipeline、stage、step等关键字</li>
                        <li>🌍 <b>环境变量补全</b>：env.BUILD_NUMBER、env.WORKSPACE等</li>
                        <li>📋 <b>参数补全</b>：params.APP_NAME、params.DEPLOY_ENV等</li>
                        <li>📖 <b>悬停文档</b>：显示方法签名和参数说明</li>
                        <li>🛡️ <b>防主题覆盖</b>：5层防护确保图标在任何主题下正确显示</li>
                        <li>⚙️ <b>可自定义颜色</b>：支持在设置中调整语法高亮颜色</li>
                    </ul>
                </li>
                <li><b>1.1.1</b> - 基础功能优化 (2024-08-01)
                    <ul>
                        <li>🔧 <b>性能优化</b>：提升插件启动速度和响应性能</li>
                        <li>🐛 <b>Bug修复</b>：修复已知问题，提升稳定性</li>
                        <li>📝 <b>文档完善</b>：完善用户文档和使用说明</li>
                    </ul>
                </li>
                <li><b>1.1.0</b> - 支持IntelliJ IDEA 2024.1，升级到Java 17 (2024-07-15)
                    <ul>
                        <li>☕ <b>Java 17支持</b>：升级到Java 17，兼容最新IntelliJ IDEA</li>
                        <li>🆕 <b>IDEA 2024.1支持</b>：完全兼容IntelliJ IDEA 2024.1版本</li>
                        <li>🔧 <b>API更新</b>：适配最新的IntelliJ Platform API</li>
                    </ul>
                </li>
                <li><b>1.0.9</b> - 新增智能类创建和前缀支持 (2024-06-20)
                    <ul>
                        <li>🎯 <b>智能类创建</b>：支持中文输入快速创建Java类</li>
                        <li>📝 <b>前缀支持</b>：支持类名前缀自动识别</li>
                        <li>🔧 <b>命名规范</b>：自动转换为规范的英文类名</li>
                    </ul>
                </li>
                <li><b>1.0.8</b> - 改进翻译准确性，新增自定义模板 (2024-05-15)
                    <ul>
                        <li>🎯 <b>翻译优化</b>：提升中文到英文的翻译准确性</li>
                        <li>📝 <b>自定义模板</b>：支持用户自定义文件模板</li>
                        <li>🔧 <b>用户体验</b>：优化用户界面和操作流程</li>
                    </ul>
                </li>
                <li><b>1.0.7</b> - 首次发布，基础中英文转换功能 (2024-04-01)
                    <ul>
                        <li>🎯 <b>基础功能</b>：中文到英文的智能转换</li>
                        <li>📝 <b>命名格式</b>：支持多种命名格式转换</li>
                        <li>🔧 <b>核心架构</b>：建立插件基础架构</li>
                    </ul>
                </li>
            </ul>
            """;
    }
    
    /**
     * 获取简化版本历史信息（用于UI显示）
     * @return 简化版本历史字符串
     */
    public static String getSimpleVersionHistory() {
        return """
            <b>版本历史：</b><br>
            • <b>1.1.8</b> - 🧪 内测版本：新增Bug记录功能本地文件启用禁用功能 (2025-9-21)<br>
            • <b>1.1.7</b> - 🧪 内测版本：新增Bug记录功能 (2024-12-21)<br>
            • <b>1.1.6</b> - 用户体验全面升级 (2024-11-15)<br>
            • <b>1.1.5</b> - SpringBoot配置文件图标显示功能 (2024-10-20)<br>
            • <b>1.1.4</b> - 多引擎翻译系统重大升级 (2024-09-25)<br>
            • <b>1.1.3</b> - 中文编程助手功能完善 (2024-08-30)<br>
            • <b>1.1.2</b> - 新增完整Jenkins Pipeline支持 (2024-08-15)<br>
            • <b>1.1.1</b> - 基础功能优化 (2024-08-01)<br>
            • <b>1.1.0</b> - 支持IntelliJ IDEA 2024.1，升级到Java 17 (2024-07-15)<br>
            • <b>1.0.9</b> - 新增智能类创建和前缀支持 (2024-06-20)<br>
            • <b>1.0.8</b> - 改进翻译准确性，新增自定义模板 (2024-05-15)<br>
            • <b>1.0.7</b> - 首次发布，基础中英文转换功能 (2024-04-01)
            """;
    }
    
    /**
     * 获取当前版本信息
     * @return 当前版本信息字符串
     */
    public static String getCurrentVersionInfo() {
        return String.format("PandaCoder v%s (%s) - %s",
            CURRENT_VERSION, VERSION_TYPE, CURRENT_FEATURES);
    }
    
    /**
     * 获取版本号
     * @return 版本号
     */
    public static String getVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * 获取版本类型
     * @return 版本类型
     */
    public static String getVersionType() {
        return VERSION_TYPE;
    }
    
    /**
     * 获取发布日期
     * @return 发布日期
     */
    public static String getReleaseDate() {
        return RELEASE_DATE;
    }
    
    /**
     * 获取当前版本主要功能
     * @return 主要功能描述
     */
    public static String getCurrentFeatures() {
        return CURRENT_FEATURES;
    }
    
    /**
     * 检查是否为内测版本
     * @return 是否为内测版本
     */
    public static boolean isBetaVersion() {
        return "内测版本".equals(VERSION_TYPE) || "beta".equalsIgnoreCase(VERSION_TYPE);
    }
    
    /**
     * 获取版本比较结果
     * @param otherVersion 要比较的版本号
     * @return 比较结果：1表示当前版本更新，0表示相同，-1表示当前版本更旧
     */
    public static int compareVersion(String otherVersion) {
        if (otherVersion == null || otherVersion.isEmpty()) {
            return 1;
        }
        
        String[] currentParts = CURRENT_VERSION.split("\\.");
        String[] otherParts = otherVersion.split("\\.");
        
        int maxLength = Math.max(currentParts.length, otherParts.length);
        
        for (int i = 0; i < maxLength; i++) {
            int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
            int otherPart = i < otherParts.length ? Integer.parseInt(otherParts[i]) : 0;
            
            if (currentPart > otherPart) {
                return 1;
            } else if (currentPart < otherPart) {
                return -1;
            }
        }
        
        return 0;
    }
}
