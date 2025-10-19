#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
PandaCoder 一键更新脚本（包含博客同步）
使用方法: python update-all.py
"""

import subprocess
import sys
import io
from pathlib import Path

# 设置 Windows 控制台输出编码
if sys.platform == 'win32':
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

def run_script(script_name, description):
    """运行脚本"""
    print()
    print("=" * 60)
    print(f"▶ {description}")
    print("=" * 60)
    print()
    
    try:
        result = subprocess.run(
            [sys.executable, script_name],
            check=True,
            capture_output=False,
            text=True
        )
        return True
    except subprocess.CalledProcessError as e:
        print(f"✗ 执行失败: {script_name}")
        print(f"错误信息: {e}")
        return False
    except FileNotFoundError:
        print(f"✗ 脚本不存在: {script_name}")
        return False

def main():
    print("=" * 60)
    print("🚀 PandaCoder 一键更新工具")
    print("=" * 60)
    print()
    print("此脚本将依次执行:")
    print("  1. 更新 PandaCoder 版本信息")
    print("  2. 同步版本信息到博客")
    print()
    
    input("按 Enter 键继续...")
    
    # 步骤 1: 更新版本
    if not run_script('update-version.py', '步骤 1/2: 更新 PandaCoder 版本信息'):
        print()
        print("=" * 60)
        print("✗ 版本更新失败，已终止")
        print("=" * 60)
        sys.exit(1)
    
    # 步骤 2: 同步到博客
    if not run_script('sync-to-blog.py', '步骤 2/2: 同步版本信息到博客'):
        print()
        print("=" * 60)
        print("⚠ 博客同步失败")
        print("=" * 60)
        print()
        print("PandaCoder 版本已更新，但博客同步失败")
        print("您可以稍后手动运行: python sync-to-blog.py")
        sys.exit(1)
    
    # 完成
    print()
    print("=" * 60)
    print("🎉 所有更新完成！")
    print("=" * 60)
    print()
    print("✓ PandaCoder 版本信息已更新")
    print("✓ 博客文章已同步")
    print()
    print("下一步操作:")
    print()
    print("📦 PandaCoder 项目:")
    print("  1. 构建项目: gradlew clean build")
    print("  2. 提交代码: git add . && git commit -m 'chore: release vX.X.X'")
    print("  3. 创建标签: git tag vX.X.X")
    print("  4. 推送代码: git push && git push --tags")
    print()
    print("📝 博客项目:")
    print("  1. 进入目录: cd E:\\Project\\博客项目\\我的博客\\shuyixiao-studio")
    print("  2. 预览博客: npm run docs:dev")
    print("  3. 访问链接: http://localhost:5173/articles/panda-coder-intro.html")
    print("  4. 提交代码: git add . && git commit -m 'docs: update PandaCoder to vX.X.X'")
    print("  5. 推送代码: git push")
    print()
    print("=" * 60)

if __name__ == '__main__':
    main()

