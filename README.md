# STP Trade - 影子交易平台后端

## 项目概述

一个面向影子交易（Shadow Trading）场景的电商交易平台后端系统，为前端小程序提供商品、购物车、订单、支付等完整业务能力。

## 核心问题

在影子交易合规场景下，如何构建一套**高安全、高并发、高可用**的电商交易系统。

## 解决方案

- **用户认证**：UUID 令牌 + Redis 会话机制，结合 RSA+AES 混合加密登录，确保凭据安全传输
- **商品体系**：分类 / 标签 / 图片 / 富文本详情一体化数据模型，支持分类筛选与标签聚合
- **分页查询**：MyBatis-Plus 分页插件实现高性能商品列表查询
- **全局拦截**：基于 Spring MVC 拦截器 + ThreadLocal 的用户上下文传递机制

## 项目亮点

1. **RSA + AES 混合加密登录**：前端随机生成 AES 密钥，使用业务 RSA 公钥加密密钥、AES-CBC 加密数据，后端私钥解密后还原明文，全程保障凭据安全
2. **UUID + Redis 会话方案**：无状态 token 设计，Redis 集中管理登录态，支持集群部署与水平扩展
3. **ThreadLocal 用户上下文**：拦截器前置解析 token 写入上下文，业务层零侵入获取当前用户信息
4. **VO 继承体系**：列表项与详情 VO 通过继承复用字段，减少重复代码同时保持接口清晰
5. **批量查询优化**：商品标签、图片通过批量查询 + Map 映射避免 N+1 查询，保障列表页性能

## 核心技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 3.x |
| ORM | MyBatis-Plus (Lambda + 分页插件) |
| 缓存 | Redis (Redisson) |
| 加密 | RSA / AES-CBC / PKCS7 |
| 文档 | Knife4j / OpenAPI 3 |
| 数据库 | MySQL |
| JSON | Jackson |

## 快速开始

```bash
# 克隆项目
git clone <repository-url>

# 配置环境变量 (.env)
#   BUSINESS_PRIVATE_KEY: RSA PKCS8 私钥
#   DB_URL, DB_USERNAME, DB_PASSWORD
#   REDIS_HOST, REDIS_PORT

# 启动
mvn spring-boot:run
```

## 接口列表

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /user/login | 用户登录（RSA+AES 加密） |
| POST | /user/logout | 退出登录 |
| POST | /user/info | 获取当前用户信息 |
| GET | /products | 分页查询商品列表 |
| GET | /product | 查询商品详情 |
