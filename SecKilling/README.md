# WHU-分布式原理作业——秒杀系统



### 1. 秒杀下单一致性

秒杀请求流程如下：

1. 用户发起 `POST /api/seckill/{productId}`
2. Redis Lua 脚本原子完成：
   - 判断是否重复抢购
   - 判断库存是否大于 0
   - 预扣减 Redis 库存
   - 写入限购标记
3. 系统生成订单 ID，发送秒杀下单消息
4. 订单服务消费消息，先落库订单，状态置为 `WAIT_STOCK_CONFIRM`
5. 订单服务通过事务消息表 `tx_message` 发布 `order-created`
6. 库存服务消费 `order-created`，执行数据库冻结库存：
   - `available_stock - 1`
   - `frozen_stock + 1`
7. 库存服务发布 `stock-result`
8. 订单服务消费结果：
   - 成功：订单状态更新为 `UNPAID`
   - 失败：订单状态更新为 `CANCELED`，并补偿 Redis 库存、释放限购标记

### 2. 订单支付一致性

支付流程如下：

1. 用户发起 `POST /api/orders/{orderId}/pay`
2. 支付服务本地事务写入 `payment_record`
3. 同事务写入 `tx_message`，发布 `payment-success`
4. 订单服务消费 `payment-success`，将订单状态从 `UNPAID` 改为 `PAID`
5. 订单服务继续发布 `order-paid`
6. 库存服务消费 `order-paid`，将冻结库存正式确认：
   - `frozen_stock - 1`

## 一致性设计

项目采用“事务消息 + Saga 补偿”的思路：

- `tx_message` 作为本地事务消息表，保证本地业务数据和待发送消息一起提交
- `TransactionMessageRelay` 定时扫描未发送消息并投递到 Kafka
- 消费端通过订单状态、库存预留记录等方式保证幂等
- 库存扣减失败时，回补 Redis 预扣库存并删除限购标记



## 关键接口

- `POST /api/seckill/{productId}`：发起秒杀
- `GET /api/seckill/status/{orderId}`：查询秒杀处理状态
- `GET /api/orders/{orderId}`：查询订单详情
- `GET /api/orders?userId=1`：查询用户订单列表
- `POST /api/orders/{orderId}/pay`：模拟支付并触发支付一致性流程

## 订单状态说明

- `-1`：等待库存服务确认
- `0`：待支付
- `1`：已支付
- `2`：已取消
- `3`：已完成

## 启动说明

1. 准备 MySQL、Redis、Kafka
2. 导入 [sql/schema.sql](/e:/Distributed_Software/Distributed_Software-Homework/SecKilling/sql/schema.sql)
3. 检查 [application.yml](/e:/Distributed_Software/Distributed_Software-Homework/SecKilling/src/main/resources/application.yml)
4. 在 `SecKilling/` 目录执行：

```bash
mvn spring-boot:run
```

## 验证

1. 登录获取 token
2. 调用秒杀接口
3. 轮询秒杀状态直到 `SUCCESS`
4. 调用支付接口
5. 再查订单详情，确认状态变为 `1`
