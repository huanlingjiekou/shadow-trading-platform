package com.shadowtradingplatform.trade.controller;

import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.constant.AuthConstants;
import com.shadowtradingplatform.trade.context.UserContext;
import com.shadowtradingplatform.trade.domain.req.LoginRequire;
import com.shadowtradingplatform.trade.domain.vo.UserInfoVO;
import com.shadowtradingplatform.trade.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户控制器：登录 / 退出登录 / 获取当前登录信息.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "用户模块", description = "用户登录、退出登录、当前信息查询")
public class UserController {

    private final UserService userService;

    /**
     * 用户登录.
     *
     * <p>前端提交 RSA+AES 混合加密登录凭据；后端解密 -> 查库 -> 生成 UUID token -> Redis 缓存。</p>
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录（RSA+AES 加密登录）")
    public R<UserInfoVO> login(@Valid @RequestBody LoginRequire req) {
        return userService.login(req);
    }

    /**
     * 用户退出登录.
     *
     * <p>从请求头 Authorization: Bearer {token} 中读取 token，删除 Redis 中的登录态。</p>
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        return userService.logout(token);
    }

    /**
     * 获取当前登录用户信息.
     *
     * <p>若未登录（或 token 过期）返回 code=500，message=未登录。</p>
     */
    @GetMapping("/user/info")
    @Operation(summary = "获取当前登录用户信息")
    public R<UserInfoVO> info() {
        UserInfoVO user = UserContext.get();
        if (user == null) {
            return R.fail("未登录或登录已过期");
        }
        // 当前上下文中的 UserInfo 已带 token，接口返回可选择剥离 token
        UserInfoVO resp = new UserInfoVO();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setAvatar(user.getAvatar());
        resp.setPhone(user.getPhone());
        resp.setToken(user.getToken());
        return R.success(resp);
    }

    /**
     * 从请求头 Authorization: Bearer xxx 中提取 token.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.HEADER_AUTHORIZATION);
        if (!StringUtils.hasText(header)) {
            return null;
        }
        header = header.trim();
        if (header.startsWith(AuthConstants.TOKEN_PREFIX)) {
            return header.substring(AuthConstants.TOKEN_PREFIX.length()).trim();
        }
        // 兼容前端未带 Bearer 前缀的情况
        return header;
    }
}
