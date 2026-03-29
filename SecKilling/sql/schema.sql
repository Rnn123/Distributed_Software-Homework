SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `seckill` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `seckill`;

DROP TABLE IF EXISTS `order`;
DROP TABLE IF EXISTS `inventory`;
DROP TABLE IF EXISTS `product`;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'user id',
  `username` varchar(50) NOT NULL COMMENT 'username',
  `password` varchar(128) NOT NULL COMMENT 'password hash',
  `phone` varchar(20) NOT NULL COMMENT 'phone',
  `email` varchar(50) DEFAULT NULL COMMENT 'email',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='user table';

CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'product id',
  `name` varchar(100) NOT NULL COMMENT 'product name',
  `original_price` decimal(10,2) NOT NULL COMMENT 'original price',
  `seckill_price` decimal(10,2) NOT NULL COMMENT 'seckill price',
  `image_url` varchar(255) DEFAULT NULL COMMENT 'image url',
  `description` text COMMENT 'product description',
  `start_time` datetime DEFAULT NULL COMMENT 'start time',
  `end_time` datetime DEFAULT NULL COMMENT 'end time',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='product table';

CREATE TABLE `inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'inventory id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `total_stock` int NOT NULL DEFAULT 0 COMMENT 'total stock',
  `available_stock` int NOT NULL DEFAULT 0 COMMENT 'available stock',
  `frozen_stock` int NOT NULL DEFAULT 0 COMMENT 'frozen stock',
  `version` int NOT NULL DEFAULT 0 COMMENT 'version',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='inventory table';

CREATE TABLE `order` (
  `id` bigint NOT NULL COMMENT 'order id',
  `user_id` bigint NOT NULL COMMENT 'user id',
  `product_id` bigint NOT NULL COMMENT 'product id',
  `order_no` varchar(32) NOT NULL COMMENT 'order no',
  `amount` decimal(10,2) NOT NULL COMMENT 'amount',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0 unpaid 1 paid 2 canceled 3 finished',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
  `pay_time` datetime DEFAULT NULL COMMENT 'pay time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_user_product` (`user_id`, `product_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='order table';

INSERT INTO `user` (`username`, `password`, `phone`, `email`)
VALUES
('admin', '$2b$12$cbSFdqXw7Lar9k0zfHnhueAZHNWkY00uyYbSFjtpmhNTCiSUYcz7i', '13800000000', 'admin@example.com'),
('alice', '$2b$12$cbSFdqXw7Lar9k0zfHnhueAZHNWkY00uyYbSFjtpmhNTCiSUYcz7i', '13900000000', 'alice@example.com');

INSERT INTO `product` (`id`, `name`, `original_price`, `seckill_price`, `image_url`, `description`, `start_time`, `end_time`)
VALUES
(1, CONVERT(0xE69CBAE6A2B0E994AEE79B98 USING utf8mb4), 399.00, 249.00, 'https://images.unsplash.com/photo-1511467687858-23d96c32e4ae?auto=format&fit=crop&w=900&q=80', CONVERT(0xE694AFE68C81E783ADE68F92E68B94E4B88E2052474220E781AFE69588E79A84E69CBAE6A2B0E994AEE79B98E38082 USING utf8mb4), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY)),
(2, CONVERT(0xE697A0E7BABFE880B3E69CBA USING utf8mb4), 699.00, 399.00, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=900&q=80', CONVERT(0xE4BD8EE5BBB6E8BF9FE8939DE78999E880B3E69CBAEFBC8CE98082E59088E9809AE58BA4E5928CE6B8B8E6888FE38082 USING utf8mb4), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY)),
(3, CONVERT(0xE698BEE7A4BAE599A8E694AFE69EB6 USING utf8mb4), 199.00, 99.00, 'https://images.unsplash.com/photo-1527443154391-507e9dc6c5cc?auto=format&fit=crop&w=900&q=80', CONVERT(0xE98082E59088E6A18CE99DA2E694B9E980A0E79A84E58F8CE88782E698BEE7A4BAE599A8E694AFE69EB6E38082 USING utf8mb4), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY));

INSERT INTO `inventory` (`product_id`, `total_stock`, `available_stock`, `frozen_stock`, `version`)
VALUES
(1, 50, 50, 0, 0),
(2, 80, 80, 0, 0),
(3, 100, 100, 0, 0);
