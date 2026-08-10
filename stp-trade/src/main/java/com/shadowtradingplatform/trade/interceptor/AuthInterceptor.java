package com.shadowtradingplatform.trade.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.constant.AuthConstants;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.vo.UserInfoVO;
import com.shadowtradingplatform.trade.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 全局认证拦截器.
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>从请求头 {@code Authorization: Bearer <token>} 提取 UUID token</li>
 *   <li>用 token 去 Redis 查询 {@link UserInfoVO}，命中则写入 {@link UserContext}</li>
 *   <li>放行（本拦截器不做强制登录拦截；各业务接口按需要自行判断 {@code UserContext.isLogin()}）</li>
 *   <li>请求完成（afterCompletion）清理 {@link UserContext}，避免线程复用串号</li>
 * </ol>
 *
 * <h3>白名单说明</h3>
 * <p>白名单路径在 {@link com.shadowtradingplatform.trade.config.WebMvcConfig} 中配置，
 * 典型包括登录接口、支付回调、静态资源、Knife4j/Swagger、OPTIONS 预检请求等。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 提取 token（兼容 Authorization: Bearer xxx 与纯 token 两种写法）
        String token = extractToken(request);
        if (StringUtils.hasText(token)) {
            UserInfoVO userInfo = userService.getUserInfoByToken(token);
            if (userInfo != null) {
                UserContext.set(userInfo);
                log.debug("拦截器认证通过：userId={}, username={}, uri={}",
                        userInfo.getId(), userInfo.getUsername(), request.getRequestURI());
            } else {
                log.debug("拦截器：token 无效或已过期，uri={}", request.getRequestURI());
            }
        }

        // 不做强制拦截：业务接口自行判断 UserContext.isLogin()
        // 如需强制登录，可在此处写回 JSON 并 return false
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        // 请求完成（含异常路径）一律清理 ThreadLocal，防止线程池复用导致数据串号
        UserContext.clear();
    }

    /**
     * 从请求头 / 参数中提取 token（不含 Bearer 前缀）.
     *
     * <p>查找顺序：Authorization 头 (Bearer) > 请求参数 token</p>
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.HEADER_AUTHORIZATION);
        if (StringUtils.hasText(header)) {
            header = header.trim();
            if (header.startsWith(AuthConstants.TOKEN_PREFIX)) {
                return header.substring(AuthConstants.TOKEN_PREFIX.length()).trim();
            }
            return header;
        }
        // 兼容 query 参数方式（部分场景下使用）
        String param = request.getParameter("token");
        if (StringUtils.hasText(param)) {
            return param.trim();
        }
        return null;
    }

    /**
     * 向响应写回未登录 JSON（保留给后续强制登录场景使用）.
     */
    @SuppressWarnings("unused")
    private void writeNotLogin(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail("未登录或登录已过期")));
        response.getWriter().flush();
    }
}
