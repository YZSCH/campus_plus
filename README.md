# 🏫 CampusTrade · 校园二手交易平台

> 基于 Spring Boot + JPA + JWT 的校园二手交易平台  
> 专为高校学生打造的安全、便捷、专属的闲置物品流转平台

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.7.18-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-11-blue" alt="Java">
  <img src="https://img.shields.io/badge/MySQL-8.0-orange" alt="MySQL">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

---

## ✨ 功能特性

### 👤 用户模块
- 手机号注册 + 短信验证码（模拟）
- 用户名/手机号 + 角色选择登录（普通用户/管理员）
- JWT Token 认证（支持记住我，2小时/7天有效期）
- 连续错误锁定机制（5次失败锁定10分钟）
- 同一账号最多3个终端在线

### 🎓 校园认证
- 用户提交学号+姓名认证申请
- **管理员审核制**：提交后由管理员通过/拒绝
- 认证状态实时更新

### 📦 商品管理
- 商品发布（标题、分类、价格、描述、联系方式）
- 多分类浏览（教材教辅、电子数码、生活用品等）
- 关键字搜索
- 按分类筛选
- 商品详情查看

### 📋 订单管理
- 创建订单（从聊天中一键下单）
- 我买到的 / 我卖出的 双视角
- 卖家确认/拒绝订单
- 订单状态追踪

### 💬 即时消息
- 站内信系统，双向对话
- 2秒轮询，实时聊天体验
- 未读消息计数（红色角标）
- 聊天中可直接创建订单

### ⚙️ 管理后台
- 管理员专用账号
- 校园认证审核（通过/拒绝）

---

## 🚀 快速开始

### 前置条件

| 工具 | 版本 |
|------|------|
| JDK | 11+ |
| MySQL | 8.0+ |

> 无需 Maven，在 VS Code 中打开项目直接运行即可（VS Code Java 插件会自动编译）

### 步骤

```bash
# 1. 克隆项目
git clone https://github.com/shenglingxn/-.git
cd campus-trade

# 2. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS campus_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 修改数据库密码
# 编辑 src/main/resources/application.yml
# 将 spring.datasource.password 改为你自己的 MySQL 密码

# 4. 用 VS Code 打开项目，运行 CampusTradeApplication.java
# 或使用 Maven：
mvn spring-boot:run
```

#### 访问

浏览器打开 **http://localhost:8080**

---

## 🧪 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 👑 管理员 | `admin` | `admin123456` | 系统管理员，可审核认证 |
| 👤 测试用户A | `alice` | `abc123456` | 已认证，有发布商品 |
| 👤 测试用户B | `bob` | `abc123456` | 未认证，可体验认证流程 |

### 万能验证码

注册时验证码输入 **`000000`** 即可通过。

---

## 🏗 技术架构

```
┌─────────────────────────────────┐
│     Frontend (SPA)              │
│     HTML5 + CSS3 + Vanilla JS   │
├─────────────────────────────────┤
│     Spring Boot 2.7 (REST)      │
│  ├── Spring Security (JWT)      │
│  ├── Spring Data JPA            │
│  └── Spring WebSocket           │
├─────────────────────────────────┤
│         MySQL 8.0               │
└─────────────────────────────────┘
```

### API 接口

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | ❌ |
| POST | `/api/user/login` | 用户登录 | ❌ |
| POST | `/api/user/send-code` | 发送验证码 | ❌ |
| POST | `/api/user/logout` | 退出登录 | ✅ |
| GET | `/api/user/me` | 获取当前用户信息 | ✅ |
| GET | `/api/user/info/{id}` | 获取用户基本信息 | ✅ |
| POST | `/api/user/auth` | 提交校园认证 | ✅ |
| GET | `/api/user/auth/status` | 查看本人认证状态 | ✅ |
| GET | `/api/admin/auth/pending` | 待审核认证列表（管理员） | ✅ |
| POST | `/api/admin/auth/review` | 审核认证（管理员） | ✅ |
| GET | `/api/goods/list` | 商品列表（支持分类/搜索） | ❌ |
| GET | `/api/goods/{id}` | 商品详情 | ❌ |
| POST | `/api/goods/create` | 发布商品 | ✅ |
| GET | `/api/goods/mine` | 我的发布 | ✅ |
| POST | `/api/order/create` | 创建订单 | ✅ |
| GET | `/api/order/buy` | 我买到的订单 | ✅ |
| GET | `/api/order/sell` | 我卖出的订单 | ✅ |
| POST | `/api/message/send` | 发送消息 | ✅ |
| GET | `/api/message/conversation` | 获取对话 | ✅ |
| GET | `/api/message/unread` | 未读消息数 | ✅ |

---

## 📄 许可证

本项目基于 MIT 许可证开源。
