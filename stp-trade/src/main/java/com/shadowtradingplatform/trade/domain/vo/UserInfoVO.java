package com.shadowtradingplatform.trade.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息 VO.
 *
 * <p>用于登录成功后返回给前端，同时也是 Redis 中缓存的用户信息结构。</p>
 */
@Data
@Schema(description = "登录用户信息")
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID", example = "1")
    private Long id;

    @Schema(description = "登录用户名", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "头像URL")
    private String avatar;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "登录令牌 (UUID)，仅登录接口返回此字段", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    private String token;
}
