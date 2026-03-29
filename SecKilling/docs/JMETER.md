# JMeter 压测说明

## 1. 压测目标

- 验证 Nginx 动静分离效果
- 验证两个后端实例负载是否大致均衡
- 验证商品详情缓存命中后响应时间是否下降
- 验证秒杀请求在高并发下不会超卖

## 2. 压测前准备

1. 启动全部容器：

```bash
docker compose up --build -d
```

2. 打开两个终端观察日志：

```bash
docker logs -f seckill-app-1
docker logs -f seckill-app-2
```

3. 确认页面可访问：

- `http://localhost/`
- `http://localhost/api/products`

## 3. 测试场景

### 3.1 静态资源压测

- 请求地址：`GET http://localhost/`
- 或者：`GET http://localhost/assets/styles.css`
- 建议线程数：100
- Ramp-Up：10s
- 循环次数：20

预期现象：

- 响应时间显著低于动态接口
- 后端应用日志基本不会出现静态资源请求

### 3.2 商品详情接口压测

- 请求地址：`GET http://localhost/api/products/1`
- 建议线程数：100
- Ramp-Up：10s
- 循环次数：20

预期现象：

- 首次请求较慢，后续命中 Redis 明显加快
- `seckill-app-1` 与 `seckill-app-2` 的处理次数接近

### 3.3 秒杀接口压测

先调用登录接口获取 Token，再带 Token 调用：

- 请求地址：`POST http://localhost/api/seckill/1`
- Header：`Authorization: Bearer <token>`

预期现象：

- 同一用户重复请求会被直接拦截
- 库存不会被扣成负数
- 订单数不会超过商品可用库存

## 4. 建议的 JMeter 组件

- Thread Group
- HTTP Request Defaults
- HTTP Header Manager
- JSON Extractor
- View Results Tree
- Summary Report
- Aggregate Report

## 5. 观察指标

- Average 响应时间
- 95% Line
- Throughput
- Error %
- 两个后端实例日志中的请求数量

## 6. 作业报告可写结论

- 静态文件由 Nginx 直接处理，响应时间更低
- 动态接口通过 Nginx 转发到两个应用实例，请求量大致均衡
- 商品详情接口在 Redis 缓存命中后响应时间下降明显
- 秒杀高并发下通过 Redis 预减库存、数据库原子扣减和幂等校验避免了超卖
