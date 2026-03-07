package com.guanlong.trading.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guanlong.trading.domain.User;
import com.guanlong.trading.dto.ApiResponse;
import com.guanlong.trading.infra.persistence.UserRepository;
import com.guanlong.trading.security.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理", description = "登录、注册、Token相关接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtils.generateToken(request.username());

        User user = userRepository.findByUsername(request.username());

        return ApiResponse.success(new LoginResponse(
                token,
                "Bearer",
                jwtUtils.getExpiration(),
                new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar())
        ));
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.countByUsername(request.username()) > 0) {
            return ApiResponse.error(400, "用户名已存在");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .email(request.email())
                .status(1)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.insert(user);
        return ApiResponse.success();
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public ApiResponse<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.unauthorized("未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ApiResponse.notFound("用户不存在");
        }

        return ApiResponse.success(new UserInfo(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        ));
    }

    @Operation(summary = "刷新Token")
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.unauthorized("未登录");
        }

        String username = authentication.getName();
        String token = jwtUtils.generateToken(username);
        User user = userRepository.findByUsername(username);

        return ApiResponse.success(new LoginResponse(
                token,
                "Bearer",
                jwtUtils.getExpiration(),
                new UserInfo(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar())
        ));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.unauthorized("未登录");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ApiResponse.notFound("用户不存在");
        }

        // 验证旧密码
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword())) {
            return ApiResponse.error(400, "旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.updateById(user);

        return ApiResponse.success();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        SecurityContextHolder.clearContext();
        return ApiResponse.success();
    }

    public record LoginRequest(String username, String password) {}

    public record RegisterRequest(String username, String password, String nickname, String email) {}

    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

    public record LoginResponse(
            String token,
            String tokenType,
            Long expiresIn,
            UserInfo user
    ) {}

    public record UserInfo(Long id, String username, String nickname, String avatar) {}
}
