# 02-spec 输出 — 同城预约服务平台

## A1 · PRD（产品需求）

**一句话**：同城上门服务的多品类综合预约平台，连接 C 端消费者与 B 端服务提供者（商家/独立技师），支持「指定商家下单」与「发单抢单」两种撮合，含模拟支付、LBS 附近推荐、阶梯退款、三角色（C/B/Admin）统一鉴权。

### 用户故事（核心）
- 消费者：注册登录 → 按位置/类目浏览附近服务 → 查看商家/技师与可服务时间 → 指定商家下单 **或** 发单等待抢单 → 模拟支付 → 跟踪订单状态（接单/上门中/完成）→ 评价 → 需要时可取消并阶梯退款。
- 商家：入驻（提交资质，Admin 审核）→ 维护店铺信息与多技师 → 发布统一服务（价格/时长/类目/可服务时间）→ 收到指定单/抢单邀请 → 接单/派技师 → 标记上门/完成。
- 独立技师：入驻（个人资质）→ 发布服务 → 抢单/接指定单 → 履约。
- 管理员：审核商家/技师资质 → 管理类目 → 监管订单（异常仲裁/退款审批）→ 数据看板。

### 范围（本期里程碑）
含：三角色鉴权、领域模型、服务发布与 LBS 浏览搜索、下单+模拟支付、指定/抢单派单+WebSocket 通知、履约状态机+评价+阶梯退款、Admin 基础（审核/类目/监管）。
不含（后续里程碑）：真实微信/支付宝支付通道、短信真实通道、智能派单算法（距离/评分/负载综合打分）、复杂推荐、IM 聊天、发票、营销、App 推送。

## A3 · Spec 五段

### ① 业务规则
- R1 撮合两模式并存：指定商家（下单锁定 merchant_id/technician_id）；抢单（订单状态 `PENDING_GRAB`，广播给服务半径内在线候选，先抢先得，超时 `GRAB_TIMEOUT_MIN` 自动转派下一候选/流单）。
- R2 支付：下单后生成支付单，`PAY_CHANNEL=mock` 时即时返回成功并回调；真实通道预留接口。支付成功前订单不进入可接单态。
- R3 阶梯退款：未接单 100% 退；已接单未上门按 `REFUND_RATE_ACCEPTED`（默认 0.8）退；服务中/已完成不退（走 Admin 仲裁）。退款幂等（同一订单仅一次成功退款）。
- R4 评价：完成且未评价可评（评分 1–5 + 文本），评价后更新商家/技师均分。
- R5 商家/技师入驻需 `status=PENDING` 经 Admin 审核为 `APPROVED` 方可接单/发布。
- R6 角色权限（RBAC）：`ROLE_CONSUMER` / `ROLE_MERCHANT` / `ROLE_TECHNICIAN` / `ROLE_ADMIN`，接口按 `@PreAuthorize` 收敛。

### ② 数据模型（核心实体，统一服务模型）
- `user`：id, phone, password(加密), nickname, role, status。
- `merchant`：id, user_id, name, logo, address, lng, lat, radius(服务半径m), status(审核), rating。
- `technician`：id, user_id, merchant_id(可空=独立), name, skill, lng, lat, status, rating。
- `category`：id, name, parent_id(两级类目)。
- `service_item`：id, merchant_id, technician_id(可空), category_id, title, description, price, duration_min, available_start, available_end, status。
- `order`：id, order_no, consumer_id, merchant_id, technician_id(可空), service_id, mode(APPOINT|GRAB), address, lng, lat, appointment_time, amount, status, pay_status, refund_status, created_at。订单状态机：`UNPAID→PAID→PENDING_GRAB/ACCEPTED→SERVICING→COMPLETED→(REFUNDED|CLOSED)`。
- `payment`：id, order_id, channel, trade_no, amount, status, paid_at。
- `grab_record`：id, order_id, merchant_id/technician_id, status(抢中/超时)。
- `review`：id, order_id, score, comment, created_at。
- `notice`：id, receiver_id, type, payload, read, created_at（WebSocket 实时推 + 落库）。
- LBS：商家/技师经纬度写入 Redis GEO（`geo:merchant` / `geo:technician`），附近查询用 `GEOSEARCH`。

### ③ 接口契约（REST，统一 `/api/v1` 前缀；统一响应 `{code,message,data,traceId}`）
- 鉴权：`POST /auth/register` `{phone,password,nickname,role}`；`POST /auth/login` → `{token}`；`GET /auth/me`。
- 服务：`GET /services?lng=&lat=&radius=&categoryId=&keyword=&page=`；`POST /merchant/services`（商家）；`GET /services/{id}`。
- 订单：`POST /orders`（指定：带 merchantId/technicianId；抢单：不带）→ 返回 orderNo；`POST /orders/{id}/pay`（模拟）；`POST /orders/{id}/grab`（抢单）；`POST /orders/{id}/accept`（接单/派技）；`POST /orders/{id}/start`；`POST /orders/{id}/complete`；`POST /orders/{id}/cancel`；`POST /orders/{id}/review`。
- 管理：`POST /admin/merchants/{id}/audit`；`POST /admin/categories`；`GET /admin/orders?status=`；`POST /admin/refunds/{id}/approve`。
- WebSocket：`/ws?token=` 推送 order/notice 事件。

### ④ 非功能
- 并发：订单创建/抢单用 Redisson 分布式锁（`lock:order:{id}`）防超卖/重复接单；支付/退款幂等键（`idempotent:pay:{orderId}`）。
- 缓存：热门服务列表 Redis 缓存（TTL 60s，写时失效）；LBS 走 Redis GEO。
- 安全：JWT（HS256，密钥≥32B），密码 BCrypt；RBAC 注解；请求脱敏日志。
- 可观测：生产接入 Prometheus（actuator）/ SkyWalking（预留）；本地用 actuator health。
- 部署：K8s 多副本 + HPA；配置/注册走 Nacos；异步走 RocketMQ；入口 Gateway。

### ⑤ 风险与开放问题
- 风险：真实支付/短信需商户资质，本期 Mock；智能派单算法 deferred；LBS 精度依赖前端定位。
- 开放：抢单超时阈值、阶梯退款比例、服务半径默认值，均由配置中心下发（已列占位符）。
- 已决议（见 STATE）：B 端双形态、派单两模式、统一服务模型、模拟支付、阶梯退款、统一 JWT+RBAC。
