# User 模块 MVP 技术文档

---

# 一、模块目标

User 模块负责：

* 用户身份存储
* 账户生命周期管理
* 角色管理
* 与 Spring Security 对接
* 提供统一用户身份主键（PKId）

---

# 二、核心设计原则

1. 用户身份唯一标识 = userPKId（数据库主键）
2. JWT subject = userPKId
3. Redis 不存储用户主数据
4. 用户状态由数据库控制
5. 权限来源数据库，不来源 Redis

---

# 三、数据模型设计

---

## 3.1 用户表（user）

```sql
CREATE TABLE user_info (
       id bigint NOT NULL AUTO_INCREMENT,
       user_id varchar(255) NOT NULL COMMENT '对外暴露的用户ID(UUID)',
       user_name varchar(64) NOT NULL COMMENT '用户名',
       avatar_url varchar(255) DEFAULT NULL COMMENT '头像地址',
       password varchar(255) NOT NULL COMMENT '加密后的密码',
       email varchar(128) NOT NULL COMMENT '邮箱',
       status enum('ACTIVE','DELETED','BANNED') NOT NULL DEFAULT 'ACTIVE',
       created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       PRIMARY KEY (id),
       UNIQUE KEY user_id (user_id),
       UNIQUE KEY email (email),
       KEY idx_user_name (user_name),
       KEY idx_email (email)
);
```

---

### 字段说明

| 字段         | 说明                |
|------------|-------------------|
| id         | 数据库主键（内部使用 PKId）  |
| user_id    | 对外暴露的用户唯一标识（UUID） |
| user_name  | 昵称 / 用户名          |
| avatar_url | 头像地址              |
| password   | BCrypt 加密密码       |
| email      | 登录邮箱（唯一）          |
| status     | 用户状态              |
| created_at | 创建时间              |
| updated_at | 更新时间              |


---

## 3.2 用户状态模型

```text
ACTIVE
DELETED
DISABLED（可扩展）
```

规则：

* DELETED 用户不可登录
* DISABLED 用户不可访问资源
* ACTIVE 才能参与聊天室

---

## 3.3 用户角色表（user_role）

```sql
CREATE TABLE role (
      id bigint NOT NULL AUTO_INCREMENT,
      role_code varchar(32) NOT NULL COMMENT '角色编码，如 ADMIN / USER',
      role_name varchar(64) NOT NULL COMMENT '角色名称',
      description varchar(255) DEFAULT NULL,
      created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY role_code (role_code)
);
```

---

### 支持角色

```text
USER
ADMIN
```

角色用于：

* 管理员封号
* 系统级操作
* 可扩展未来管理能力

---

### 字段说明

| 字段          | 说明         |
|-------------|------------|
| role_code   | 程序中使用的角色标识 |
| role_name   | 展示名称       |
| description | 描述         |

---

## 权限表（permission）

```sql
CREATE TABLE permission (
      id bigint NOT NULL AUTO_INCREMENT,
      permission_code varchar(64) NOT NULL,
      permission_name varchar(128) NOT NULL,
      description varchar(255) DEFAULT NULL,
      created_at timestamp NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY permission_code (permission_code)
);
```

---

### 用户-角色表（user_role）

```sql
CREATE TABLE user_role (
      user_id varchar(64) NOT NULL,
      role_id bigint NOT NULL,
      PRIMARY KEY (user_id, role_id),
      KEY role_id (role_id),
      CONSTRAINT user_role_ibfk_1
          FOREIGN KEY (role_id)
          REFERENCES role(id)
          ON DELETE CASCADE
);
```

### 角色-权限表（user_role）

```sql
CREATE TABLE role_permission (
     role_id bigint NOT NULL,
     permission_id bigint NOT NULL,
     PRIMARY KEY (role_id, permission_id),
     KEY permission_id (permission_id),
     FOREIGN KEY (role_id) REFERENCES role(id) ON DELETE CASCADE,
     FOREIGN KEY (permission_id) REFERENCES permission(id) ON DELETE CASCADE
);

```

---

# 四、控制器接口定义

---

## 4.1 用户注册接口

**路径**: `POST /user`

**请求体**: 
```json
{
  "userName": "用户名",
  "password": "密码",
  "email": "邮箱",
  "avatarUrl": "头像地址"
}
```

**功能**:
1. 接收用户注册信息
2. 调用 UserService 进行用户注册
3. 返回 201 Created 状态码

---

## 4.2 用户信息更新接口

**路径**: `PUT /user`

**请求体**: 
```json
{
  "userId": "用户ID",
  "userName": "新用户名",
  "avatarUrl": "新头像地址",
  "email": "新邮箱"
}
```

**功能**:
1. 更新当前认证用户的个人信息
2. 从 JWT 中获取操作用户ID
3. 返回 200 OK 状态码

---

## 4.3 用户注销接口

**路径**: `DELETE /user/{targetUserId}`

**路径参数**: `targetUserId` - 目标用户ID

**功能**:
1. 用户可以注销自己或其他用户
2. 通过 UserLifecycleService 处理注销逻辑
3. 从 JWT 中获取操作用户ID进行权限验证
4. 返回 200 OK 状态码

---

## 4.4 获取当前用户信息接口

**路径**: `GET /user/me`

**功能**:
1. 获取当前认证用户的完整信息
2. 从 JWT 中获取用户ID
3. 返回用户信息字符串表示

---

## 4.5 管理员接口

### 4.5.1 添加用户角色

**路径**: `PUT /admin/role`

**查询参数**: 
- `userId`: 用户ID
- `roleCode`: 角色编码

**功能**: 为指定用户添加角色

### 4.5.2 封禁用户

**路径**: `PUT /admin/banned/{userId}`

**路径参数**: `userId` - 要封禁的用户ID

**功能**: 管理员封禁指定用户

### 4.5.3 获取用户详细信息

**路径**: `GET /admin/user-profile/{userId}`

**路径参数**: `userId` - 目标用户ID

**功能**: 管理员获取指定用户的详细信息

---

# 五、权限模型

---

## 5.1 系统级权限

由数据库角色决定：

```
ADMIN
```

用于：

* 封禁用户
* 管理系统

---

## 5.2 房间级权限

User 模块不管理房间权限。

Room 模块根据：

```
ownerId
members
```

控制权限。

User 只提供身份。

---

# 六、身份获取模型（统一方式）

推荐统一方式：

```java
public class IdentityResolver {

    public static String currentUserPKId() {
        Authentication authentication =
            SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
```

Service 层获取当前用户。

Controller 不处理身份。

---

# 七、异常模型

| 异常                         | 场景        |
|----------------------------|-----------|
| UserNotFoundException      | 用户不存在     |
| AuthorityException         | 权限不足      |
| UserAlreadyExistsException | 用户已存在     |
| EmailOccupiedException     | 邮箱已被占用    |
| UserCanceledException      | 用户已被注销    |
| UserIdLostConnection       | 用户失去数据库连接 |

---

# 八、生命周期说明

---

## 用户创建

数据库持久化。

---

## 用户登录

生成 JWT。

---

## 用户注销

更新 status = DELETED。