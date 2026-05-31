#!/usr/bin/env python3
# -*- coding: utf-8 -*-

with open(r'D:\JAVA IDEA\JAVA\work1\src\main\resources\static\index.html', 'rb') as f:
    content = f.read()

# 查找仲裁管理
arb_text = '仲裁管理'.encode('utf-8')
idx = content.find(arb_text)

if idx > 0:
    # 显示前面的内容（查找<span class="mi2i">标签）
    start = max(0, idx - 60)
    end = idx + 20
    snippet = content[start:end]
    print('找到仲裁管理，前后内容：')
    print(snippet.decode('utf-8', errors='replace'))
    print()
    print('十六进制：')
    print(snippet.hex(' '))
