package com.shadowtradingplatform.trade.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 智能 Long 序列化器
 * 将 Long 类型序列化为 String 类型，防止前端雪花 ID 精度丢失
 * 
 * 特点：
 * 1. 值为 null 时输出 null
 * 2. 值不为 null 时输出字符串形式
 */
public class SmartLongSerializer extends JsonSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeString(value.toString());
        }
    }
}