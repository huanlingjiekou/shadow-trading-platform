package com.shadowtradingplatform.trade.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

/**
 * 驼峰命名策略 - 保持 Java 驼峰命名不变.
 *
 * <p>用于覆盖全局 {@link PropertyNamingStrategy#SNAKE_CASE} 策略，
 * 使响应 VO 类序列化时保持 camelCase (如 outTradeNo 而非 out_trade_no)。</p>
 *
 * <p>使用方式: 在 VO 类上标注 {@code @JsonNaming(CamelCaseNamingStrategy.class)}</p>
 */
public class CamelCaseNamingStrategy extends PropertyNamingStrategy {

    @Override
    public String nameForField(MapperConfig<?> config, AnnotatedField field, String defaultName) {
        return defaultName;
    }

    @Override
    public String nameForGetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return defaultName;
    }

    @Override
    public String nameForSetterMethod(MapperConfig<?> config, AnnotatedMethod method, String defaultName) {
        return defaultName;
    }
}
