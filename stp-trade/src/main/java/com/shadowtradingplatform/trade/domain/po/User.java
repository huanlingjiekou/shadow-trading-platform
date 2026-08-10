package com.shadowtradingplatform.trade.domain.po;


import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class User {
    @Schema(description="用户ID")
    private Long id;
    @Schema(description="登录用户名")
    private String username;
    @Schema(description="密码（加盐哈希，如 BCrypt）")
    private String password;
    @Schema(description="昵称")
    private String nickname;
    @Schema(description="头像URL")
    private String avatar;
    @Schema(description="手机号")
    private String phone;
    @Schema(description="创建时间")
    private LocalDateTime create_time;
    @Schema(description="更新时间")
    private LocalDateTime update_time;
}
