#!/usr/bin/env python3
# -*- coding: utf-8 -*-

file_path = r'D:\JAVA IDEA\JAVA\work1\src\main\resources\static\index.html'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 修复仲裁管理的图标：从空改为📝
old_arb = '<span class="mi2i">️</span>仲裁管理'
new_arb = '<span class="mi2i">📝</span>仲裁管理'

# 修复违规管理的图标：从空改为️
old_vio = '<span class="mi2i">️</span>违规管理'
new_vio = '<span class="mi2i">⚠️</span>违规管理'

content = content.replace(old_arb, new_arb)
content = content.replace(old_vio, new_vio)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print('已添加图标：')
print('  仲裁管理: ')
print('  违规管理: ⚠️')
print('\n请刷新浏览器查看效果。')
