-- H2 自举建表（仅 embedded 数据库启动时执行；生产 MySQL 不执行）
CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY,
    phone VARCHAR(20),
    password VARCHAR(100),
    nickname VARCHAR(50),
    role VARCHAR(20),
    wx_openid VARCHAR(64),
    status INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS merchant (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(100),
    logo VARCHAR(255),
    address VARCHAR(255),
    lng DOUBLE,
    lat DOUBLE,
    radius INT,
    status VARCHAR(20),
    rating DOUBLE,
    reject_reason VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS technician (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    merchant_id BIGINT,
    name VARCHAR(50),
    skill VARCHAR(100),
    lng DOUBLE,
    lat DOUBLE,
    status VARCHAR(20),
    rating DOUBLE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    parent_id BIGINT,
    sort INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS service_item (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT,
    technician_id BIGINT,
    category_id BIGINT,
    title VARCHAR(100),
    description VARCHAR(500),
    price DECIMAL(10,2),
    duration_min INT,
    available_start TIMESTAMP,
    available_end TIMESTAMP,
    status VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS biz_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(50),
    consumer_id BIGINT,
    merchant_id BIGINT,
    technician_id BIGINT,
    service_id BIGINT,
    mode VARCHAR(10),
    address VARCHAR(255),
    lng DOUBLE,
    lat DOUBLE,
    appointment_time TIMESTAMP,
    amount DECIMAL(10,2),
    status VARCHAR(20),
    pay_status VARCHAR(10),
    refund_status VARCHAR(10),
    grab_deadline TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    channel VARCHAR(20),
    trade_no VARCHAR(50),
    amount DECIMAL(10,2),
    status VARCHAR(10),
    paid_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS grab_record (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    merchant_id BIGINT,
    technician_id BIGINT,
    status VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    consumer_id BIGINT,
    score INT,
    comment VARCHAR(500),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY,
    receiver_id BIGINT,
    type VARCHAR(30),
    payload VARCHAR(1000),
    read BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted INT DEFAULT 0
);
