package com.shadowtradingplatform.trade.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求参数 DTO.
 *
 * <p>前端通过 RSA+AES 混合加密方案提交登录凭据：</p>
 * <ol>
 *   <li>随机生成 AES 密钥，使用 AES/CBC/PKCS7 加密原始 JSON 得到 {@code encryptedData}</li>
 *   <li>使用业务 RSA 公钥加密 AES 密钥得到 {@code encryptedKey}</li>
 *   <li>生成 16 字节 IV（十六进制字符串）得到 {@code iv}</li>
 * </ol>
 *
 * <p>后端解密流程：RSA 私钥解密 encryptedKey → AES key；
 * AES key + IV(hex decode) 解密 encryptedData → 原始 JSON Map。</p>
 */
@Data
@Schema(description = "登录请求参数（加密）")
public class LoginReqDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "encryptedData 不能为空")
    @Schema(description = "AES 加密后的业务数据（Base64 编码）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String encryptedData;

    @NotBlank(message = "encryptedKey 不能为空")
    @Schema(description = "RSA 公钥加密后的 AES 密钥（Base64 编码）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String encryptedKey;

    @NotBlank(message = "iv 不能为空")
    @Schema(description = "AES-CBC 初始向量（十六进制字符串，32 字符 = 16 字节）", example = "aabbccddeeff00112233445566778899", requiredMode = Schema.RequiredMode.REQUIRED)
    private String iv;
}
