package com.shadowtradingplatform.trade.context;

import com.shadowtradingplatform.trade.domain.vo.UserInfoVO;

/**
 * 当前登录用户上下文.
 *
 * <p>使用 {@link ThreadLocal} 在请求线程内传递 {@link UserInfoVO}，
 * 拦截器前置写入，业务层/控制层读取，请求完成后清理。</p>
 */
public final class UserContext {

    private static final ThreadLocal<UserInfoVO> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    /**
     * 设置当前用户信息（拦截器调用）.
     */
    public static void set(UserInfoVO user) {
        HOLDER.set(user);
    }

    /**
     * 获取当前用户信息.
     *
     * @return 用户信息，若未登录则返回 {@code null}
     */
    public static UserInfoVO get() {
        return HOLDER.get();
    }

    /**
     * 获取当前用户 ID，未登录时返回 {@code null}.
     */
    public static Long getUserId() {
        UserInfoVO u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    /**
     * 获取当前用户名，未登录时返回 {@code null}.
     */
    public static String getUsername() {
        UserInfoVO u = HOLDER.get();
        return u == null ? null : u.getUsername();
    }

    /**
     * 是否已经登录.
     */
    public static boolean isLogin() {
        return HOLDER.get() != null;
    }

    /**
     * 清理当前线程上下文（必须在请求处理完成后调用，避免线程复用导致串号）.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
