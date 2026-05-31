package com.campus.trade.user.service;

import com.campus.trade.common.ApiResult;
import com.campus.trade.message.repository.MessageRepository;
import com.campus.trade.security.JwtTokenProvider;
import com.campus.trade.user.dto.LoginRequest;
import com.campus.trade.user.dto.LoginResult;
import com.campus.trade.user.dto.RegisterRequest;
import com.campus.trade.user.entity.User;
import com.campus.trade.user.entity.UserAuth;
import com.campus.trade.user.repository.UserAuthRepository;
import com.campus.trade.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class UserService {
    private static final int MAX_FAIL = 5;
    private static final long LOCK_MINUTES = 10;
    private static final String TEST_CODE = "000000";

    private final UserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final BCryptPasswordEncoder encoder;
    private final JwtTokenProvider jwtProvider;
    private final MessageRepository messageRepository;

    private final Map<String, SmsCode> codeCache = new HashMap<>();
    private final Map<Long, List<String>> userTokens = new HashMap<>();

    public UserService(UserRepository ur, UserAuthRepository uar,
                       BCryptPasswordEncoder e, JwtTokenProvider j,
                       MessageRepository mr) {
        userRepository = ur; userAuthRepository = uar; encoder = e; jwtProvider = j;
        messageRepository = mr;
    }

    // ==================== 验证码 ====================

    public ApiResult<Void> sendCode(String phone) {
        if (phone == null || !phone.matches("^\\d{11}$"))
            return ApiResult.error(400, "手机号格式不正确");
        String code = String.format("%06d", new Random().nextInt(1000000));
        log.info("[模拟短信] {} → 验证码: {}", phone, code);
        codeCache.put(phone, new SmsCode(code, LocalDateTime.now().plusMinutes(5)));
        return ApiResult.success(null);
    }

    // ==================== 注册 ====================

    @Transactional
    public ApiResult<LoginResult> register(RegisterRequest req) {
        if (req.getPhone() == null || req.getUsername() == null || req.getPassword() == null)
            return ApiResult.error(400, "请填写所有必填字段");
        if (!req.getPhone().matches("^\\d{11}$"))
            return ApiResult.error(400, "手机号格式不正确");
        if (!TEST_CODE.equals(req.getVerificationCode())) {
            SmsCode c = codeCache.get(req.getPhone());
            if (c == null || c.isExpired()) return ApiResult.error(400, "验证码错误或已过期");
            if (!c.getCode().equals(req.getVerificationCode())) return ApiResult.error(400, "验证码错误");
        }
        if (req.getUsername().length() < 2 || req.getUsername().length() > 50)
            return ApiResult.error(400, "用户名长度须为2-50位");
        if (req.getStudentId() == null || req.getStudentId().length() > 20)
            return ApiResult.error(400, "学号格式不正确");
        if (!req.getPassword().equals(req.getConfirmPassword()))
            return ApiResult.error(400, "两次密码不一致");
        if (!req.getPassword().matches("^(?=.*[A-Za-z])(?=.*\\d).{8,20}$"))
            return ApiResult.error(400, "密码须为8-20位，含字母和数字");
        if (userRepository.existsByPhone(req.getPhone()))
            return ApiResult.error(400, "手机号已被注册");
        if (userRepository.existsByUsername(req.getUsername()))
            return ApiResult.error(400, "用户名已存在");
        if (userRepository.existsByStudentId(req.getStudentId()))
            return ApiResult.error(400, "学号已被注册");

        codeCache.remove(req.getPhone());

        // 模拟头像：生成一个模拟URL
        String avatarUrl = null;
        if (req.getAvatarBase64() != null && !req.getAvatarBase64().isEmpty()) {
            avatarUrl = "/avatars/" + req.getUsername() + "_" + System.currentTimeMillis() + ".jpg";
            log.info("[模拟头像] 保存头像: {}", avatarUrl);
        }


        User user = User.builder()
                .phone(req.getPhone()).username(req.getUsername())
                .passwordHash(encoder.encode(req.getPassword()))
                .studentId(req.getStudentId()).realName(req.getRealName())
                .department(req.getDepartment()).className(req.getClassName())
                .avatarUrl(avatarUrl)
                .creditScore(100).authStatus(0).role("user").build();
        userRepository.save(user);

        String token = jwtProvider.createToken(user.getId(), user.getUsername(), user.getRole(), false);
        recordToken(user.getId(), token);
        return ApiResult.success("注册成功", toResult(token, user));
    }

    // ==================== 登录 ====================

    @Transactional
    public ApiResult<LoginResult> login(LoginRequest req) {
        if (req.getAccount() == null || req.getPassword() == null)
            return ApiResult.error(400, "请输入账号和密码");

        Optional<User> userOpt = userRepository.findByUsername(req.getAccount().trim());
        if (userOpt.isEmpty()) userOpt = userRepository.findByPhone(req.getAccount().trim());
        if (userOpt.isEmpty()) return ApiResult.error(400, "账号不存在");
        User user = userOpt.get();

        if (user.getStatus() == 1) return ApiResult.error(403, "账号已被禁用");
        if (user.getLockTime() != null && LocalDateTime.now().isBefore(user.getLockTime())) {
            long m = java.time.Duration.between(LocalDateTime.now(), user.getLockTime()).toMinutes();
            return ApiResult.error(403, "账号已锁定，" + (m + 1) + "分钟后重试");
        }
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            userRepository.incrementFailCount(user.getId());
            int cnt = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            if (cnt >= MAX_FAIL) {
                userRepository.lockAccount(user.getId(), LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                return ApiResult.error(403, "密码错误次数过多，已锁定" + LOCK_MINUTES + "分钟");
            }
            return ApiResult.error(400, "密码错误，剩余" + (MAX_FAIL - cnt) + "次机会");
        }
        userRepository.resetFailCount(user.getId());
        String token = jwtProvider.createToken(user.getId(), user.getUsername(), user.getRole(),
                req.getRememberMe() != null && req.getRememberMe());
        recordToken(user.getId(), token);
        return ApiResult.success("登录成功", toResult(token, user));
    }

    public ApiResult<Void> logout(Long userId, String token) {
        if (userId != null && token != null) {
            List<String> ts = userTokens.get(userId);
            if (ts != null) { ts.remove(token); jwtProvider.invalidate(token); }
        }
        return ApiResult.success(null);
    }

    // ==================== 校园认证 ====================

    /**
     * 用户提交校园认证 → 状态设为"待审核"
     */
    @Transactional
    public ApiResult<String> submitAuth(Long userId, String studentId, String realName) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return ApiResult.error(404, "用户不存在");
        User user = userOpt.get();

        if (user.getAuthStatus() == 1) return ApiResult.error(400, "已认证，无需重复提交");

        // 检查是否有待审核的记录
        if (userAuthRepository.existsByUserIdAndStatus(userId, 0))
            return ApiResult.error(400, "已有认证申请正在审核中");

        // 学号姓名与注册信息一致性校验
        if (!studentId.equals(user.getStudentId()) || !realName.equals(user.getRealName()))
            return ApiResult.error(400, "学号或姓名与注册信息不一致");

        // 创建认证记录（待审核）
        UserAuth auth = UserAuth.builder()
                .userId(userId).studentId(studentId).realName(realName)
                .idCardUrl("/mock/idcard/" + userId + ".jpg")
                .campusCardUrl("/mock/campus/" + userId + ".jpg")
                .status(0) // 待审核
                .build();
        userAuthRepository.save(auth);

        return ApiResult.success("认证申请已提交，等待管理员审核");
    }

    /**
     * 管理员审核认证
     */
    @Transactional
    public ApiResult<String> reviewAuth(Long authId, Long reviewerId, boolean approved, String comment) {
        Optional<UserAuth> authOpt = userAuthRepository.findById(authId);
        if (authOpt.isEmpty()) return ApiResult.error(404, "认证记录不存在");
        UserAuth auth = authOpt.get();
        if (auth.getStatus() != 0) return ApiResult.error(400, "该记录已审核");

        auth.setStatus(approved ? 1 : 2);
        auth.setReviewerId(reviewerId);
        auth.setReviewComment(approved ? null : (comment != null ? comment : "认证信息不符"));
        auth.setReviewedAt(LocalDateTime.now());
        userAuthRepository.save(auth);

        // 更新用户认证状态
        userRepository.findById(auth.getUserId()).ifPresent(u -> {
            u.setAuthStatus(approved ? 1 : 2);
            userRepository.save(u);
        });

        return ApiResult.success(approved ? "认证已通过" : "认证已拒绝");
    }

    /**
     * 查询认证记录（用户自己查看）
     */
    public ApiResult<?> getMyAuth(Long userId) {
        Optional<UserAuth> authOpt = userAuthRepository.findByUserId(userId);
        if (authOpt.isEmpty()) return ApiResult.error(404, "未提交认证申请");
        UserAuth auth = authOpt.get();
        Map<String, Object> m = new HashMap<>();
        m.put("id", auth.getId());
        m.put("studentId", auth.getStudentId());
        m.put("realName", auth.getRealName());
        m.put("status", auth.getStatus());
        m.put("reviewComment", auth.getReviewComment());
        m.put("createdAt", auth.getCreatedAt());
        m.put("reviewedAt", auth.getReviewedAt());
        return ApiResult.success(m);
    }

    /**
     * 管理员查询待审核列表
     */
    public ApiResult<?> listPendingAuth(int page, int size) {
        var p = userAuthRepository.findByStatusOrderByCreatedAtDesc(0,
                org.springframework.data.domain.PageRequest.of(page, size));
        return ApiResult.success(p);
    }

    // ==================== 辅助 ====================

    private LoginResult toResult(String token, User user) {
        return new LoginResult(token, user.getId(), user.getUsername(), user.getRole(),
                user.getAuthStatus(), user.getAvatarUrl(), user.getCreditScore());
    }

    private void recordToken(Long userId, String token) {
        userTokens.computeIfAbsent(userId, k -> new ArrayList<>()).add(token);
        List<String> ts = userTokens.get(userId);
        if (ts.size() > 3) jwtProvider.invalidate(ts.remove(0));
    }

    private static class SmsCode {
        String code; LocalDateTime expireAt;
        SmsCode(String c, LocalDateTime e) { code = c; expireAt = e; }
        String getCode() { return code; }
        boolean isExpired() { return LocalDateTime.now().isAfter(expireAt); }
    }
}
