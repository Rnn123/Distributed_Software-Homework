# 秒杀系统作业项目

秒杀系统 Demo。项目在单仓库内按“用户、商品、库存、订单”四类服务职责进行模块化实现，并通过 Docker Compose 组织数据库、缓存、消息队列、后端多实例和 Nginx 网关。

## 1. 技术栈

- Java 17
- Spring Boot 3.2
- MyBatis
- MySQL 8.0
- Redis 7
- Kafka 3.x
- Nginx
- Docker / Docker Compose

## 2. 目录结构

```text
SecKilling/
├── Dockerfile
├── docker-compose.yml
├── docs/
│   ├── DESIGN.md
│   └── JMETER.md
├── mysql/
│   ├── master/conf/my.cnf
│   └── slave/conf/my.cnf
├── nginx/
│   ├── conf/nginx.conf
│   └── html/
│       ├── index.html
│       └── assets/
├── sql/schema.sql
└── src/main/java/com/seckill/
```

## 3. 功能清单

- 用户注册、登录、获取当前用户
- 商品列表、商品详情缓存、商品搜索
- 库存查询
- 秒杀下单、订单查询、订单状态轮询
- Redis 缓存防穿透、防击穿、防雪崩
- Redis 预减库存 + Kafka 异步处理订单
- 同一用户同一商品只允许秒杀一次
- Snowflake 订单 ID
- MySQL 主从读写分离
- Nginx 动静分离和负载均衡

## 4. 本地启动

### 4.1 直接运行后端

1. 准备 MySQL 主从、Redis、Kafka，或使用下方 Compose 一键启动。
2. 导入 `sql/schema.sql`。
3. 在 `src/main/resources/application.yml` 中确认数据库、Redis、Kafka 地址。
4. 执行：

```bash
mvn spring-boot:run
```

### 4.2 Docker Compose 一键启动

在 `SecKilling/` 目录执行：

```bash
docker compose up --build -d
```

启动后访问：

- 前端页面：`http://localhost/`
- 后端实例 1：`http://localhost:8081`
- 后端实例 2：`http://localhost:8082`
- Nginx 统一入口：`http://localhost`
- MySQL 主库：`localhost:3306`
- MySQL 从库：`localhost:3307`
- Redis：`localhost:6379`
- Kafka：`localhost:9092`

## 5. 默认测试账号

- 手机号：`13800000000`
- 密码：`password`

## 6. 关键演示点

### 6.1 负载均衡

- 通过 `docker logs -f seckill-app-1`
- 通过 `docker logs -f seckill-app-2`
- 多次访问 `/api/products` 或进行 JMeter 压测
- 观察两边实例日志中请求数量是否大致均衡

### 6.2 动静分离

- `/index.html`、`/assets/styles.css`、`/assets/app.js` 由 Nginx 直接返回
- `/api/**` 由 Nginx 反向代理到后端集群

### 6.3 读写分离

- 写请求走主库
- 带 `@ReadOnlyDataSource` 的查询方法自动切到从库

### 6.4 秒杀链路

1. 用户登录获取 Token
2. Redis 校验重复购买并预减库存
3. Kafka 异步投递下单消息
4. 消费端写入订单并扣减数据库库存
5. 前端轮询订单状态
