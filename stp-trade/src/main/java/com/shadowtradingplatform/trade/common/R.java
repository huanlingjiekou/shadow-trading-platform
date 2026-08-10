package com.shadowtradingplatform.trade.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应结果封装类.
 *
 * <p>约定：code == 0 表示业务成功，非 0 表示业务失败。</p>
 *
 * @param <T> 业务数据类型
 */
@Data
@Schema(description = "通用响应结果")
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务成功码 */
    public static final int CODE_SUCCESS = 0;

    /** 默认业务失败码 */
    public static final int CODE_FAIL = 500;

    @Schema(description = "业务状态码，0 成功，非 0 失败", example = "0")
    private int code;

    @Schema(description = "提示信息", example = "操作成功")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 成功响应（无数据）.
     */
    public static <T> R<T> success() {
        return new R<>(CODE_SUCCESS, "操作成功", null);
    }

    /**
     * 成功响应（带数据）.
     */
    public static <T> R<T> success(T data) {
        return new R<>(CODE_SUCCESS, "操作成功", data);
    }

    /**
     * 成功响应（自定义消息 + 数据）.
     */
    public static <T> R<T> success(String message, T data) {
        return new R<>(CODE_SUCCESS, message, data);
    }

    /**
     * 失败响应（默认 500 码）.
     */
    public static <T> R<T> fail(String message) {
        return new R<>(CODE_FAIL, message, null);
    }

    /**
     * 失败响应（自定义码 + 消息）.
     */
    public static <T> R<T> fail(int code, String message) {
        return new R<>(code, message, null);
    }

    /**
     * 判断是否业务成功.
     */
    public boolean isSuccess() {
        return this.code == CODE_SUCCESS;
    }
}
