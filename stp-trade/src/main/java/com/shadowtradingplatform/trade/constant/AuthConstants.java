package com.shadowtradingplatform.trade.constant;

/**
 * 认证 / Redis 相关常量.
 */
public final class AuthConstants {

    private AuthConstants() {
    }

    /** Redis 中用户信息键的前缀，拼接 UUID token 使用 */
    public static final String REDIS_USER_KEY_PREFIX = "stp:user:token:";

    /** 用户登录令牌默认过期时间：7 天（单位：秒） */
    public static final long REDIS_USER_TTL_SECONDS = 7 * 24 * 60 * 60L;

    /** HTTP 请求头中携带 Token 的字段名 */
    public static final String HEADER_AUTHORIZATION = "Authorization";

    /** Bearer Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 登录路径（拦截器白名单使用） */
    public static final String LOGIN_PATH = "/user/login";

    /** 退出登录路径（拦截器白名单之外，但需要认证才能执行，在拦截器内通过 token 判断） */
    public static final String LOGOUT_PATH = "/user/logout";
}
