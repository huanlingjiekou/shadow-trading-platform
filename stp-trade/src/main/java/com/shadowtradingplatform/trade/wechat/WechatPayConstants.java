package com.shadowtradingplatform.trade.wechat;

/**
 * 微信支付 APIv3 常量定义.
 */
public final class WechatPayConstants {

    private WechatPayConstants() {
    }

    // ==================== APIv3 地址 ====================
    public static final String API_BASE_URL = "https://api.mch.weixin.qq.com";
    /** JSAPI 统一下单 */
    public static final String JSAPI_PREPAY_PATH = "/v3/pay/transactions/jsapi";
    /** Native 统一下单 */
    public static final String NATIVE_PREPAY_PATH = "/v3/pay/transactions/native";

    // ==================== 请求头 ====================
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_USER_AGENT = "User-Agent";

    // ==================== 回调请求头 ====================
    public static final String HEADER_WECHATPAY_TIMESTAMP = "Wechatpay-Timestamp";
    public static final String HEADER_WECHATPAY_NONCE = "Wechatpay-Nonce";
    public static final String HEADER_WECHATPAY_SERIAL = "Wechatpay-Serial";
    public static final String HEADER_WECHATPAY_SIGNATURE = "Wechatpay-Signature";

    // ==================== 签名算法 ====================
    /** SHA256withRSA 签名算法 */
    public static final String SIGN_ALGORITHM = "SHA256withRSA";
    /** 签名类型标识 */
    public static final String SIGN_TYPE = "WECHATPAY2-SHA256-RSA2048";

    // ==================== 加密算法 ====================
    /** AES-256-GCM 加密算法 */
    public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    /** GCM 认证标签长度 (位) */
    public static final int GCM_TAG_LENGTH_BITS = 128;
    /** GCM Nonce 长度 (字节) */
    public static final int GCM_NONCE_LENGTH = 12;
    /** RSA 密钥长度 */
    public static final int RSA_KEY_SIZE = 2048;

    // ==================== HTTP ====================
    public static final String ACCEPT_JSON = "application/json";
    public static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";
    public static final String USER_AGENT = "stp-trade/1.0.0";

    // ==================== 回调交易状态 ====================
    public static final String TRADE_STATE_SUCCESS = "SUCCESS";
    public static final String TRADE_STATE_NOTPAY = "NOTPAY";
    public static final String TRADE_STATE_CLOSED = "CLOSED";
    public static final String TRADE_STATE_REFUND = "REFUND";
    public static final String TRADE_STATE_PAYERROR = "PAYERROR";
}
