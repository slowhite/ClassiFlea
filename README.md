# ClassiFlea — 校园二手交易 Demo（Spring Boot 3.3.2, Java 21）


## 技术栈
- Spring Boot **3.3.2**（Web / Validation / Security / Mail / Thymeleaf）
- JPA (Hibernate) + **H2 文件数据库**
- 前端：**AdminLTE** 静态页（`src/main/resources/static/adminlte`）
- 表单登录（`/doLogin`），CORS 已放开（仅演示环境）

---

## 本地运行

### 依赖
- **JDK 21**
- Maven 3.9+

### 启动
```bash
# 根目录（含 pom.xml）下：
mvn spring-boot:run

# 或打包为可运行 JAR：
mvn -q package && java -jar target/ClassiFlea-java-demo-0.0.1-SNAPSHOT.jar
```

启动后：  
- 登录页：`http://localhost:8080/adminlte/login.html`  
- 市场列表：`/adminlte/market.html`  
- 详情页：`/adminlte/listing_detail.html?id={ID}`  
- 我的发布：`/adminlte/edge_mine.html`  
- 已购记录：`/adminlte/purchased.html`  
- 注册页：`/adminlte/register.html`

### 演示账号（代码中 CommandLineRunner 预置）
- `a@example.com` / `pass1234`
- `b@example.com` / `pass1234`

### H2 数据库
- JDBC：`jdbc:h2:file:./data/ClassiFlea;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MySQL`
- 账号/密码：`sa` / 空
- 控制台：`/h2`

> **重置数据库**：停止应用后删除 `./data/*.mv.db`。  
> `src/main/resources/data.sql` 含演示数据；默认未自动导入（`spring.sql.init.mode=never`）。如需导入：
>
> ```properties
> spring.sql.init.mode=always
> ```

---

## 安全与登录
- 表单登录页：`/adminlte/login.html`（提交到 **`/doLogin`**）
- 登录成功跳转：`/adminlte/market.html`  
- 放行路径：`/adminlte/**`、`/uploads/**`、`/h2/**`、注册与验证码接口等
- 其他读写接口默认需登录（见下方 API）

> **注意**：演示环境关闭了 CSRF。正式环境请开启 CSRF 并在前端携带 Token。

---

## 注册 & 邮件验证码
前端注册页：`/adminlte/register.html`  
接口：
- `POST /account/verification-code` — 向邮箱发送 6 位验证码（60 秒限频、10 分钟有效）
- `POST /account/register` — 校验验证码并创建账号（同时补建/更新 Profile）
- `POST /account/password` — 修改密码（需登录，参数：`oldPassword` / `newPassword`）

> 邮件通过 `spring-boot-starter-mail` 发送。**默认为关闭发信**（`.env.example`）。如需启用见下「邮件配置」。

---

## 列表 / 物品（Listing）
- `GET /listings` — 列出 *active* 的物品
- `GET /listings/all` — 列出全部物品
- `GET /listings/{id}` — 查看详情

我的发布（需登录，前端：`/adminlte/edge_mine.html`）：
- `GET /my/listings[?status=active|reserved|closed]`
- `POST /my/listings` — 发布（JSON 字段：`title`、`price`、`campus`、`category`、`conditionLabel`、`location`、`description`、`coverImage`、`imagesJson`）
- `PATCH /my/listings/{id}` — 更新（允许改价/标题/描述/图；`status` 允许改为 `active`/`closed`；从 `reserved` -> `active/closed` 会触发**取消订单 & 钱包退款 & 释放卖家挂起**）
- `DELETE /my/listings/{id}` — 删除（如处于 `reserved` 会先做退款与释放）

**联系卖家**（邮件转发）：  
- `POST /listings/{id}/ask`（需登录，JSON：`{ "message": "..." }`；卖家邮箱取自该 Listing；**邮件 Reply-To 为买家邮箱**）

---

## 订单（Order）
- `GET /orders` — 我创建的订单（买家视角）
- `POST /orders` — 创建订单（需登录；要求：商品 `active`、不能给自己下单；**创建后商品置为 `reserved`**，并进行钱包结算的“预扣/挂起”流程，见下）
- `POST /orders/{id}/cancel` — 取消订单（买家），商品回到 `active`，触发**退款**与**释放卖家挂起收入**
- `POST /orders/{id}/confirm` — 确认收货，触发**释放与入账给卖家**
- `DELETE /orders/{id}` — 删除订单记录（买家）

> 价格取自 Listing（以“元”为单位转换为“分”保存），下单即：买家余额扣减 + 卖家生成 **SALE_HOLD**（挂起收入）。取消 -> 买家退款 & 卖家挂起置为 CANCELLED；确认 -> 卖家挂起 RELEASED 并生成 **SALE** 入账。

---

## 钱包
- `GET /wallet` — 查询我的钱包（余额等）
- `GET /wallet/txns` — 最近流水（默认隐藏 `SALE_HOLD`；可带 `?includePending=1` 查看）
- `GET /wallet/tx` — 同上（兼容路径）
- `POST /wallet/topup` — 充值（演示路由，支持 `amountCents` 或 `amount`/`amountYuan`）
- `POST /wallet/redeem` — **兑换码**：`CLASSIFLEA100`（¥100）、`WELCOME5`（¥5）

> 钱包与订单的结算/退款/入账逻辑见 `wallet/WalletSettlementService.java`。

---

## 个人资料
- `GET /profile` — 获取我的资料
- `POST /profile` — upsert 我的资料（`nickname`、`avatarUrl`、`phone`、`bio`）

---

## 上传
- 上传接口：`POST /upload`（表单字段名：`file`；返回 JSON：`{ "url": "/adminlte/uploads/{filename}" }`）
- 访问 URL 前缀：
  - **`/adminlte/uploads/**`** → 实际目录 **`./uploads/`**（见 `WebConfig`）
  - **`/uploads/**`** → 实际目录 **`./uploads/`**（见 `StaticResourceConfig`）

> 生产建议：将 `uploads/` 指向持久化卷 / 对象存储；做类型/大小校验与安全扫描。

---

## 邮件配置（可选）
默认 `.env.example` 里 `APP_MAIL_ENABLED=false`若要开启：

1. 复制：
   ```bash
   cp .env.example .env
   ```
2. 在 `.env` 中填写：
   ```dotenv
   APP_MAIL_ENABLED=true
   MAIL_HOST=smtp.example.com
   MAIL_PORT=465
   MAIL_USER=your_account@example.com
   MAIL_PASS=app_specific_password
   MAIL_SMTP_TRUST=smtp.example.com
   MAIL_FROM=显示名<your_account@example.com>
   MAIL_DEBUG=false
   ```

> 说明：支持任意标准 SMTP；465 通常为 SSL，587 为 STARTTLS。

---

## 配置片段（`src/main/resources/application.properties`）
- H2 文件库、H2 控制台 `/h2`
- JPA：`ddl-auto=update`，`sql.init.mode=never`
- Mail：映射到 `spring.mail.*` & `spring.mail.properties.*`，通过 `.env` 注入
- 错误响应：`server.error.include-message=always` 等

> 另有 `application-devproperties`（应为 **`application-dev.properties`**）。如需 dev profile，请改名并用 `--spring.profiles.active=dev` 启用。

---

## 目录结构
```
ClassiFlea/
├─ src/main/java/com/example/ClassiFlea/
│  ├─ security/ (SecurityConfig, AuthController)
│  ├─ account/  (验证码、注册、MailService)
│  ├─ listing/  (Listing、API、联系卖家邮件)
│  ├─ wallet/   (Wallet、WalletTxn、WalletSettlementService、API)
│  ├─ profile/  (Profile、API)
│  ├─ admin/    (AdminUserController：按邮箱删除用户)
│  ├─ ...       (MeController、UploadController 等)
├─ src/main/resources/
│  ├─ static/adminlte/  (登录/注册/市场/详情/我的发布等页面及资源)
│  ├─ application.properties
│  ├─ application-devproperties
│  └─ data.sql
├─ data/        # H2 数据文件
├─ uploads/     # 运行期上传目录
└─ pom.xml
```


