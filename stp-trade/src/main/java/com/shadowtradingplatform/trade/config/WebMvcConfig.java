package com.shadowtradingplatform.trade.config;

import com.shadowtradingplatform.trade.interceptor.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 扩展配置.
 *
 * <p>职责：</p>
 * <ul>
 *   <li>注册 {@link AuthInterceptor} 全局认证拦截器</li>
 *   <li>配置登录 / 支付回调 / 接口文档 / 静态资源等白名单路径</li>
 * </ul>
 *
 * <p>CORS 跨域配置见 {@link WebCorsConfig}（使用 CorsFilter，优先级高于拦截器，
 * 避免 OPTIONS 预检被拦截器前置处理）。</p>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                // 白名单：无需认证也可以访问的路径
                .excludePathPatterns(
                        // 1. 登录接口（匿名可访问）
                        "/user/login",

                        // 2. 支付回调（第三方服务器请求，无前端 token）
                        "/alipay/notify",
                        "/wxpay/notify",

                        // 3. Knife4j / Swagger / OpenAPI3 接口文档资源
                        "/doc.html",
                        "/doc.html/**",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/favicon.ico",

                        // 4. 静态资源（若后续接入前端打包产物）
                        "/static/**",
                        "/public/**",
                        "/assets/**",
                        "/error"
                );
    }
}
