# CityBooking 同城预约服务平台

一个面向同城生活服务的**预约 / 派单**平台，包含三大端：

| 端 | 技术栈 | 位置 | 说明 |
|---|---|---|---|
| **后端服务** | Spring Boot 3 + MyBatis-Plus + MySQL | `server/` | 统一 API（用户/商家/技师/订单/派单/支付 mock） |
| **管理端（运营后台）** | React + Vite + TypeScript + Tailwind | `web/` | 平台运营、商家/技师审核、数据看板 |
| **微信小程序（消费者/商家/技师）** | 微信原生小程序 | `miniprogram/` | 消费者下单、商家接单、技师接单（需微信开发者工具） |

> 当前仓库默认 **dev** 环境：本地 MySQL 8（Docker），无需 Redis，微信登录走 mock（`WX_MOCK=true`），支付走 mock 渠道。

---

## 1. 架构概览

```
                         ┌─────────────────────────────┐
   微信小程序 ──HTTPS────►│        Spring Boot 后端       │  端口 18100, 上下文 /api
   (miniprogram/)        │   server/  (dev profile)    │
                         │   - 用户/商家/技师/订单/派单   │
   浏览器 ──HTTP─────────►│   - JWT 鉴权 / 微信登录 mock  │──► MySQL 8 (Docker: mysql-cb)
   (web/ 管理端)         │   - 支付 mock / Redis(仅prod) │     citybooking_dev
                         └─────────────────────────────┘
```

- **后端 API**：`http://localhost:18100/api`
- **管理端（Vite dev）**：`http://localhost:5173`，访问路径 `/admin`
- **超管种子账号**：`10000000000` / `Admin@123456`（首次启动由 schema 初始化）

### 端口约定

| 组件 | 端口 | 说明 |
|---|---|---|
| 后端 Spring Boot | `18100` | `server.port`，上下文 `/api` |
| 前端 Vite (dev) | `5173` | `/api` 由 vite proxy 转发到 `:18100` |
| MySQL | `3306` | Docker 容器 `mysql-cb` 映射到宿主机 `3306` |

---

## 2. 环境准备（前置依赖）

| 依赖 | 版本 | 用途 | 安装 |
|---|---|---|---|
| **Docker Desktop** | 最新 | 跑 MySQL 容器 | https://www.docker.com/products/docker-desktop/ |
| **JDK** | 17+ | 后端运行 | https://adoptium.net/ |
| **Maven** | 3.8+ | 后端构建（项目内置 `mvnw.cmd` 可免装） | https://maven.apache.org/ |
| **Node.js + npm** | 18 LTS+ | 管理端前端 | https://nodejs.org/ |
| **微信开发者工具** | 最新 | 仅小程序端（消费者/商家/技师） | https://developers.weixin.qq.com/miniprogram/dev/devtools/download.html |

> 仅本地起“后端 + 管理端”不需要微信开发者工具；小程序端需单独用开发者工具打开（见 §6）。

---

## 3. 一键启动（本地开发）

仓库根目录 `citybooking/` 已提供 Windows 一键脚本（PowerShell 7+，兼容 Windows PowerShell 5.1）。

### 3.1 启动「管理端全栈」（推荐）

```powershell
# 方式 A：双击图形入口
citybooking\start-admin.cmd

# 方式 B：直接用 PowerShell
citybooking\start-admin.ps1
```

该脚本会依次：

1. **MySQL**：创建/复用 Docker 容器 `mysql-cb`（首次运行会拉取 `mysql:8` 镜像），等待就绪。
2. **后端**：`mvn clean package -DskipTests` 构建 jar，以 `dev` profile 在 `:18100` 启动。
3. **前端**：`npm install`（首次）后 `npm run dev`，在 `:5173` 启动 Vite。

启动完成后保持前台阻塞，**按 `Ctrl+C` 停止全部**（自动清理后端 + 前端进程）。

#### 常用参数

| 参数 | 行为 |
|---|---|
| （无参） | 全量：构建后端 + 启动 MySQL + 后端 + 前端 |
| `-NoBuild` | 跳过 `mvn` 构建，复用已有 jar（快速重启） |
| `-Build` | 显式强制重新构建（默认即构建，等价无参） |
| `-NoDB` | 不管理 MySQL 容器（你已用外部 MySQL 时） |
| `-NoBackend` | 仅启动前端（调试纯前端） |
| `-Test` | 自检：启动 → 等端口就绪 → 打印健康 → 自动停止退出（供 CI 验证） |

```powershell
citybooking\start-admin.ps1 -NoBuild      # 快速重启后端，不重新打包
citybooking\start-admin.ps1 -NoDB         # 用已有外部 MySQL，不启 Docker
citybooking\start-admin.ps1 -Test         # CI 自检后自动退出
```

### 3.2 仅启动「后端 + MySQL」（无前端）

```powershell
citybooking\start-dev.cmd      # 或 start-dev.ps1
```

仅拉起 MySQL + 后端（前台运行，便于看后端日志），不启动管理端前端。适合只调后端接口的场景。

### 3.3 一键停止

```powershell
citybooking\stop.cmd           # 或 stop.ps1
```

与 `start-admin.ps1` 对称，按端口清理：前端 `:5173` → 后端 `:18100/:8080` → MySQL 容器 `mysql-cb`。
不依赖启动脚本记录的 PID，**也能清理非脚本启动的残留进程**。

```powershell
citybooking\stop.ps1 -NoDB     # 不停止 MySQL 容器（保留库内数据）
```

> 注意：`start-dev.cmd` 启动的链路可用 `stop.cmd` 停止（按端口识别，通用）。

---

## 4. 手动启动（不使用脚本）

### 4.1 后端

```powershell
# 1) MySQL（Docker，首次）
docker run -d --name mysql-cb -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=citybooking_dev -p 3306:3306 mysql:8

# 2) 构建并运行
cd citybooking/server
mvn clean package -DskipTests
java -jar target/*.jar        # dev profile 默认激活，连接 localhost:3306
```

数据库表结构由 `server/src/main/resources/schema-mysql.sql` 在启动时自动初始化（`spring.sql.init.mode=always`）。

### 4.2 管理端前端

```powershell
cd citybooking/web
npm install
npm run dev                   # http://localhost:5173  → /admin
```

API 通过 vite dev proxy（`vite.config.ts`）将 `/api` 转发到 `http://localhost:18100`，无需额外 CORS 配置。

---

## 5. 配置说明

核心配置位于 `server/src/main/resources/application.yml`，通过环境变量覆盖：

| 变量 | 默认 | 说明 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` / `test` / `prod` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | 见 yml | 数据源（dev 连本地 MySQL） |
| `JWT_SECRET` / `JWT_EXPIRE_MIN` | dev 占位 | 生产务必修改 |
| `WX_APPID` / `WX_SECRET` / `WX_MOCK` | `WX_MOCK=true` | 微信登录；`WX_MOCK=true` 时 code 直接映射为 mock openid |
| `PAY_CHANNEL` | `mock` | `mock` 为模拟支付 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost:6379` | **仅 prod 生效** |

---

## 6. 微信小程序端（手动）

小程序端**未纳入一键脚本**（需微信开发者工具 GUI/CLI），需手动打开：

1. 安装并登录微信开发者工具。
2. 导入项目：`citybooking/miniprogram/`，AppID 使用测试号或自有 AppID。
3. 在小程序内通过 `utils/config.js` 配置后端基地址（默认 `http://localhost:18100/api`）。
4. 若后端 `WX_MOCK=true`，可用任意 code 直接换取 mock openid，无需真实微信资质即可联调。

> 仓库根目录的 `cb_consumer.json` / `cb_merchant.json` / `cb_tech.json` 为自动化测试用的账号/配置样例（非源码，仅供回归测试参考）。

---

## 7. 部署

部署相关编排文件位于 `citybooking/deploy/`：

| 文件 | 说明 |
|---|---|
| `docker-compose.yml` | 一键编排 MySQL + 后端 + Nginx（管理端静态资源） |
| `k8s/01-config.yaml` 等 | Kubernetes 配置（ConfigMap / Secret / Deployment / Service / Ingress） |
| `server/Dockerfile` | 后端镜像构建 |
| `web/Dockerfile` + `web/nginx.conf` | 管理端构建为静态资源，由 Nginx 托管 |

### 7.1 Docker Compose（快速部署）

```powershell
cd citybooking/deploy
docker compose up -d
```

### 7.2 Kubernetes

```powershell
kubectl apply -f citybooking/deploy/k8s/
```

> 生产（`prod` profile）需要 **Redis**（地理派单 `RedisGeo` 与 Redisson 分布式锁），请在 `deploy` 中一并部署 Redis 并注入 `REDIS_*` 环境变量。

---

## 8. 测试

```powershell
cd citybooking/server
mvn test                  # 单元 + 集成测试（test profile 用 H2 内存库，无需外部中间件）
```

`start-admin.ps1 -Test` 可做“启动自检”：拉起全栈 → 等待端口 → 打印就绪状态 → 自动停止。

---

## 9. 常见问题（Troubleshooting）

| 现象 | 原因 / 解决 |
|---|---|
| 后端启动报连不上 MySQL | Docker 未启动，或 `mysql-cb` 容器未就绪 → 运行 `start-admin.ps1` 会自动起容器并等待 |
| 前端报 404 / 接口不通 | 确认后端已起在 `:18100`，且 `vite.config.ts` 的 proxy 指向正确 |
| 端口被占用 | 运行 `stop.cmd` 清理残留；或手动 `netstat -ano \| findstr :18100` 后 `taskkill /pid <pid> /f` |
| 改了后端代码不生效 | 用 `start-admin.ps1 -NoBuild` 之外的方式需重新构建；直接无参启动会重新 `mvn package` |
| 启动后无数据 | dev 环境表结构由 `schema-mysql.sql` 自动初始化；若要重置库：`docker rm -f mysql-cb` 后重跑脚本 |
| 微信登录失败 | 后端 `WX_MOCK=true` 时无需真实配置；接真实微信需填 `WX_APPID` / `WX_SECRET` 并设 `WX_MOCK=false` |
| PowerShell 禁止脚本执行 | 以 `start-admin.cmd` / `stop.cmd` 入口运行（内部已加 `-ExecutionPolicy Bypass`） |

---

## 10. 目录结构

```
citybooking/
├── server/                 # Spring Boot 后端
│   ├── src/main/resources/application.yml
│   ├── src/main/resources/schema-mysql.sql   # 表结构初始化
│   └── Dockerfile
├── web/                    # React + Vite 管理端
│   ├── src/  vite.config.ts  nginx.conf  Dockerfile
│   └── dist/               # 生产构建产物（由 CI/构建生成）
├── miniprogram/            # 微信小程序（消费者/商家/技师）
├── deploy/                 # docker-compose + k8s + nginx
├── start-admin.ps1/.cmd    # 管理端全栈一键启动
├── start-dev.ps1/.cmd      # 仅后端+MySQL 启动
├── stop.ps1/.cmd           # 一键停止（对称）
└── README.md
```

---

## 11. 许可

内部项目，仅供学习/演示使用。
