package com.shadowtradingplatform.trade.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.shadowtradingplatform.trade.serializer.SmartLongSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记字段需要将 Long 类型序列化为 String 类型
 * 用于解决前端雪花 ID 精度丢失问题
 * 
 * 使用方式：
 * @LongToString
 * private Long id;
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JsonSerialize(using = SmartLongSerializer.class)
@JacksonAnnotationsInside
public @interface LongToString {
}