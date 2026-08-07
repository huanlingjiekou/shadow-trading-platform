package com.shadowtradingplatform.trade.pay.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易类型枚举.
 *
 * <p>支付宝：
 * <ul>
 *   <li>WEB - 电脑网站支付</li>
 *   <li>WAP - 手机网站支付</li>
 *   <li>SCAN - 当面付扫码</li>
 *   <li>APP - APP支付</li>
 * </ul>
 * 微信支付：
 * <ul>
 *   <li>JSAPI - 公众号/小程序</li>
 *   <li>NATIVE - 扫码</li>
 *   <li>APP - APP支付</li>
 *   <li>H5 - H5支付</li>
 * </ul>
 */
@Getter
@AllArgsConstructor
public enum TradeTypeEnum {

    WEB("WEB", "电脑网站支付"),

    WAP("WAP", "手机网站支付"),

    SCAN("SCAN", "扫码支付"),

    APP("APP", "APP支付"),

    JSAPI("JSAPI", "公众号/小程序支付"),

    NATIVE("NATIVE", "Native扫码支付"),

    H5("H5", "H5支付");

    private final String code;

    private final String desc;
}
