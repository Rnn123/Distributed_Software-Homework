# 秒杀系统 (Seckill Demo) - 分布式部署与测试指南

这是一个基于 Spring Boot、MySQL、Redis 和 Nginx 的分布式秒杀系统演示项目。本项目实现了后端服务的多实例负载均衡、动静分离以及高可用的缓存策略。

## 1. 系统架构

*   **网关层 (Nginx)**: 负责反向代理、负载均衡（轮询策略）以及静态资源（HTML/CSS/JS）的托管。
*   **应用层 (Backend)**: 两个 Spring Boot 后端实例（`seckill-app-1`, `seckill-app-2`），运行在不同端口（内部8080，外部映射8081/8082）。
*   **缓存层 (Redis)**: 用于缓存商品详情，防止数据库压力过大。实现了缓存穿透、击穿和雪崩的解决方案。
*   **数据层 (MySQL)**: 存储用户、商品、订单等持久化数据。

## 2. 快速开始 (使用 Docker Compose)

这是推荐的运行方式，可以一键启动完整的分布式环境。

### 前置条件
*   已安装 Docker 和 Docker Compose。
*   已安装 Java 17 和 Maven (用于本地编译 jar 包，或者直接依赖 Docker 多阶段构建)。

### 启动步骤

1.  **构建并启动服务**
    在项目根目录（包含 `docker-compose.yml` 的目录）下执行终端命令：
    ```bash
    docker-compose up --build -d
    ```
    *   `--build`: 强制重新构建镜像（会执行 Maven 打包）。
    *   `-d`: 后台运行。
    *   注意：首次运行会下载 Maven 依赖和 Docker 镜像，需要一定时间。

2.  **查看服务状态**
    ```bash
    docker-compose ps
    ```
    你应该能看到 `seckill-nginx`, `seckill-app-1`, `seckill-app-2`, `seckill-redis`, `seckill-mysql` 都在运行中。

3.  **停止服务**
    ```bash
    docker-compose down
    ```

## 3. 功能验证与测试

### 3.1 访问前端页面 (动静分离)
访问本地 Nginx 入口：
*   **地址**: [http://localhost/index.html](http://localhost/index.html)
*   **说明**: 该页面是一个静态 HTML 文件，由 Nginx 直接提供服务（静态资源分离），页面加载后的商品数据通过 Ajax 请求 `/api/product/detail/{id}` 获取。

### 3.2 验证负载均衡
后端启动了两个实例。你可以查看容器日志来验证请求是否被分发到了不同的实例。

1.  打开两个终端窗口，分别监控两个后端的日志：
    ```bash
    # 终端 1
    docker logs -f seckill-app-1
    
    # 终端 2
    docker logs -f seckill-app-2
    ```
2.  多次刷新浏览器页面或使用 JMeter 发送请求。你会看到请求交替出现在两个实例的日志中（默认轮询算法）。

### 3.3 验证缓存策略 (Redis)
项目在 `ProductService.java` 中实现了以下缓存策略：

*   **缓存穿透**: 查询不存在的 ID 时，缓存空值 (`null`) 并设置较短过期时间（60s）。
*   **缓存击穿**: 使用 Redis `setIfAbsent` (NX) 实现分布式锁，防止热点 Key 失效时大量请求打到数据库。
*   **缓存雪崩**: 缓存过期时间加入随机值（3600s + 随机600s），防止大规模缓存同时失效。

**验证方法**:
1.  第一次访问商品详情页（如 ID=1），日志会显示查询数据库。
2.  再次刷新，日志不再显示数据库查询，说明走了 Redis 缓存。
3.  进入 Redis 容器查看缓存 Key：
    ```bash
    docker exec -it seckill-redis redis-cli get product:1
    ```

## 4. 压力测试 (JMeter)

使用 JMeter 进行以下场景的压测：

1.  **静态资源压测**:
    *   目标 URL: `http://localhost/index.html`
    *   目的: 测试 Nginx 处理静态文件的并发能力。
    *   预期: 响应时间极快，吞吐量高。

2.  **后端 API 压测 (动静分离后)**:
    *   目标 URL: `http://localhost/api/product/detail/1`
    *   目的: 测试后端服务集群的处理能力及 Redis 缓存效果。
    *   观察:
        *   开启缓存前后的响应时间对比（第一次慢，后续快）。
        *   两个后端实例的 CPU/内存负载是否均衡。

## 5. 目录结构说明

```
SecKilling/
├── docker-compose.yml      # 容器编排文件
├── Dockerfile              # 后端应用镜像构建文件
├── nginx/
│   ├── conf/nginx.conf     # Nginx 配置（负载均衡、反向代理）
│   └── html/index.html     # 前端静态页面
├── src/
│   └── main/java/com/seckill/service/ProductService.java # 核心缓存逻辑
└── sql/schema.sql          # 数据库初始化脚本
```
