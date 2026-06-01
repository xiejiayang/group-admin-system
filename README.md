# 集团后台管理系统

## 技术栈

- 后端：Java 17、Spring Boot 4、MySQL、Flyway
- 前端：Vue 3、TypeScript、Element Plus
- 部署：Docker Compose、Nginx

## 默认账号

- 账号：`superadmin`
- 初始密码：`xjyadmin`

## 本地开发入口

- 后端：`backend/`
- 前端：`frontend/`
- 设计说明：`docs/superpowers/specs/2026-05-31-group-admin-system-design.md`
- 实施计划：`docs/superpowers/plans/2026-05-31-group-admin-system.md`

## Docker Compose 部署

1. 复制环境变量模板：

   ```bash
   cp .env.example .env
   ```

2. 修改 `.env` 中的数据库密码和 `JWT_SECRET`。生产环境建议用下面的命令生成新密钥：

   ```bash
   openssl rand -base64 48
   ```

3. 构建并启动：

   ```bash
   docker compose up -d --build
   ```

4. 访问前端：

   ```text
   http://localhost:${FRONTEND_PORT}
   ```

Compose 会启动 MySQL 8.4、Spring Boot 后端和 Nginx 前端。后端会连接 `mysql:3306` 并执行 Flyway 迁移；上传文件目录通过 `uploads` volume 挂载到 `${UPLOAD_DIR}`，默认 `/app/uploads`。Nginx 将 `/api/` 代理到容器内 `http://backend:8080/api/`，默认保持 `BACKEND_PORT=8080`。
