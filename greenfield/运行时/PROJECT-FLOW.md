# PROJECT-FLOW — 同城预约服务平台（里程碑：核心交易闭环 + 基建）

> 机器可读块 ` ```machine ` 供 `运行时/gate.py` 解析。节点测试命令均为真实可跑命令。
> 后端为单 Maven 模块 `citybooking/server`（按域分包，微服务就绪），开发态 H2 自举，保证 gate 可绿。代码已搬出 `greenfield/`（文档专用），全部程序位于 `citybooking/`。

## 节点总览

| 节点 | 名称 | 关键交付 | 状态 |
|---|---|---|---|
| N0 | 脚手架与基建 | Spring Boot 骨架、分层、统一返回/异常、Redisson/LBS 抽象、CI 占位 | in_progress |
| N1 | 统一鉴权 | 注册/登录/JWT/RBAC/三角色 | pending |
| N2 | 领域模型 | 用户/商家/技师/类目/统一服务 表与 CRUD | pending |
| N3 | 服务发布与浏览搜索 | 发布服务、LBS 附近、分页、缓存 | pending |
| N4 | 下单与模拟支付 | 订单创建、支付 mock、幂等、回调 | pending |
| N5 | 派单与接单 | 指定+抢单、超时转派、WebSocket 通知 | pending |
| N6 | 履约/评价/阶梯退款 | 服务状态机、评价、阶梯退款 | pending |
| N7 | 平台管理后台 | 商家审核、类目管理、订单监管 | pending |
| N8 | 前端（design-taste） | React 核心流程 UI（浏览/下单/抢单/后台） | pending |
| N9 | 部署验证 | docker-compose 联调 + K8s 清单 + gate 全绿 | pending |
| N10 | 配套小程序 + gate | 微信小程序（消费者/商家/技师端）+ 微信授权登录 + 小程序自动化 gate | pending |

## 依赖与顺序

```
N0 ──► N1 ──► N2 ──► N3 ──► N4 ──► N5 ──► N6 ──► N7
                                          │
                                          └─► N8(前端并行) ──► N9
```

## 机器可读（gate.py）

```machine
{
  "nodes": [
    {"id":"N0","name":"脚手架与基建","status":"in_progress",
     "test":"mvn -q -f ../citybooking/server test -Dtest=ScaffoldTest"},
    {"id":"N1","name":"统一鉴权","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=AuthIntegrationIT"},
    {"id":"N2","name":"领域模型","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=MerchantIntegrationIT"},
    {"id":"N3","name":"服务浏览搜索","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=ServiceSearchIT"},
    {"id":"N4","name":"下单与模拟支付","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=OrderPaymentIT"},
    {"id":"N5","name":"派单与接单","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=DispatchIT"},
    {"id":"N6","name":"履约评价退款","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=FulfillmentIT"},
    {"id":"N7","name":"平台管理后台","status":"pending",
     "test":"mvn -q -f ../citybooking/server test -Dtest=AdminIT"},
    {"id":"N8","name":"前端","status":"pending",
     "test":"cd ../citybooking/web && npm run build"},
    {"id":"N9","name":"部署验证","status":"pending",
     "test":"mvn -q -f ../citybooking/server test",
     "build":"mvn -q -f ../citybooking/server package -DskipTests"},
    {"id":"N10","name":"小程序+自动化gate","status":"completed",
     "test":"node ../citybooking/miniprogram/scripts/gate.js"}
  ]
}
```
