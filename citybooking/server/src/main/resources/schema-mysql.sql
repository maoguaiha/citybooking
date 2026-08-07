-- MySQL DDL for dev/prod. Executed by spring.sql.init on startup.
-- All secondary indexes are defined INSIDE CREATE TABLE. Tables use
--   CREATE TABLE IF NOT EXISTS so re-running is idempotent: when a table
--   already exists it (and its inline indexes) is skipped entirely, so we
--   never hit "Duplicate key" on a CREATE INDEX.
-- NOTE: MySQL does NOT support CREATE INDEX IF NOT EXISTS / DROP INDEX IF EXISTS.
-- NOTE: `read` is a MySQL reserved word; always quote it with backticks.

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY,
    phone VARCHAR(20),
    password VARCHAR(100),
    nickname VARCHAR(50),
    role VARCHAR(20),
    wx_openid VARCHAR(64),
    status INT,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_app_user_phone (phone),
    INDEX idx_app_user_wx_openid (wx_openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_merchant_user_id (user_id),
    INDEX idx_merchant_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_technician_merchant_id (merchant_id),
    INDEX idx_technician_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50),
    parent_id BIGINT,
    sort INT,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_category_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS service_item (
    id BIGINT PRIMARY KEY,
    merchant_id BIGINT,
    technician_id BIGINT,
    category_id BIGINT,
    title VARCHAR(100),
    description VARCHAR(500),
    price DECIMAL(10,2),
    duration_min INT,
    available_start DATETIME,
    available_end DATETIME,
    status VARCHAR(10),
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_service_merchant_id (merchant_id),
    INDEX idx_service_category_id (category_id),
    INDEX idx_service_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
    appointment_time DATETIME,
    amount DECIMAL(10,2),
    status VARCHAR(20),
    pay_status VARCHAR(10),
    refund_status VARCHAR(10),
    grab_deadline DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    UNIQUE INDEX uk_order_no (order_no),
    INDEX idx_order_consumer_id (consumer_id),
    INDEX idx_order_merchant_id (merchant_id),
    INDEX idx_order_service_id (service_id),
    INDEX idx_order_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS payment (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    channel VARCHAR(20),
    trade_no VARCHAR(50),
    amount DECIMAL(10,2),
    status VARCHAR(10),
    paid_at DATETIME,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_payment_order_id (order_id),
    INDEX idx_payment_trade_no (trade_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS grab_record (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    merchant_id BIGINT,
    technician_id BIGINT,
    status VARCHAR(10),
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_grab_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS review (
    id BIGINT PRIMARY KEY,
    order_id BIGINT,
    consumer_id BIGINT,
    score INT,
    comment VARCHAR(500),
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_review_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY,
    receiver_id BIGINT,
    type VARCHAR(30),
    payload VARCHAR(1000),
    `read` BOOLEAN,
    created_at DATETIME,
    updated_at DATETIME,
    deleted INT DEFAULT 0,
    INDEX idx_notice_receiver_id (receiver_id),
    INDEX idx_notice_read (`read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
