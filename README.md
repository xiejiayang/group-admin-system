# 集团后台管理系统

## 技术栈

- 后端：Java 17、Spring Boot 4、MySQL、Flyway
- 前端：Vue 3、TypeScript、Element Plus
- 部署：Docker Compose、Nginx

## 默认账号

- 账号：`superadmin`
- 初始密码：`xjyadmin`

生产上线前必须替换默认超级管理员密码。`.env.example` 默认只把前端绑定到 `127.0.0.1:8080`，请先在本机完成改密，再把 `FRONTEND_BIND` 调整为公网地址或交给反向代理开放。首版暂未提供前端改密功能，请先生成新的 BCrypt 密码哈希，再在数据库中执行更新：

```bash
docker compose exec mysql sh -lc 'mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "UPDATE sys_user SET password_hash = '\''<新BCrypt哈希>'\'' WHERE username = '\''superadmin'\'';"'
```

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

2. 修改 `.env` 中的数据库密码和 `JWT_SECRET`。`JWT_SECRET=change_this_jwt_secret_before_deploy` 是不可用占位值，后端会拒绝使用它启动。生产环境建议用下面的命令生成新密钥：

   ```bash
   openssl rand -base64 48
   ```

3. 构建并启动：

   ```bash
   docker compose up -d --build
   ```

4. 访问前端：

   ```text
   http://${FRONTEND_BIND}:${FRONTEND_PORT}
   ```

Compose 会启动 MySQL 8.4、Spring Boot 后端和 Nginx 前端。后端会连接 `mysql:3306` 并执行 Flyway 迁移；上传文件目录通过 `uploads` volume 固定挂载到容器内 `/app/uploads`。Nginx 将 `/api/` 代理到容器内 `http://backend:8080/api/`，因此后端容器内端口固定为 `8080`。对外访问地址通过 `FRONTEND_BIND` 和 `FRONTEND_PORT` 调整。
