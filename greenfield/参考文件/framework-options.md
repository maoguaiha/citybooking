# 框架选型（01-init / A0）

## 后端候选（Java 生态，契合 greenfield 红线）

| 框架 | 成熟度 | 微服务/云原生 | 学习/维护成本 | 结论 |
|---|---|---|---|---|
| **Spring Boot 3.3 + Spring Cloud Alibaba** | 极高 | 原生（Nacos/RocketMQ/Sentinel/Gateway 一站式） | 中 | ✅ 选定 |
| Quarkus | 高 | 原生（K8s 友好、启动快） | 中 | 备选（若追求极致启动/内存） |
| Micronaut | 高 | 原生（AOT、低内存） | 中 | 备选 |

> 选定 **Spring Boot 3.3 + Spring Cloud Alibaba**：与红线（Nacos / RocketMQ / Redisson / MyBatis-Plus / Gateway）零摩擦，社区与招人成本最优。

## 前端候选

| 框架 | 选型理由 |
|---|---|
| **React 18 + TypeScript + Vite** | ✅ 选定：组件生态、TS 类型安全、Vite 快速；配合 design-taste 规范避免模板化 UI |
| Vue 3 + TS | 备选（团队熟悉度更高时） |

## 本地可运行策略（关键决策）

- 开发态：单 Spring Boot 应用，按业务域分包（`auth` / `merchant` / `order` / `dispatch` / `payment` / `notice` / `gateway`），**微服务就绪**；使用 H2 自动建表 + 内存 Redis（或本地 Redis），无需起中间件即可编译/测试。
- 生产态：通过 `spring.profiles.active=prod` 切换 MySQL / Redis / RocketMQ / Nacos / Gateway；提供 `docker-compose.yml` 联调与 `k8s/*.yaml` 部署清单。
- 测试：JUnit5 + `@SpringBootTest` + TestRestTemplate（H2 全上下文），真实覆盖「核心交易闭环」，保证 `gate.py` 可跑绿。
