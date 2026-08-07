package com.shadowtradingplatform.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

/**
 * 全局 CORS 跨域配置.
 *
 * <p>为前端 HTML 测试页 / Knife4j / 外部回调预留跨域能力。</p>
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>使用 {@code CorsFilter} 而非 {@code WebMvcConfigurer#addCorsMappings}，
 *       因为 {@code CorsFilter} 优先级更高，可在 Spring Security/拦截器之前生效，
 *       避免 OPTIONS 预检请求被前置过滤器拦截导致跨域失败。</li>
 *   <li>使用 {@code allowedOriginPatterns("*")} + {@code allowCredentials(true)}，
 *       替代旧的 {@code allowedOrigins("*")}，后者与 credentials=true 组合时会被浏览器拒绝。</li>
 *   <li>显式放行微信支付回调的专用请求头（Wechatpay-*）与常用自定义头。</li>
 *   <li>maxAge 设置为 3600 秒，减少预检请求频率。</li>
 * </ul>
 */
@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 允许所有来源 (配合 allowCredentials=true 必须用 pattern 方式)
        config.setAllowedOriginPatterns(Collections.singletonList(CorsConfiguration.ALL));

        // 是否允许携带凭证 (Cookie / Authorization)
        config.setAllowCredentials(true);

        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // 允许的请求头（放行前端常用 + 微信支付回调专用）
        config.setAllowedHeaders(Arrays.asList(
                // 通用头
                "Origin",
                "Content-Type",
                "Accept",
                "Authorization",
                "X-Requested-With",
                "Cache-Control",
                "User-Agent",
                "Accept-Language",
                "Accept-Encoding",
                "Connection",
                "Referer",
                "Host",
                // Knife4j / Swagger 头
                "token",
                "X-Access-Token",
                "swagger-uuid",
                // 微信支付 Api-v3 回调专用请求头
                "Wechatpay-Timestamp",
                "Wechatpay-Nonce",
                "Wechatpay-Serial",
                "Wechatpay-Signature",
                "Wechatpay-Signature-Type"
        ));

        // 允许前端读取的响应头（暴露给 JS 读取）
        config.setExposedHeaders(Arrays.asList(
                "Content-Disposition",
                "Authorization",
                "X-Request-Id"
        ));

        // 预检请求缓存时间 (秒)，减少浏览器重复发送 OPTIONS
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
