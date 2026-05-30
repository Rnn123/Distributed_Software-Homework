# 第六次作业：Nacos、Gateway 与流量治理

## 1. 作业完成情况

本次作业在原秒杀系统基础上完成了服务注册发现、配置管理、服务网关、动态路由、动态配置刷新和流量治理能力建设。

已完成内容如下：

- 搭建 Nacos 环境，用于服务注册发现和配置管理。
- 秒杀业务服务 `seckill-service` 已接入 Nacos Discovery，并注册两个业务实例。
- 新增独立 Spring Cloud Gateway 服务 `seckill-gateway`，通过 Nacos 发现后端服务。
- Gateway 统一暴露 `http://localhost:9000`，所有 `/api/**` 请求由网关转发到业务服务。
- 业务服务接入 Nacos Config，支持运行期动态刷新属性。
- Gateway 对 `/api/seckill/**` 配置了基于 Redis 令牌桶的限流。
- Gateway 配置了熔断降级 fallback。
- 业务服务接入 Sentinel，实现限流、熔断和降级测试接口。
- 使用 JMeter 对动态路由、限流、熔断和降级效果进行了压力测试验证。

## 2. 环境启动

在 `SecKilling/` 目录执行：

```bash
docker compose up --build -d
```

启动后的服务地址如下：

| 服务 | 地址 |
| --- | --- |
| Nacos 控制台 | `http://localhost:8848/nacos` |
| Spring Cloud Gateway | `http://localhost:9000` |
| 业务实例 1 | `http://localhost:8081` |
| 业务实例 2 | `http://localhost:8082` |
| 原 Nginx 入口 | `http://localhost` |

Nacos 控制台账号密码为 `nacos/nacos`。

## 3. 服务注册发现结果

Nacos 控制台“服务管理 / 服务列表”中已注册以下服务：

| 服务名 | 实例数 | 说明 |
| --- | ---: | --- |
| `seckill-service` | 2 | 秒杀业务服务，分别对应 `seckill-app-1` 和 `seckill-app-2` |
| `seckill-gateway` | 1 | Spring Cloud Gateway 网关服务 |

通过 Gateway 调用业务接口成功：

```bash
curl http://localhost:9000/api/products
curl http://localhost:9000/api/config/runtime
```

`/api/**` 请求由 Gateway 根据 `lb://seckill-service` 从 Nacos 获取可用实例，并完成负载均衡转发。

## 4. 动态服务路由测试结果

动态服务路由测试步骤如下：

```bash
curl http://localhost:9000/api/config/runtime
docker stop seckill-app-1
curl http://localhost:9000/api/products
curl http://localhost:9000/api/config/runtime
docker start seckill-app-1
```

测试结果：

- 停止 `seckill-app-1` 后，Nacos 中 `seckill-service` 实例数从 2 变为 1。
- Gateway 继续通过 `http://localhost:9000/api/products` 成功访问业务服务。
- 请求由剩余实例 `seckill-app-2` 处理，服务没有中断。
- 恢复 `seckill-app-1` 后，Nacos 中 `seckill-service` 实例数恢复为 2。
- Gateway 自动重新纳入恢复后的实例，动态服务路由测试通过。

## 5. Nacos 配置动态刷新结果

项目提供了 Nacos 配置文件样例：

- `nacos/config/seckill-service.yml`
- `nacos/config/seckill-gateway.yml`

在 Nacos 控制台新增配置：

| 配置项 | 值 |
| --- | --- |
| Data ID | `seckill-service.yml` |
| Group | `DEFAULT_GROUP` |
| 格式 | `YAML` |

配置内容如下：

```yaml
app:
  dynamic:
    banner: nacos-config-v1
    seckill-enabled: true
  traffic:
    ping-qps: 2
    slow-request-ms: 300
    slow-ratio-threshold: 0.5
    min-request-amount: 5
    stat-interval-ms: 10000
    degrade-time-window-seconds: 10
```

通过 Gateway 查询运行期配置：

```bash
curl http://localhost:9000/api/config/runtime
```

随后在 Nacos 中将动态配置修改为：

```yaml
app:
  dynamic:
    banner: nacos-config-v2
    seckill-enabled: false
```

再次访问：

```bash
curl http://localhost:9000/api/config/runtime
curl http://localhost:9000/api/traffic/ping
```

测试结果：

- `banner` 从 `nacos-config-v1` 更新为 `nacos-config-v2`。
- `seckillEnabled` 从 `true` 更新为 `false`。
- 业务服务未重启，属性完成动态刷新。
- Nacos 配置管理和动态属性刷新测试通过。

## 6. 流量治理测试结果

### 6.1 Gateway 限流

Gateway 对 `/api/seckill/**` 配置 Redis 令牌桶限流：

```yaml
redis-rate-limiter.replenishRate: 20
redis-rate-limiter.burstCapacity: 40
redis-rate-limiter.requestedTokens: 1
```

压测接口：

```bash
curl -X POST http://localhost:9000/api/seckill/1
```

JMeter 高并发压测结果：

- 正常流量被 Gateway 转发到 `seckill-service`。
- 并发超过令牌桶阈值后，Gateway 返回 HTTP `429 Too Many Requests`。
- `/api/seckill/**` 网关限流生效。

### 6.2 Gateway 熔断降级

熔断降级测试命令：

```bash
docker stop seckill-app-1 seckill-app-2
curl http://localhost:9000/api/products
docker start seckill-app-1 seckill-app-2
```

业务实例全部停止后，Gateway 返回 fallback JSON：

```json
{"code":503001,"msg":"gateway fallback: seckill service is unavailable","data":"please retry later"}
```

测试结果：

- 后端服务不可用时，Gateway 没有直接暴露调用异常。
- 请求进入统一 fallback 响应。
- Gateway 熔断降级测试通过。

### 6.3 业务服务 Sentinel 限流

`/api/traffic/ping` 使用 Sentinel QPS 规则，规则由 `app.traffic.ping-qps` 控制。

压测接口：

```bash
curl http://localhost:9000/api/traffic/ping
```

JMeter 高并发压测后，业务服务返回限流响应：

```json
{"code":500103,"msg":"request is rate limited","data":null}
```

测试结果：

- Sentinel 对 `traffic-ping` 资源完成 QPS 限流。
- 修改 Nacos 中 `app.traffic.ping-qps` 后，限流阈值同步更新。
- 业务服务 Sentinel 限流测试通过。

### 6.4 业务服务 Sentinel 熔断降级

`/api/traffic/slow?ms=500` 用于模拟慢请求。系统配置慢调用阈值为 `300ms`，统计窗口内慢调用比例达到阈值后打开熔断器。

压测接口：

```bash
curl "http://localhost:9000/api/traffic/slow?ms=500"
```

连续高频请求后，业务服务返回熔断响应：

```json
{"code":500104,"msg":"service circuit breaker is open","data":null}
```

测试结果：

- Sentinel 对 `traffic-slow` 资源完成慢调用比例熔断。
- 熔断打开后，请求快速失败并返回统一业务响应。
- 熔断窗口结束后，服务进入恢复探测流程。
- 业务服务 Sentinel 熔断降级测试通过。

## 7. JMeter 压测结果

JMeter 线程组配置如下：

| 场景 | URL | 线程数 | Ramp-Up | 循环 |
| --- | --- | ---: | ---: | ---: |
| 动态路由 | `GET http://localhost:9000/api/products` | 50 | 5s | 20 |
| Sentinel 限流 | `GET http://localhost:9000/api/traffic/ping` | 100 | 5s | 30 |
| Sentinel 熔断 | `GET http://localhost:9000/api/traffic/slow?ms=500` | 50 | 5s | 30 |
| Gateway 限流 | `POST http://localhost:9000/api/seckill/1` | 100 | 5s | 30 |

压测结论：

- 动态路由场景中，停止一个业务实例后，Gateway 仍可正常访问业务接口。
- Gateway 限流场景中，超过阈值的请求返回 HTTP `429`。
- Sentinel 限流场景中，超过 QPS 阈值的请求返回业务码 `500103`。
- Sentinel 熔断场景中，慢调用比例达到阈值后返回业务码 `500104`。
- Nacos 动态配置修改后，业务接口返回值实时变化，服务无需重启。

## 8. 关键代码位置

| 功能 | 文件 |
| --- | --- |
| 主服务 Nacos/Sentinel 依赖 | `pom.xml` |
| 主服务 Nacos/Sentinel 配置 | `src/main/resources/application.yml` |
| 服务发现启动配置 | `src/main/java/com/seckill/SecKillApplication.java` |
| 动态配置属性 | `src/main/java/com/seckill/config/DynamicConfigProperties.java` |
| 流量治理属性 | `src/main/java/com/seckill/config/TrafficRuleProperties.java` |
| Sentinel 规则加载 | `src/main/java/com/seckill/config/SentinelTrafficRuleConfig.java` |
| 动态配置接口 | `src/main/java/com/seckill/controller/RuntimeConfigController.java` |
| 流量治理接口 | `src/main/java/com/seckill/controller/TrafficGovernanceController.java` |
| Gateway 工程 | `seckill-gateway/` |
| Nacos 配置样例 | `nacos/config/` |
| Docker Compose 编排 | `docker-compose.yml` |

## 9. 总结

本次作业完成了 Nacos 注册发现、Nacos 配置管理、Spring Cloud Gateway 服务网关、动态服务路由、动态属性刷新、限流、熔断、降级和 JMeter 压测验证。系统通过 Gateway 统一入口访问后端秒杀服务，服务实例变化能够被 Nacos 感知并被 Gateway 动态路由，业务配置能够通过 Nacos 动态刷新，流量治理规则能够在高并发场景下生效。
