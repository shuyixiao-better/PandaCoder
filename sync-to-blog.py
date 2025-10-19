#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PandaCoder 版本信息同步到博客脚本
使用方法: python sync-to-blog.py
"""

import re
import sys
import io
import json
from pathlib import Path
from datetime import datetime

# 设置 Windows 控制台输出编码
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

# 加载配置
def load_config():
    """加载配置文件"""
    config_file = Path('sync-config.json')
    if config_file.exists():
        with open(config_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    else:
        return {
            "blog": {
                "projectPath": r"E:\Project\博客项目\我的博客\shuyixiao-studio",
                "articlePath": "docs/articles/panda-coder-intro.md",
                "configPath": "docs/.vitepress/config.mts",
                "devServerUrl": "http://localhost:5173",
                "articleUrl": "/articles/panda-coder-intro.html"
            },
            "sync": {
                "enabled": True,
                "autoCommit": False,
                "createChangelog": True,
                "updateSidebar": True
            }
        }

CONFIG = load_config()
BLOG_PROJECT_PATH = Path(CONFIG['blog']['projectPath'])
BLOG_ARTICLE_PATH = BLOG_PROJECT_PATH / CONFIG['blog']['articlePath']
BLOG_CONFIG_PATH = BLOG_PROJECT_PATH / CONFIG['blog']['configPath']

def read_properties(file_path):
    """读取 properties 文件"""
    props = {}
    with open(file_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key, value = line.split('=', 1)
                props[key.strip()] = value.strip()
    return props

def read_version_history_from_info():
    """从 VersionInfo.java 读取最新版本的详细信息"""
    version_info_path = Path('src/main/java/com/shuyixiao/version/VersionInfo.java')
    if not version_info_path.exists():
        return None
    
    content = version_info_path.read_text(encoding='utf-8')
    
    # 提取 getVersionHistory 方法中的最新版本信息
    match = re.search(
        r'public static String getVersionHistory\(\) \{.*?return """(.*?)""";',
        content,
        re.DOTALL
    )
    
    if match:
        history_html = match.group(1).strip()
        # 提取第一个版本的信息（最新版本）
        version_match = re.search(
            r'<li><b>([\d.]+)</b>.*?<ul>(.*?)</ul>',
            history_html,
            re.DOTALL
        )
        
        if version_match:
            version = version_match.group(1)
            features_html = version_match.group(2)
            
            # 解析功能列表
            features = []
            for feature in re.findall(r'<li>(.*?)</li>', features_html):
                # 移除 HTML 标签
                feature_text = re.sub(r'<[^>]+>', '', feature)
                features.append(feature_text.strip())
            
            return {
                'version': version,
                'features': features
            }
    
    return None

def generate_version_anchor(version, date):
    """生成版本号的锚点链接"""
    # 格式: v2.0.0 (2025-10-19) -> v2-0-0-2025-10-19
    anchor = f"v{version.replace('.', '-')}-{date.replace('-', '-')}"
    return anchor.lower()

def create_version_entry(plugin_version, version_type, release_date, current_features):
    """创建版本日志条目"""
    
    # 从 VersionInfo.java 读取详细功能
    version_details = read_version_history_from_info()
    
    entry = f"""### v{plugin_version} ({release_date})

#### 🎉 新增功能
- **{current_features}**：详细功能描述
"""
    
    if version_details and version_details['features']:
        # 如果有详细功能列表，使用它
        entry = f"""### v{plugin_version} ({release_date})

#### 🎉 新增功能
"""
        for feature in version_details['features']:
            entry += f"- {feature}\n"
    
    entry += "\n---\n"
    
    return entry

def update_blog_article(plugin_version, version_type, release_date, current_features):
    """更新博客文章中的版本信息"""
    
    if not BLOG_ARTICLE_PATH.exists():
        print(f"✗ 博客文章不存在: {BLOG_ARTICLE_PATH}")
        return False
    
    content = BLOG_ARTICLE_PATH.read_text(encoding='utf-8')
    
    # 1. 更新 frontmatter 中的日期
    today = datetime.now().strftime('%Y-%m-%d')
    content = re.sub(
        r'date: \d{4}-\d{2}-\d{2}',
        f'date: {today}',
        content
    )
    
    # 2. 查找版本日志部分
    version_log_match = re.search(r'## 版本日志\n\n', content)
    
    if version_log_match:
        # 生成新版本条目
        new_version_entry = create_version_entry(plugin_version, version_type, release_date, current_features)
        
        # 在版本日志标题后插入新版本
        insert_pos = version_log_match.end()
        content = content[:insert_pos] + new_version_entry + '\n' + content[insert_pos:]
        
        print(f"✓ 已添加版本 v{plugin_version} 到版本日志")
    else:
        print("⚠ 未找到版本日志部分，跳过版本日志更新")
    
    # 写入文件
    BLOG_ARTICLE_PATH.write_text(content, encoding='utf-8')
    print(f"✓ 已更新博客文章: {BLOG_ARTICLE_PATH}")
    
    return True

def update_sidebar_config(plugin_version, release_date):
    """更新 VitePress 侧边栏配置"""
    
    if not CONFIG['sync'].get('updateSidebar', True):
        print("⚠ 侧边栏更新已禁用")
        return False
    
    if not BLOG_CONFIG_PATH.exists():
        print(f"✗ 配置文件不存在: {BLOG_CONFIG_PATH}")
        return False
    
    content = BLOG_CONFIG_PATH.read_text(encoding='utf-8')
    
    # 生成锚点链接
    anchor = generate_version_anchor(plugin_version, release_date)
    
    # 生成新的版本导航项
    new_version_item = f"            {{ text: 'v{plugin_version} ({release_date})', link: '/articles/panda-coder-intro#{anchor}' }},"
    
    # 查找版本日志的 items 数组
    pattern = r"(text: '版本日志',\s*collapsed: false,\s*items: \[\s*)"
    
    match = re.search(pattern, content)
    
    if match:
        # 检查是否已存在该版本号（不管日期）
        version_pattern = f"v{plugin_version} \\("
        if re.search(version_pattern, content):
            print(f"⚠ 版本 v{plugin_version} 已存在于侧边栏配置中，跳过添加")
            return True
        
        # 在 items 数组的开头插入新版本
        insert_pos = match.end()
        content = content[:insert_pos] + '\n' + new_version_item + content[insert_pos:]
        print(f"✓ 已添加版本 v{plugin_version} ({release_date}) 到侧边栏配置")
    else:
        print("⚠ 未找到版本日志配置部分，跳过侧边栏更新")
        return False
    
    # 写入文件
    BLOG_CONFIG_PATH.write_text(content, encoding='utf-8')
    print(f"✓ 已更新侧边栏配置: {BLOG_CONFIG_PATH}")
    
    return True

def create_changelog_entry(plugin_version, version_type, release_date, current_features):
    """创建变更日志条目"""
    
    if not CONFIG['sync'].get('createChangelog', True):
        return
    
    changelog_path = BLOG_PROJECT_PATH / "CHANGELOG.md"
    
    entry = f"""
## [{plugin_version}] - {release_date}

### {version_type}

{current_features}

---

"""
    
    if changelog_path.exists():
        content = changelog_path.read_text(encoding='utf-8')
        if '# 更新日志' in content:
            content = content.replace('# 更新日志\n', f'# 更新日志\n{entry}')
        else:
            content = f'# 更新日志\n{entry}' + content
    else:
        content = f'# 更新日志\n{entry}'
    
    changelog_path.write_text(content, encoding='utf-8')
    print(f"✓ 已更新变更日志: {changelog_path}")

def main():
    print("=" * 60)
    print("PandaCoder 版本信息同步到博客")
    print("=" * 60)
    print()
    
    # 检查博客项目路径
    if not BLOG_PROJECT_PATH.exists():
        print(f"✗ 博客项目路径不存在: {BLOG_PROJECT_PATH}")
        print("请检查 sync-config.json 中的配置")
        sys.exit(1)
    
    # 读取 gradle.properties
    props_file = Path('gradle.properties')
    if not props_file.exists():
        print("✗ 未找到 gradle.properties 文件")
        sys.exit(1)
    
    props = read_properties(props_file)
    plugin_version = props.get('pluginVersion', '')
    version_type = props.get('versionType', '')
    release_date = props.get('releaseDate', '')
    current_features = props.get('currentFeatures', '')
    
    print("当前版本信息:")
    print(f"  版本号: {plugin_version}")
    print(f"  版本类型: {version_type}")
    print(f"  发布日期: {release_date}")
    print(f"  主要功能: {current_features}")
    print()
    
    # 更新博客文章
    print("正在同步到博客...")
    article_success = update_blog_article(plugin_version, version_type, release_date, current_features)
    
    # 更新侧边栏配置
    sidebar_success = update_sidebar_config(plugin_version, release_date)
    
    # 创建变更日志
    if article_success:
        create_changelog_entry(plugin_version, version_type, release_date, current_features)
        
        print()
        print("=" * 60)
        print("✓ 同步完成！")
        print("=" * 60)
        print()
        print("已更新的文件:")
        print(f"  1. {BLOG_ARTICLE_PATH.relative_to(BLOG_PROJECT_PATH)}")
        if sidebar_success:
            print(f"  2. {BLOG_CONFIG_PATH.relative_to(BLOG_PROJECT_PATH)}")
        print(f"  3. CHANGELOG.md")
        print()
        print("下一步操作:")
        print(f"  1. 进入博客目录: cd {BLOG_PROJECT_PATH}")
        print("  2. 启动开发服务器: npm run docs:dev")
        print(f"  3. 访问: {CONFIG['blog']['devServerUrl']}{CONFIG['blog']['articleUrl']}")
        print(f"  4. 验证版本日志中是否有 v{plugin_version}")
        print(f"  5. 验证左侧导航是否显示 v{plugin_version} ({release_date})")
        print(f"  6. 点击左侧导航链接，确认能正确跳转到版本详情")
        print()
        print("确认无误后提交:")
        print(f"  git add .")
        print(f"  git commit -m 'docs: 发布 PandaCoder v{plugin_version}'")
        print(f"  git push")
        print("=" * 60)
    else:
        print()
        print("=" * 60)
        print("✗ 同步失败")
        print("=" * 60)
        sys.exit(1)

if __name__ == '__main__':
    main()
