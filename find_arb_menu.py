#!/usr/bin/env python3
# -*- coding: utf-8 -*-

with open(r'D:\JAVA IDEA\JAVA\work1\src\main\resources\static\index.html', 'rb') as f:
    content = f.read()

# 查找包含"仲裁管理"和span class="mi2i"的行
lines = content.decode('utf-8', errors='replace').split('\n')
for i, line in enumerate(lines, 1):
    if '仲裁管理' in line and 'mi2i' in line:
        print(f'第 {i} 行:')
        print(line)
        print()
