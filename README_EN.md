# ClassiFlea — Campus Second‑hand Marketplace Demo (Spring Boot 3.3.2, Java 21)

An example application built with Spring Boot 3, JPA (Hibernate), and an H2 file database. 
The frontend uses AdminLTE static pages that call backend APIs via AJAX. 
It covers sign‑up/sign‑in (email verification code), listing publish & management, order flow, a minimal wallet settlement, image upload, and static resource mapping.

## Tech Stack
- Spring Boot **3.3.2** (Web / Validation / Security / Mail / Thymeleaf)
- JPA (Hibernate) + **H2 file database**
- Frontend: **AdminLTE** static pages (`src/main/resources/static/adminlte`)
- Form login (`/doLogin`), CORS relaxed for demo/dev

---

## Run Locally

### Requirements
- **JDK 21**
- Maven 3.9+

### Start
```bash
# At project root (where pom.xml resides):
mvn spring-boot:run

# Or build a runnable JAR:
mvn -q package && java -jar target/ClassiFlea-java-demo-0.0.1-SNAPSHOT.jar
```

After startup:
- Sign‑in page: `http://localhost:8080/adminlte/login.html`  
- Market list: `/adminlte/market.html`  
- Item detail: `/adminlte/listing_detail.html?id={ID}`  
- My listings: `/adminlte/edge_mine.html`  
- Purchases: `/adminlte/purchased.html`  
- Sign‑up page: `/adminlte/register.html`

### Demo Accounts (seeded via CommandLineRunner)
- `a@example.com` / `pass1234`
- `b@example.com` / `pass1234`

### H2 Database
- JDBC: `jdbc:h2:file:./data/ClassiFlea;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;MODE=MySQL`
- Username/Password: `sa` / empty
- Console: `/h2`

> **Reset database**: stop the app and delete `./data/*.mv.db`.  
> `src/main/resources/data.sql` contains demo data; it is **not** auto‑imported by default (`spring.sql.init.mode=never`). To import on startup:
>
> ```properties
> spring.sql.init.mode=always
> ```

---

## Security & Login
- Form login page: `/adminlte/login.html` (submits to **`/doLogin`**)
- Post‑login redirect: `/adminlte/market.html`  
- Permitted paths: `/adminlte/**`, `/uploads/**`, `/h2/**`, and the sign‑up / verification endpoints
- Other read/write APIs require authentication

> **Note**: CSRF is disabled for the demo to simplify local testing. For any deployment, enable CSRF and handle the token on the frontend.

---

## Sign‑up & Email Verification
Frontend sign‑up page: `/adminlte/register.html`  
Endpoints:
- `POST /account/verification-code` — send 6‑digit code (60s rate limit; 10‑minute TTL)
- `POST /account/register` — verify the code and create the account (also upserts Profile)
- `POST /account/password` — change password (requires login; params: `oldPassword`, `newPassword`)

> Mail is sent via `spring-boot-starter-mail`. It is **disabled by default** (see `.env.example`). To enable, see “Mail configuration” below.

---

## Listings
- `GET /listings` — list *active* items
- `GET /listings/all` — list all items
- `GET /listings/{id}` — get item detail

My listings (requires login; frontend: `/adminlte/edge_mine.html`):
- `GET /my/listings[?status=active|reserved|closed]`
- `POST /my/listings` — create (JSON fields: `title`, `price`, `campus`, `category`, `conditionLabel`, `location`, `description`, `coverImage`, `imagesJson`)
- `PATCH /my/listings/{id}` — update (allow price/title/description/images; `status` can be `active`/`closed`; switching from `reserved` → `active/closed` triggers **order cancel + wallet refund + release seller’s hold**)
- `DELETE /my/listings/{id}` — delete (if `reserved`, will refund and release before deletion)

**Contact seller** (email relay):  
- `POST /listings/{id}/ask` (login required; JSON: `{ "message": "..." }`; seller’s email is taken from the Listing; **email Reply‑To is the buyer’s email**)

---

## Orders
- `GET /orders` — orders created by me (buyer view)
- `POST /orders` — create an order (login required; item must be `active`, cannot buy your own item; **creates a `reserved` hold** and initiates the wallet “pre‑hold” flow; see below)
- `POST /orders/{id}/cancel` — cancel (buyer); item returns to `active`; triggers **refund** and **release seller’s pending income**
- `POST /orders/{id}/confirm` — confirm receipt; triggers **release and credit to seller**
- `DELETE /orders/{id}` — delete an order record (buyer)

> Price is stored as integer cents. On create: deduct buyer balance + create **SALE_HOLD** for the seller.  
> On cancel: refund buyer & mark seller hold as CANCELLED.  
> On confirm: release seller hold and create a **SALE** credit.

---

## Wallet
- `GET /wallet` — my wallet info
- `GET /wallet/txns` — recent transactions (hide `SALE_HOLD` by default; use `?includePending=1` to include)
- `GET /wallet/tx` — alias
- `POST /wallet/topup` — top up (demo route; supports `amountCents` or `amount`/`amountYuan`)
- `POST /wallet/redeem` — demo codes: `CLASSIFLEA100` (¥100), `WELCOME5` (¥5)

> Settlement/refund/credit logic is implemented in `wallet/WalletSettlementService.java`.

---

## Profile
- `GET /profile` — get my profile
- `POST /profile` — upsert my profile (`nickname`, `avatarUrl`, `phone`, `bio`)

---

## Uploads
- Endpoint: `POST /upload` (form field name: `file`; returns `{ "url": "/adminlte/uploads/{filename}" }`)
- Public URL prefixes:
  - **`/adminlte/uploads/**`** → actual dir **`./uploads/`** (see `WebConfig`)
  - **`/uploads/**`** → actual dir **`./uploads/`** (see `StaticResourceConfig`)

> For deployment, map `uploads/` to persistent storage and add file‑type/size checks and scanning as needed.

---

## Mail Configuration (optional)
By default `.env.example` sets `APP_MAIL_ENABLED=false`. To enable:

1. Copy:
   ```bash
   cp .env.example .env
   ```
2. Fill in `.env`:
   ```dotenv
   APP_MAIL_ENABLED=true
   MAIL_HOST=smtp.example.com
   MAIL_PORT=465
   MAIL_USER=your_account@example.com
   MAIL_PASS=app_specific_password
   MAIL_SMTP_TRUST=smtp.example.com
   MAIL_FROM=Display Name <your_account@example.com>
   MAIL_DEBUG=false
   ```

> Supports standard SMTP providers. 465 is typically SSL; 587 is STARTTLS.

---

## Configuration Notes (`src/main/resources/application.properties`)
- H2 file DB and console `/h2`
- JPA: `ddl-auto=update`, `sql.init.mode=never`
- Mail: mapped to `spring.mail.*` and `spring.mail.properties.*` (values injected from `.env`)
- Error response settings such as `server.error.include-message=always`

> There is also `application-devproperties` (likely a typo). If you need a dev profile, rename to **`application-dev.properties`** and enable with `--spring.profiles.active=dev`.

---

## Directory Layout
```
ClassiFlea/
├─ src/main/java/com/example/ClassiFlea/
│  ├─ security/ (SecurityConfig, AuthController)
│  ├─ account/  (verification code, sign-up, MailService)
│  ├─ listing/  (Listing, APIs, contact-seller mail)
│  ├─ wallet/   (Wallet, WalletTxn, WalletSettlementService, APIs)
│  ├─ profile/  (Profile APIs)
│  ├─ admin/    (AdminUserController: delete user by email)
│  ├─ ...       (MeController, UploadController, etc.)
├─ src/main/resources/
│  ├─ static/adminlte/  (login / register / market / detail / my listings pages & assets)
│  ├─ application.properties
│  ├─ application-devproperties
│  └─ data.sql
├─ data/        # H2 data files
├─ uploads/     # runtime upload directory
└─ pom.xml
```
