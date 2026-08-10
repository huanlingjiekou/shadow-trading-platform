package com.shadowtradingplatform.trade.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadowtradingplatform.trade.common.R;
import com.shadowtradingplatform.trade.config.BusinessProperties;
import com.shadowtradingplatform.trade.constant.AuthConstants;
import com.shadowtradingplatform.trade.domain.po.User;
import com.shadowtradingplatform.trade.domain.req.LoginRequire;
import com.shadowtradingplatform.trade.domain.vo.UserInfoVO;
import com.shadowtradingplatform.trade.mapper.UserMapper;
import com.shadowtradingplatform.trade.service.UserService;
import com.shadowtradingplatform.trade.util.LoginCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现（含登录 / 退出登录 / Redis 会话管理）.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final LoginCryptoUtil loginCryptoUtil;
    private final BusinessProperties businessProperties;
    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public R<UserInfoVO> login(LoginRequire req) {
        try {
            // 1. 解密登录载荷 -> Map
            Map<String, Object> payload = loginCryptoUtil.decryptLoginPayload(
                    req.getEncryptedData(),
                    req.getEncryptedKey(),
                    req.getIv(),
                    businessProperties.getPrivateKey()
            );

            // 2. 从明文中提取用户名 / 手机号等关键字段进行匹配
            //    约定解密后的 JSON 至少含 username 字段（如有 password 字段，按业务需要校验）
            String username = asString(payload.get("username"));
            String phone = asString(payload.get("phone"));
            String password = asString(payload.get("password"));

            if (!StringUtils.hasText(username) && !StringUtils.hasText(phone)) {
                return R.fail("登录凭据缺少 username 或 phone");
            }

            // 3. 查询数据库用户（优先按 username，其次按 phone）
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            if (StringUtils.hasText(username)) {
                wrapper.eq(User::getUsername, username);
            } else {
                wrapper.eq(User::getPhone, phone);
            }
            User user = this.getOne(wrapper, false);
            if (user == null) {
                log.warn("登录失败：用户不存在，username={}, phone={}", username, phone);
                return R.fail("用户名或密码错误");
            }

            // 4. 若明文中包含 password，进行密码校验（此处简单按明文比较，实际若使用 BCrypt 可替换）
            if (StringUtils.hasText(password) && StringUtils.hasText(user.getPassword())) {
                // 项目 User 实体注释提到 "加盐哈希，如 BCrypt"，这里做兼容：
                //   - 若数据库密码为 $2a$ 开头的 BCrypt 哈希，可在此集成 BCryptPasswordEncoder
                //   - 当前默认按相等比较作为兜底，未满足的密码强度校验由业务方接入 BCrypt 即可
                if (!password.equals(user.getPassword())
                        && !password.equals(stripBcryptMarker(user.getPassword()))) {
                    log.warn("登录失败：密码不匹配，userId={}", user.getId());
                    return R.fail("用户名或密码错误");
                }
            }

            // 5. 生成 UUID token，UserInfoVO 缓存到 Redis
            String token = UUID.randomUUID().toString().replace("-", "");
            UserInfoVO vo = toUserInfoVO(user, token);

            cacheUserInfo(token, vo);

            log.info("登录成功：userId={}, username={}, token={}", user.getId(), user.getUsername(), token);
            return R.success("登录成功", vo);

        } catch (Exception e) {
            log.error("登录处理异常：{}", e.getMessage(), e);
            return R.fail("登录失败：" + e.getMessage());
        }
    }

    @Override
    public R<Void> logout(String token) {
        try {
            if (!StringUtils.hasText(token)) {
                return R.fail("未登录");
            }
            String key = buildRedisKey(token);
            RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
            boolean deleted = bucket.delete();
            if (deleted) {
                log.info("退出登录成功，token={}", token);
            } else {
                log.debug("退出登录：Redis 中已不存在该 token，{}", token);
            }
            return R.success();
        } catch (Exception e) {
            log.error("退出登录异常：{}", e.getMessage(), e);
            return R.fail("退出登录失败");
        }
    }

    @Override
    public UserInfoVO getUserInfoByToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        try {
            String key = buildRedisKey(token);
            RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
            String json = bucket.get();
            if (!StringUtils.hasText(json)) {
                return null;
            }
            UserInfoVO vo = objectMapper.readValue(json, UserInfoVO.class);
            // Redis 中不存储 token 字段，防止被修改；读取后回写 token 方便上层使用
            if (vo != null) {
                vo.setToken(token);
            }
            return vo;
        } catch (JsonProcessingException e) {
            log.error("解析 Redis 中的 UserInfo 失败：{}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("从 Redis 读取 UserInfo 异常：{}", e.getMessage(), e);
            return null;
        }
    }

    // ============== 私有辅助方法 ==============

    /**
     * 将用户信息写入 Redis，带过期时间.
     */
    private void cacheUserInfo(String token, UserInfoVO vo) throws JsonProcessingException {
        String key = buildRedisKey(token);
        // 注意：存入 Redis 时去掉 token 字段，避免冗余
        UserInfoVO cached = new UserInfoVO();
        cached.setId(vo.getId());
        cached.setUsername(vo.getUsername());
        cached.setNickname(vo.getNickname());
        cached.setAvatar(vo.getAvatar());
        cached.setPhone(vo.getPhone());

        String json = objectMapper.writeValueAsString(cached);
        RBucket<String> bucket = redissonClient.getBucket(key, StringCodec.INSTANCE);
        bucket.set(json, AuthConstants.REDIS_USER_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 组装 Redis 键.
     */
    private String buildRedisKey(String token) {
        return AuthConstants.REDIS_USER_KEY_PREFIX + token;
    }

    /**
     * User -> UserInfoVO 转换.
     */
    private UserInfoVO toUserInfoVO(User u, String token) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setPhone(u.getPhone());
        vo.setToken(token);
        return vo;
    }

    /**
     * Object -> String 安全转换.
     */
    private static String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    /**
     * 占位方法：BCrypt 哈希以 $2a$ 开头，此方法返回原串，
     * 若集成 BCryptPasswordEncoder 可在此处做预处理。
     */
    private static String stripBcryptMarker(String passwordHash) {
        return passwordHash;
    }
}
