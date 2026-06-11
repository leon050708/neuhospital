# JWT 登录态设计总结（三端版）

## 1. 这套方案解决什么问题

当前后端同时面向 3 类用户：

- 患者端
- 医生端
- 管理端

这 3 类用户都走统一认证入口，但登录成功后进入的业务界面、可访问的接口、可拥有的权限都不同。

所以这里采用的是：

- `JWT` 负责让前端随请求携带身份信息
- `Redis` 负责保存服务端登录会话状态
- `Access Token + Refresh Token` 负责兼顾安全性和用户体验

这不是“纯无状态 JWT”，而是“JWT + 服务端会话”的折中方案。这样做的好处是：

- 可以主动让用户下线
- 可以支持重新登录顶掉旧登录
- 可以做单端单登录或多端并存策略
- 后续更容易接入微信小程序静默登录

## 2. 这次已经落地的代码范围

本次已经完成的是第一版登录主流程，代码位置如下：

- 认证控制器：[AuthController.java](E:\Smart-Medical\Backend\src\main\java\com\neusoft\neu23\neuhospital\auth\controller\AuthController.java)
- 认证服务：[AuthServiceImpl.java](E:\Smart-Medical\Backend\src\main\java\com\neusoft\neu23\neuhospital\auth\service\AuthServiceImpl.java)
- JWT 生成与解析：[JwtTokenProvider.java](E:\Smart-Medical\Backend\src\main\java\com\neusoft\neu23\neuhospital\auth\security\JwtTokenProvider.java)
- Redis 会话存储：[RedisLoginSessionStore.java](E:\Smart-Medical\Backend\src\main\java\com\neusoft\neu23\neuhospital\auth\security\RedisLoginSessionStore.java)
- 配置项：[application.yml](E:\Smart-Medical\Backend\src\main\resources\application.yml)
- 测试：
  - [AuthServiceTests.java](E:\Smart-Medical\Backend\src\test\java\com\neusoft\neu23\neuhospital\auth\service\AuthServiceTests.java)
  - [JwtTokenProviderTests.java](E:\Smart-Medical\Backend\src\test\java\com\neusoft\neu23\neuhospital\auth\security\JwtTokenProviderTests.java)

当前已经支持 3 个接口：

- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

## 3. JWT 和 Session 到底是什么关系

很多人第一次做登录时会混淆这两个概念。

可以把它理解成：

- `JWT`：前端手里拿着的“电子门票”
- `Session`：后端 Redis 里保存的“门票登记记录”

也就是说：

1. 用户登录成功后，后端签发 `access_token`
2. 同时再签发 `refresh_token`
3. 后端把这次登录生成一个 `sessionId`，写入 Redis
4. 前端之后每次请求都带上 `access_token`
5. 后端先验证 JWT 是否真的是自己签发的，再去 Redis 确认这次登录会话是否还有效

所以我们虽然用了 JWT，但仍然保留了“服务端可控会话”。

## 4. Token 里放了哪些信息

### access_token

当前 access token 中包含的核心信息：

- `sub`：用户 ID
- `username`：登录名
- `role`：角色编码
- `userType`：用户类型
- `bizId`：业务主体 ID
- `sessionId`：本次登录会话 ID
- `iss`：签发方
- `exp`：过期时间

### refresh_token

当前 refresh token 中主要包含：

- `sub`：用户 ID
- `role`
- `userType`
- `bizId`
- `sessionId`
- `refreshTokenId`
- `iss`
- `exp`

这里故意把 `refreshTokenId` 单独放进去，是为了让 Redis 可以精确控制某一个刷新令牌是否失效。

## 5. 为什么要先验签，再查 Redis

后端收到 token 后，不应该第一步就直接信任它，更不应该只看 Redis 里有没有记录。

正确顺序是：

1. 先校验 JWT 签名
2. 再校验 JWT 是否过期
3. 再从 JWT 中解析出 `sessionId`
4. 最后去 Redis 查这条会话是否存在

原因很直接：

- 如果 token 连签名都不对，说明它可能是伪造的
- 如果 token 已经过期，就算 Redis 里还有数据也不能继续放行
- Redis 负责“会话是否仍然有效”
- JWT 签名和 `exp` 负责“这个票据是不是合法票据”

所以两层都要校验，缺一层都不稳。

## 6. 当前 Redis Key 设计

当前实现里用了 3 类 key：

- `login:session:{sessionId}`
- `login:refresh:{refreshTokenId}`
- `login:user:{userId}`

它们分别表示：

- `login:session:{sessionId}`：这次登录会话本身
- `login:refresh:{refreshTokenId}`：这次登录对应的刷新令牌记录
- `login:user:{userId}`：当前这个用户绑定的是哪一次登录

这样设计后，可以支持：

- 用户再次登录时顶掉旧会话
- 登出时删除整套会话
- 根据用户 ID 找到当前有效登录

## 7. 三条核心时序

### 7.1 登录

1. 前端提交用户名和密码
2. 后端校验账号状态和密码
3. 如果该用户已有旧会话，先清掉旧会话
4. 生成新的 `sessionId`
5. 生成新的 `refreshTokenId`
6. 生成 `access_token`
7. 生成 `refresh_token`
8. 把 session 和 refresh 记录写入 Redis
9. 返回 token 和用户身份信息

### 7.2 刷新 access_token

1. 前端提交 `refresh_token`
2. 后端先验签并检查是否过期
3. 从 token 里拿到 `refreshTokenId` 和 `sessionId`
4. 去 Redis 取刷新记录
5. 再去 Redis 取登录 session
6. 如果都有效，重新签发新的 `access_token`
7. 返回新的 access token

当前第一版实现里，刷新时只换 `access_token`，不轮换 `refresh_token`。

### 7.3 退出登录

1. 前端携带 `access_token` 请求退出
2. 后端先解析 access token
3. 取出 `sessionId` 和 `userId`
4. 删除 Redis 中的 session、refresh、user-binding
5. 这次登录立即失效

## 8. 三端怎么区分

当前认证链路仍然在使用 `userType + role` 这组身份信息，但数据库设计恢复为更规范的版本：

- `sys_user` 负责登录账号
- `sys_role` 负责角色定义
- `sys_user_role` 负责用户角色关系

也就是说，后续真实查库登录时，JWT 里的 `role` 将来自角色关系，而不是写死在单表字段中。

### userType

`userType` 解决的是“你属于哪一类入口”：

- `PATIENT`
- `DOCTOR`
- `MANAGEMENT`

它更偏向“前端应该进哪个端”。

### role

`role` 解决的是“你在这个端里扮演什么角色”：

- 患者端：`PATIENT`
- 医生端：`DOCTOR`、`DEPT_ADMIN`、`IMAGE_DOCTOR`
- 管理端：`REGISTRATION_CLERK`、`ADMIN`

也就是说：

- `userType` 决定进入哪个大界面
- `role` 决定当前这个账号在该界面里的角色身份
- `departmentId` 或 `bizId` 决定它归属哪个科室或业务主体

## 9. 现在这版实现的限制

这次实现是认证主链路第一版，目的是先把流程跑通，不是一次性做成最终生产版。

当前限制主要有这些：

- 账号来源还是演示版 `DemoAuthAccountProvider`
- 还没有真正接数据库版 `sys_user`
- 还没有真正接 `sys_role / sys_user_role`
- 还没有把“角色”和“科室数据范围”真正打通
- 还没有接入 Spring Security 过滤器链
- 还没有做接口级权限注解
- 还没有做刷新令牌轮换
- 还没有接微信小程序 `openid / unionid`

所以现在它是“认证内核已经具备”，但还没到“完整生产鉴权体系”。

## 10. 目前推荐的三端策略

### 患者端

- 登录方式先走账号密码或手机号验证码都可以
- token 生命周期可以相对长一点，但 refresh token 仍建议保留
- 后续优先接微信小程序静默登录

### 医生端

- 建议 access token 短一些
- 更强调主动退出、重新登录、账号状态校验
- 后续可以叠加更严格的操作审计

### 管理端

- 建议权限颗粒度最细
- 建议启用更严格的会话策略
- 后续可以支持强制下线、异地登录提醒、操作日志追踪

## 11. 一句话理解整套方案

这套方案不是“只靠 JWT 放任前端自己保管身份”，而是：

“前端拿 JWT，当作每次请求的身份证；后端用 Redis 保存这次登录是否仍然有效，并保留主动控制权。”
