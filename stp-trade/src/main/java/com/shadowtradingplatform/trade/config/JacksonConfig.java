package com.shadowtradingplatform.trade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * 全局 Jackson 序列化配置.
 *
 * <h3>命名策略</h3>
 * <ul>
 *   <li>全局默认: {@link PropertyNamingStrategy#SNAKE_CASE} -
 *       前端使用 snake_case 传参 (如 out_trade_no)，
 *       Jackson 自动映射到 Java 驼峰字段 (如 outTradeNo)。</li>
 *   <li>支付 POJO: 使用 {@code @JsonNaming(SnakeCaseStrategy.class)} -
 *       序列化到支付宝/微信 API 时保持 snake_case (如 out_trade_no)。</li>
 *   <li>响应 VO: 使用 {@code @JsonNaming(LowerCamelCaseStrategy.class)} -
 *       返回前端时恢复驼峰命名 (如 outTradeNo)，不影响其他业务模块。</li>
 * </ul>
 *
 * <h3>设计意图</h3>
 * <p>统一前端参数风格为 snake_case（与支付宝/微信 API 对齐），
 * 后端 Java 代码保持驼峰风格，通过 Jackson 自动转换，无需手动映射。</p>
 */
@Configuration
public class JacksonConfig {

    /**
     * 全局 ObjectMapper - 默认 SnakeCaseStrategy.
     *
     * <p>影响范围: 所有未显式指定 {@code @JsonNaming} 的类。</p>
     * <ul>
     *   <li>反序列化 (前端→后端): snake_case JSON → camelCase Java 字段</li>
     *   <li>序列化 (后端→前端): camelCase Java 字段 → snake_case JSON (需 VO 覆盖)</li>
     * </ul>
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // 全局默认使用 snake_case 命名策略
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return objectMapper;
    }
}
