# Distributed Software Homework

WHU分布式作业_秒杀系统，代码位于 `SecKilling/`。


- 系统设计文档：服务拆分、RESTful API、ER 图、技术栈与环境说明
- 基础后端：Spring Boot + MyBatis + MySQL + Redis + Kafka
- 用户模块：注册、登录、Token 会话
- 商品/库存/订单模块：商品查询、库存查询、订单查询
- 秒杀链路：Redis 预扣库存 + Kafka 异步创建订单 + 雪花算法订单 ID
- 分布式缓存：商品详情缓存，并处理穿透、击穿、雪崩
- 容器化部署：Dockerfile、docker-compose、Nginx 负载均衡、动静分离
- 读写分离：MySQL 主从容器 + 应用层动态数据源路由
- 压测说明：JMeter 场景设计、验证口径与观测点

快速入口：

- 项目说明：`SecKilling/README.md`
- 系统设计：`SecKilling/docs/DESIGN.md`
- 压测说明：`SecKilling/docs/JMETER.md`
