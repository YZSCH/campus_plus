-- 校园二手交易平台 数据库初始化脚本
CREATE DATABASE IF NOT EXISTS campus_trade_v2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_trade_v2;

-- 用户表
CREATE TABLE IF NOT EXISTS tb_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(256) NOT NULL COMMENT 'BCrypt加密密码',
    phone VARCHAR(20) NOT NULL UNIQUE COMMENT '手机号',
    student_id VARCHAR(20) UNIQUE COMMENT '学号',
    real_name VARCHAR(50) COMMENT '真实姓名',
    department VARCHAR(100) COMMENT '院系',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    credit_score INT DEFAULT 100 COMMENT '信用分',
    auth_status INT DEFAULT 0 COMMENT '认证状态 0未认证 1已认证 2失败',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色 user/admin',
    status INT NOT NULL DEFAULT 0 COMMENT '状态 0正常 1禁用',
    login_fail_count INT DEFAULT 0 COMMENT '登录失败次数',
    lock_time DATETIME COMMENT '锁定截止时间',
    created_at DATETIME, updated_at DATETIME,
    INDEX idx_phone (phone), INDEX idx_username (username), INDEX idx_student_id (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 商品表
CREATE TABLE IF NOT EXISTS tb_goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(100) NOT NULL COMMENT '商品标题',
    description TEXT COMMENT '商品描述',
    category VARCHAR(50) COMMENT '分类',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    images TEXT COMMENT '图片JSON数组',
    status VARCHAR(20) DEFAULT 'onsale' COMMENT '状态 onsale/sold/off',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    wechat VARCHAR(50) COMMENT '微信',
    location VARCHAR(100) COMMENT '所在地',
    created_at DATETIME, updated_at DATETIME,
    INDEX idx_user (user_id), INDEX idx_category (category), INDEX idx_status (status),
    FULLTEXT INDEX ft_title_desc (title, description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS tb_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    goods_id BIGINT NOT NULL COMMENT '商品ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    total_price DECIMAL(10,2) COMMENT '总价',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态 pending/paid/shipped/received/cancelled',
    shipping_method VARCHAR(50) COMMENT '交易方式',
    remark TEXT COMMENT '备注',
    created_at DATETIME, updated_at DATETIME,
    INDEX idx_buyer (buyer_id), INDEX idx_seller (seller_id), INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 评价表
CREATE TABLE IF NOT EXISTS tb_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '评价者ID',
    target_user_id BIGINT COMMENT '被评价者ID',
    goods_id BIGINT COMMENT '商品ID',
    content TEXT COMMENT '评价内容',
    rating INT COMMENT '评分1-5',
    images TEXT COMMENT '图片',
    created_at DATETIME,
    INDEX idx_user (user_id), INDEX idx_target (target_user_id), INDEX idx_goods (goods_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 消息表
CREATE TABLE IF NOT EXISTS tb_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT NOT NULL COMMENT '发送者ID',
    to_user_id BIGINT NOT NULL COMMENT '接收者ID',
    content TEXT COMMENT '消息内容',
    msg_type INT DEFAULT 0 COMMENT '消息类型 0文本 1图片',
    is_read INT DEFAULT 0 COMMENT '是否已读 0未读 1已读',
    image_url VARCHAR(500) COMMENT '图片URL',
    goods_id BIGINT COMMENT '关联商品ID',
    created_at DATETIME,
    INDEX idx_from (from_user_id), INDEX idx_to (to_user_id), INDEX idx_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';
