package com.shadowtradingplatform.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.domain.User;
import com.shadowtradingplatform.trade.domain.dto.LoginReqDTO;
import com.shadowtradingplatform.trade.domain.vo.UserInfoVO;

/**
 * 用户服务接口（扩展登录 / 退出登录能力）.
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录.
     *
     * <ol>
     *   <li>使用业务私钥 + AES-CBC-PKCS7 解密登录参数得到明文 Map</li>
     *   <li>从 Map 中读取 username 并查询数据库校验用户</li>
     *   <li>生成 UUID 作为 token，将 {@link UserInfoVO} 缓存到 Redis</li>
     *   <li>返回带 token 的 {@code R<UserInfoVO>}</li>
     * </ol>
     *
     * @param req 加密后的登录请求参数
     * @return 登录结果（含 token）
     */
    R<UserInfoVO> login(LoginReqDTO req);

    /**
     * 用户退出登录.
     *
     * <p>根据当前请求上下文中的 token 删除 Redis 中缓存的用户信息.</p>
     *
     * @param token 登录令牌（UUID，不含 Bearer 前缀）
     * @return 操作结果
     */
    R<Void> logout(String token);

    /**
     * 根据 token 从 Redis 读取用户信息.
     *
     * @param token 登录令牌（UUID）
     * @return 用户信息，不存在/过期返回 {@code null}
     */
    UserInfoVO getUserInfoByToken(String token);
}
