# STATE — 同城预约服务平台（greenfield 运行时）

> 本文件是流水线唯一状态源。机器人每轮先读它，再决定动作。
> 协议：**不确定就问**——涉及需求/范围/接口歧义，立即挂起并 @ 用户，绝不在猜测上继续。

## 项目信息卡（占位符已落地）

| 项 | 值 |
|---|---|
| 项目名称 | 同城预约服务平台（到家撮合） |
| 业务形态 | 上门服务（到家）· 多品类综合平台 |
| 用户角色 | C 端消费者 / B 端（商家[可多技师] + 独立技师）/ 平台管理员 |
| 派单机制 | 用户指定商家下单 **或** 发单后商家/技师抢单（并存）；LBS 附近/半径 |
| 服务建模 | 统一服务模型（标题/描述/价格/时长/可服务时间/类目标签） |
| 支付 | 模拟支付（预留真实微信/支付宝接口，策略可切换） |
| 退款 | 按阶段阶梯退款（未接单全退；接单/服务中按比例扣违约金） |
| 鉴权 | 统一账号 + JWT + RBAC |
| 通知 | WebSocket 站内实时推送（生产必需）+ 短信通道策略预留（Mock） |
| 后端框架 | Spring Boot 3.3（Java 21）+ MyBatis-Plus 3.5 + Spring Security |
| 前端框架 | React 18 + TypeScript + Vite |
| 数据/缓存/锁 | MySQL（生产）/ H2（开发自举）；Redis（GEO+LBS / 缓存 / Redisson 分布式锁） |
| 消息/注册/网关 | RocketMQ（异步解耦）/ Nacos（注册+配置）/ Spring Cloud Gateway（生产多实例） |
| 部署 | 多实例 K8s（docker-compose 本地联调 + K8s 清单生产） |
| 交付目标 | 生产级；本里程碑 = 核心交易闭环 + 基建 |
| 当前状态 | 编码完成（核心闭环 + 基建 + 前端 + 部署清单，各节点 gate 全绿） |
| 当前节点 | N10 配套小程序 + gate（已完成，见 PROJECT-FLOW.md） |

current_node: 10

node_status:
  0: done
  1: done
  2: done
  3: done
  4: done
  5: done
  6: done
  7: done
  8: done
  9: done
  10: done

## 占位符（运行时由环境变量/配置注入，禁止硬编码）

| 占位符 | 说明 | 默认值（开发） |
|---|---|---|
| `{{DB_URL}}` | 数据库 JDBC URL | `jdbc:h2:mem:booking;MODE=MySQL` |
| `{{DB_USER}}` | 数据库账号 | `sa` |
| `{{DB_PASSWORD}}` | 数据库密码 | `` |
| `{{REDIS_URL}}` | Redis 地址 | `redis://localhost:6379` |
| `{{REDIS_PASSWORD}}` | Redis 密码 | `` |
| `{{JWT_SECRET}}` | JWT 签名密钥（≥32 字节） | 开发随机值（生产必改） |
| `{{JWT_EXPIRE_MIN}}` | JWT 有效期（分钟） | `120` |
| `{{NACOS_ADDR}}` | Nacos 地址 | `127.0.0.1:8848` |
| `{{ROCKETMQ_ADDR}}` | RocketMQ namesrv | `127.0.0.1:9876` |
| `{{SMS_PROVIDER}}` | 短信通道（aliyun/tencent/mock） | `mock` |
| `{{SMS_KEY}}` / `{{SMS_SECRET}}` | 短信密钥 | `` |
| `{{PAY_CHANNEL}}` | 支付通道（mock/wechat/alipay） | `mock` |
| `{{WECHAT_MCH_ID}}` / `{{WECHAT_API_KEY}}` | 微信支付商户 | `` |
| `{{ALIPAY_APP_ID}}` | 支付宝应用 ID | `` |
| `{{GEO_DEFAULT_CITY}}` | 默认城市（LBS 兜底） | `北京` |
| `{{WX_APPID}}` | 微信小程序 AppID | `` |
| `{{WX_SECRET}}` | 微信小程序 Secret | `` |
| `{{WX_MOCK}}` | 微信登录 mock 模式（true 免真实微信，code→mock openid） | `true` |

## 跨节点结论（已确认，禁止回退）

1. B 端同时支持「商家（可多技师）」与「独立技师」。数据模型：`merchant` 表 + `technician` 表（独立技师 `merchant_id` 为空）。
2. 派单两模式：指定商家（下单即锁定 merchant/technician）+ 抢单（发单广播给附近候选，先抢先得，超时转派）。
3. 服务统一模型，类目仅作标签/分类，不做差异化扩展属性（后续可加 `service_attr` 扩展表）。
4. 支付走 `PaymentChannel` 策略接口；默认 `MockPaymentChannel`，预留 `WechatPaymentChannel` / `AlipayPaymentChannel` 骨架。
5. 退款阶梯：未接单 100% 退；已接单未上门按比例（如 80%）；服务中/已完成不退（需平台仲裁入口）。

## 协议（执行纪律）

- 红线：见 `参考文件/redlines.md`（`grep` 门禁）。任何 `// TODO(实现)` 注释 = 卡死，不可提交。
- 节点完成以 `python 运行时/gate.py <节点id>` 全绿为唯一判据。
- 重大需求变更 → 回到本卡更新并通知用户。

## 补充测试与缺陷修复（N9 之后，确保无回归）

> 目标：补充页面/测试用例，确保系统无问题。新增 21 个集成测试，并修复 2 个影响正确性的缺陷。

### 新增测试（server/src/test/java/com/citybooking/server）
- `SecurityIT.java`（9 例）：鉴权/ RBAC 边界——未登录 401/4xx 拦截；消费者越权访问 `/merchant/onboard`、`/orders/grab-board`、`/admin/merchants`、`/merchant/services` 一律 403；商家越权创建类目 403；技师越权审核商家 403。
- `ExtraCoverageIT.java`（12 例）：未覆盖接口——服务详情、我的服务列表、添加技师、我的订单（消费者+商家视角、状态过滤）、管理员商家/订单列表、未支付取消→CANCELLED、服务中取消被拒、重复抢单冲突、类目创建与公开列表联动。

### 缺陷修复（重要）
1. **RBAC 长期未生效**：`SecurityConfig` 缺少 `@EnableMethodSecurity`，导致所有 `@PreAuthorize` 角色校验被静默忽略（消费者可调商家/管理员接口）。已启用 `@EnableMethodSecurity`，全部角色隔离恢复。`GlobalExceptionHandler` 已将 `AccessDeniedException` 映射为 403。
2. **分页列表字段名不一致**：后端 `PageResult` 序列化字段为 `list`，而前端契约（Home/Orders 的 `records`）期望 `records`，导致搜索/我的订单列表在 UI 为空。已将 `PageResult` 列表字段重命名为 `records`，前后端对齐。

### 前端
- 新增 `pages/OrderDetail.tsx`：订单详情页（状态时间线 + 按角色/状态驱动支付/取消/评价/接单/抢单/开始/完成），接入 `/orders/:id` 路由；`Orders.tsx` 订单卡片可点击跳转并新增「查看详情」。

### 验证结果
- 后端 `mvn test`：36 例全部通过（原 14 + 新 21 + 微信登录 1），BUILD SUCCESS。
- 前端 `npm run build`：通过（tsc 无错 + vite 构建成功）。
- 全部节点 gate 仍为绿（N9 = 全量测试，已验证）。

## 配套微信小程序（含商家/技师端 + 微信授权登录，N9 之后新增，N10 纳入 gate）

> 目标：为平台提供移动端入口，复用现有后端 `/api` 接口（JWT）。首版覆盖 消费者端 与 商家/技师端，并接入微信授权登录。

### 目录
- `miniprogram/`：原生微信小程序（JS + WXML/WXSS），无构建步骤，可直接用微信开发者工具导入。
- `miniprogram/scripts/gate.js`：小程序自动化 gate（校验全部 JS 语法 + JSON 可解析）。

### 页面与功能（按登录角色自适应）
- `pages/login`：手机号+密码登录/注册（角色可选）、**微信一键登录**（`wx.login` → `/auth/wechat-login`，首次自动注册消费者）。
- `pages/home`：
  - 消费者：LBS 附近服务浏览、关键词搜索、类目筛选、重新定位。
  - 商家/技师：抢单大厅（`/orders/grab-board` 一键抢单）+ 我的接单。
- `pages/service-detail`：服务详情 + 下单（指定商家 APPOINT / 发单抢约 GRAB，含地址与预约时间）。
- `pages/orders`：我的订单，按状态分组（消费者：全部/待支付/进行中/已完成；商家/技师同接口返回自身订单）。
- `pages/order-detail`：订单详情，按角色+状态驱动：
  - 消费者：去支付 / 取消 / 评价（星级+评论）。
  - 商家/技师：接单（WAIT_ACCEPT）/ 开始服务（ACCEPTED）/ 完成服务（SERVICING）。

### 后端配套改动（微信登录）
- `User` 新增 `wx_openid` 字段，`app_user` 表新增 `wx_openid` 列。
- 新增 `WechatService`：`wx.login` 的 `code` 换 `openid`；`app.wechat.mock=true`（默认）时直接将 `code` 映射为 `mock_<code>`，免真实微信配置即可开发与测试。
- `AuthService.wechatLogin(code)`：按 `openid` 查用户，不存在则自动创建 `CONSUMER`；返回 JWT。新增 `POST /api/auth/wechat-login`。
- 新增 `WechatLoginIT`（1 例）：mock 模式下幂等（同 code 同用户）、不同 code 不同用户、`token` 可访问 `/auth/me`。

### 接入说明
- `utils/config.js` 的 `BASE_URL` 默认指向 `http://localhost:8080/api`；生产需改为 HTTPS 域名并在小程序后台「开发管理 → 服务器域名」配置 request 合法域名。
- `project.config.json` 已设 `urlCheck:false`，开发者工具可直连本地后端（不校验域名）。
- 运行：微信开发者工具 → 导入项目 → 选择 `miniprogram/` 目录（测试 AppID 用 `touristappid`）。
- 商家/技师要使用抢单/接单履约，需先在后端的 `/merchant/onboard` 完成入驻（`MERCHANT`/`TECHNICIAN`）。

### 自检
- 小程序 gate（`node miniprogram/scripts/gate.js`/PROJ-FLOW 节点 N10）：校验 11 个 JS 语法 + 8 个 JSON，全部 PASS。
- 后端 `mvn test`：36 例全过（含 `WechatLoginIT`），BUILD SUCCESS。
- 前端 `npm run build`：保持通过。
- API 契约与 `web/src/lib/api.ts` 完全一致（含 `PageResult.records`、抢单/接单/履约接口），后端 RBAC 已生效。
