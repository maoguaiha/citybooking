# 🚦 十七维红线清单（grep 三连查坏 diff）

> 本文件为两分支共用的**查证手册**。审查阶段（04-review）按改动类型跑对应维度的 grep 三连，证据贴 MR。
> grep 锚点占位符（如 `{{真相源调用点}}`）统一登记在 [spec-template.md §占位符清单](spec-template.md#placeholders)；derive 时把占位符换成项目真实字符串，grep 才查得准。
> **技术栈无关性（占位符契约）**：本文件用 `{{注册中心}}`/`{{MQ}}`/`{{RPC客户端}}` 等占位符表达规则；项目初始化须在 [spec-template.md §项目信息卡](spec-template.md#infocard) 完成「技术栈确认」把占位符换成真实组件，**derive 据此自动生成专属执行版 redlines**（如 `{{MQ}}`→Kafka 则 ⑮ grep 自动变 `grep -rn "KafkaTemplate"`），规则无需手写改。
> Spec 必带项（告诉 AI 边界在哪）见 [spec-template.md §7.0](spec-template.md#sec70)。

---

<a id="sec71"></a>
## 7.1 通用红线（命中即打回，QA 一票否决）①②③④

- [ ] **① 命根子（原子性 / 真相源）**：扣减/库存/状态决策的原子操作仍在真相源处（如 Redis Lua / DB 事务 / 第三方幂等 API），**没被拆成两步判断**。
  → `grep -n "{{真相源调用点}}"` 确认在主链路；前置查询没被当成决策。
- [ ] **② 事务边界**：跨系统的「提交/回滚」**没有**错误地套单库 `@Transactional` 当兜底去回滚**已提交的外部操作**（真相源已提交，回滚只回单库 = 假安全）；**允许**「本地 DB 操作 + 本地消息表（同库）」用 `@Transactional` 绑定（可靠消息最终一致性方案的基础）。
  → `grep -n "@Transactional"` 看是否出现在跨系统方法上。**若 Spec 已勾选「可靠消息最终一致性（本地消息表）」改动类型（见 spec-template.md §7.0），该方法自动豁免 ②/⑬ 的 @Transactional 跨系统审查**；否则命中即打回。
- [ ] **③ 吞异常**：关键副作用（发资产/扣减/落库）**未被 try/catch 静默成 success**；异常必须冒泡或被显式处理。
  → 看这些行是否被 `try {` 包住；catch 里出现 `return success` 即打回。
- [ ] **④ 幂等**：防重复提交的机制（注解/Token/去重表）**未被「清理冗余」删掉**。
  → `grep -n "{{幂等注解/机制}}"` 确认还在。

<a id="sec72"></a>
## 7.2 通用核对项 ⑤⑥

- [ ] **⑤ 业务规则零改动**：概率/保底/计费/排序等业务规则，除非 Spec 明确涵盖，否则零改动。
  → `git diff` 比对。
- [ ] **⑥ 风格**：无格格不入写法（对照 derive 出的风格约束）；返回结构统一；无裸异常处理。
  → `grep -n "{{反模式特征，如 @Autowired / 裸 fetch / 原生 SQL}}"`。

---

<a id="sec73"></a>
## 7.3 AI 辅助开发专项审查维（⑦~⑰，高频踩坑 · grep 三连查坏 diff）

**⑦ Spring 容器红线**
- [ ] **红线**：a. 大片 `@Autowired` 字段注入 → **优先**改为构造器注入（推荐配合 Lombok `@RequiredArgsConstructor`，用 final 字段）；仅当遇到无法解开的循环依赖时，允许使用 `@Lazy` 或字段注入，但**必须加注释说明原因**；b. `new` 一个被 Spring 管理的组件（`@Service`/`@Component`/`@Repository`/`@Mapper`）→ 打回（绕过容器，事务/AOP/Redis Lua 全失效）；**纯 POJO / 工具类（无 Spring 注解、无状态）允许直接 `new`**；c. 为省事直接 `@Bean` 硬覆盖自动配置关键 bean（如 `RedisTemplate`）而不用 `@ConditionalOnMissingBean` → 打回。
  → `grep -rn " @Autowired"`（⑦a）：命中后判断是否带 `@Lazy` 或注释「循环依赖无解」——是则属豁免模式，否则按构造器注入改（模板见 代码生成模板.md CT-07）。
  → `new` 托管组件（⑦b）两步减误报：① `grep -rn "new .*Service()\|new .*Repository()\|new .*Mapper()\|new .*Component()"` 找 new 点；② 对命中类名 `XXX` 再 `grep -rn "class XXX"` 并查其是否有 `@Service`/`@Component`/`@Repository`/`@Mapper`——**无注解（纯 POJO/工具类）直接跳过**，有注解则打回（绕过容器）。
  → `grep -rn "<dependency>" pom.xml`（⑦c，确认 bean 硬覆盖是否用 `@ConditionalOnMissingBean`）。
- [ ] 注入方式 = **优先构造器注入**（final 字段，推荐 Lombok `@RequiredArgsConstructor`）；仅循环依赖等无解场景允许 `@Lazy`/字段注入并加注释；配置走占位符+多环境；依赖走 `spring-boot-starter-*`。

**⑧ 并发 / 锁边界红线**
- [ ] **红线**：a. 集群业务出现 `synchronized`/`ReentrantLock` → 打回（只锁本实例，超卖照旧）；b. 把「读库存→校验→扣减」拆成 Java 两步靠 Java 判断 → 打回（TOCTOU 竞态，对应①）；c. 给 Redis Lua 扣减套 `@Transactional` → 打回（事务只回 MySQL，回不了已提交的 Redis，对应②）。
  → `grep -rn "synchronized\|ReentrantLock"`、`grep -rn "GET\|getStock\|stock > 0"`、`grep -rn "@Transactional"`。
- [ ] 扣减的「唯一真相」放一处（Redis Lua 或 DB 行锁），别多处各自判断；复杂互斥用 Redisson RLock。

**⑨ 内存 / GC 红线**
- [ ] **红线**：a. `static Map`/缓存只加不删、没上限没过期 → 打回；b. 列表/导出「一次性查全表塞进 List」不加分页/流式 → 打回；c. 启动参数没开 `HeapDumpOnOutOfMemoryError` → 打回。
  → `static Map`（⑨a）减误报：命中后查该变量是否 `final` 且配套清理——`grep -rn "static final.*Map"` 或附近是否有 `@PostConstruct` 初始化 / `@PreDestroy` 清理 / 有上限+过期（`Caffeine`/`expireAfterWrite`）代码块；满足任一项即合法，否则打回。`grep -rn "selectList\|loadAll\|findAll\|list()"`（⑨b）、`grep -rn "new byte\[\|ByteArrayOutputStream"`（⑨c）照旧。
- [ ] 对象短寿命；大对象（>几 MB）避免常驻；DevTools 频繁 reload 会堆积类 → 提醒 AI 别滥用。

**⑩ 线程池 / 池化红线**
- [ ] **红线**：a. 出现 `new Thread(` → 打回；b. `@Async` 却没配有限队列的池 → 打回（默认无界队列）；c. 异步/跨实例逻辑里套 `synchronized` 或 `@Transactional` → 打回。
  → `grep -rn "new Thread(\|Executors\.new\|newFixedThreadPool"`、`grep -rn "@Async"`（再确认有 `ThreadPoolTaskExecutor` 配置）、`grep -rn "LinkedBlockingQueue\|RejectedExecutionHandler"`。
- [ ] 队列必须有界；拒绝策略用 `CallerRunsPolicy`（背压）；连接池（Tomcat/Hikari/Lettuce/RocketMQ）同心跳。

**⑪ Web 层红线**
- [ ] **红线**：a. 破坏统一返回——裸返对象/`Map`/`String`，或 `response.getWriter().write(...)`、混用 `new ResponseEntity` → 打回；b. controller 里**静默吞掉异常**（catch 后 `return success`、或只 `log` 不处理/不抛业务异常/不返回明确错误码）→ 打回（呼应③）；**允许**捕获后转为统一 `Result.fail(明确错误码)` 或抛出业务异常并保留日志；c. 参数格式校验手写 `if (x==null)` 不走路 `@Validated`、且 DTO 没标 `@NotNull` → 打回。
  → `grep -rn "response.getWriter\|new ResponseEntity\|return new HashMap\|return \""`、`grep -rn "try {" src/**/controller/`、`grep -rn "if (.*== null)" src/**/controller/`（命中 ⑪b 后需人工复核：是否为「静默吞异常」，合法的 `Result.fail`/抛业务异常除外）。
- [ ] 业务规则校验可留方法体；格式校验交给 `@Validated`；RESTful 动词正确（禁用 GET 做写）。

**⑫ 数据层红线**
- [ ] **红线**：a. 业务表用物理删（手写 `delete` SQL / 绕过 `@TableLogic`）→ 打回；b. `update(entity)` / `remove()` **不带 Wrapper** → 打回（全表事故）；c. `selectList` 一次性全量查回内存、且条件用字符串拼 SQL → 打回（呼应⑨）。
  → `grep -rn "DELETE FROM\|delete from\|removeById\|remove("`、`grep -rn "this.update(\|mapper.update(\|this.remove(\|mapper.delete("`、`grep -rn "selectList\|\${\|list()"`。
- [ ] 列表/导出用 `Page` 分页；条件用 `LambdaQueryWrapper`；手写 XML 的 `delete` 不认 `@TableLogic` 会真删。

**⑬ 事务 / AOP 红线**
- [ ] **红线**：a. 给「扣 Redis/MQ/HTTP 等外部操作 + 写库」跨系统方法只套单库 `@Transactional` 当兜底、试图回滚**已提交的外部操作** → 打回（呼应②）；**允许**「本地 DB 操作 + 本地消息表（同库事务）」用 `@Transactional`（可靠消息发送的基础）；b. 同类里 `this.事务方法()` 以为有事务（自调用绕过代理）→ 打回；c. 裸 `@Transactional` 不写 `rollbackFor` / `@Async` 方法上套 `@Transactional` → 打回。
  → `grep -rn "@Transactional"`、`grep -rn "this\.[a-zA-Z]*(.*)"`、`grep -rn "@Transactional\b\|@Async"`。（若 Spec 已勾选「可靠消息最终一致性」改动类型，⑬a 对「本地 DB+本地消息表同库」方法自动豁免）
- [ ] 真相源=单库（或本地 DB + 本地消息表同库）才用 `@Transactional(rollbackFor=Exception.class)`；含 Redis/MQ/HTTP 等**已提交外部操作** → 用幂等+补偿/对账（Saga），不可用单库事务去回滚它们。**注**：「本地 DB + 本地消息表」豁免仅在 Spec 勾选「可靠消息最终一致性」改动类型时自动生效（见 spec-template.md §7.0 与 代码生成模板.md CT-13），否则默认按跨系统处理。

**⑭ Redis / 异步 / 调度 / Actuator 红线**
- [ ] **红线**：a. 缓存 `set` 不带 TTL / 容量上限 → 打回；b. 分布式锁不在 `finally` 释放、或手写 SETNX 上生产 → 打回；c. `@Async` 未配自定义池 → 打回；d. 同类型多 Redis Bean 用字段 `@Autowired` 不选 → 打回；e. 生产暴露 Actuator `shutdown` → 打回。
  → `grep -rn "opsForValue()\.set(\|opsForHash()\.put("`、`grep -rn "SETNX\|setIfAbsent"`、`grep -rn "@Async"`、`grep -rn "@Autowired RedisTemplate"`。
- [ ] 缓存键带业务前缀 + TTL；跨实例互斥用 `RedissonClient.getLock()`（即 `{{分布式锁}}`，默认 Redisson）+ `finally { unlock() }`；跨服务异步走 RocketMQ（即 `{{MQ}}`，默认 RocketMQ）。

**⑮ 微服务拆分 / 跨进程红线**
- [ ] **红线**：a. 多个服务连同一个库且都写同一批表 → 打回；b. 跨服务同步长链路无熔断/降级 → 打回；c. 用单机 `@Transactional` 当跨服务一致性 → 打回；d. 跨服务用本地 `@Async` → 打回；e. 按技术层拆 → 打回。
  → `grep -rn "INSERT|UPDATE|DELETE" src/**/mapper/`、`grep -rn "RestTemplate|FeignClient|@Async"`、`grep -rn "@Transactional"`。
- [ ] 按业务域拆；跨服务同步走 `{{RPC客户端}}`（默认 OpenFeign）+ `{{熔断降级}}`（默认 Resilience4j）；一致性用 `{{MQ}}`（默认 RocketMQ）/Saga；引入新服务走绞杀者模式。

**⑯ 微服务核心组件红线**

> **技术栈插槽（默认实现为示例，切换技术栈时替换本表，并同步改下方 grep 锚点）**：`{{注册中心}}`={{Nacos}}、`{{配置中心}}`={{Nacos}}、`{{RPC客户端}}`={{OpenFeign}}、`{{熔断降级}}`={{Resilience4j}}、`{{MQ}}`={{RocketMQ}}、`{{网关}}`={{Spring Cloud Gateway}}、`{{分布式锁}}`={{Redisson}}、`{{链路追踪}}`={{Zipkin}}、`{{编排平台}}`={{K8s}}。derive 时把 `{{...}}` 换成项目真实组件，grep 才查得准。


- [ ] **红线**：a. `{{注册中心}}`/`{{配置中心}}` 单实例无集群 → 生产必须集群；b. 配了中心却仍硬编码字面量 → 打回；c. `{{RPC客户端}}`（默认 OpenFeign）调用无熔断降级 → 打回；d. `{{网关}}`（默认 Spring Cloud Gateway）无路由超时/无限流 → 打回；e. 服务循环依赖 → 打回。
  → `grep -rn "http://[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:|localhost:[0-9]+"`（硬编码地址，对应 b）、`grep -rn "{{RPC客户端注解，默认 @FeignClient}}"`（对应 c）、`grep -rn "limit-for-period|gachaDraw|[0-9]+s"`（限流，对应 d）、`grep -rn "{{网关配置特征，默认 spring.cloud.gateway}}"`（对应 d）。
- [ ] 服务地址走 `{{注册中心}}`；动态配置进 `{{配置中心}}` + `@RefreshScope`；`{{RPC客户端}}` 必带熔断降级；`{{网关}}` 配超时+限流；禁循环依赖。

**⑰ 可观测 / 容器化红线**
- [ ] **红线**：a. 没配健康检查（探真依赖 Redis/DB）→ 打回；b. 链路追踪采样率 0 / traceId 没透传 → 打回；c. 镜像单阶段 + root → 打回；d. 配置写死进镜像 → 打回；e. 日志不落 stdout / 无 JSON / traceId 没进 MDC → 打回。
  → `grep -rn "actuator/health\|readinessProbe\|livenessProbe"`、`grep -rn "^USER " Dockerfile`、`grep -rn "FROM maven\|FROM gradle" Dockerfile`、`grep -rn "password:\s*\"\|port:\s*8080"`、`grep -rn "System.out\|ConsoleAppender"`。
- [ ] 生产开 `health,info,metrics,prometheus` 禁 `shutdown`；tracing 开发 1.0/生产 0.1；多阶段 Dockerfile + `USER appuser`；配置外部化；日志 JSON + traceId 进 MDC。

---

## 7.4 运行时三验证（QA 主导，别只看代码）

- [ ] **前后对比**：操作前/后，真相源关键值只变该变的量。
- [ ] **幂等复现**：同一请求连发两次 → 第二次被拦，数据只动一次。
- [ ] **异常注入**：让关键副作用抛错 → 确认返回失败（非 success），验证没被吞。
- [ ] **并发/压测（⑦~⑮）**：10+ 线程并发扣减断言「最终库存 = 初始 − 总扣减」且不为负；压测盯 `activeThreads/queueSize/rejectCount/TP99`；接口统一返回验证：错参返 `code=400` 非 500；删除后查库确认 `del_flag=2` 行还在；跨系统（Redis/MQ 已提交）却只用单库 `@Transactional` 兜底时，造 Redis 成功+MySQL 失败，确认两者未对齐（单库事务回不了外部，未对齐即红线②）；缓存写后 `TTL key` 确认有剩余时间；并发拿锁确认仅一个实例进临界区且异常路径 `finally` 仍 unlock。
- [ ] **⑯ 微服务组件场景**：改一个阈值确认不用重启即热生效（@RefreshScope）；杀掉下游服务确认调用方走熔断 fallback 而非线程卡死；`{{网关}}`（默认 Spring Cloud Gateway）慢响应确认超时触发；制造 A→B→A 循环依赖确认启动期报错；`{{注册中心}}`（默认 Nacos）单实例宕机演练确认有集群兜底。
- [ ] **⑰ 可观测/容器化场景**：杀掉 Redis 后确认 `/actuator/health` 变 DOWN 且 `{{编排平台}}`（默认 K8s）摘出 Pod；造慢请求确认 Zipkin 能定位慢跳且 traceId 串起；`docker history` 确认最终镜像只含 JRE 且非 root；改配置只改 ConfigMap 确认不用重 build；容器日志确认 JSON 且含 traceId。
