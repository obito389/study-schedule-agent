package com.studyplanner.config;

import com.studyplanner.business.AppUserRepository;
import com.studyplanner.common.BusinessException;
import com.studyplanner.common.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 从请求头 X-User-Id 解析当前登录用户，写入 CurrentUserContext。
 *
 * <p>第 4 周收紧：/api/** 下的接口一律要求登录（唯一例外是 POST /api/v1/users，用于开通账号）。</p>
 * <ul>
 *   <li>未携带 X-User-Id：返回 401，不再回落到“返回全部数据”；</li>
 *   <li>X-User-Id 非数字：返回 401；</li>
 *   <li>X-User-Id 对应的用户在库中不存在：返回 401（此前任意数字都会被当作已登录，越权/空数据）。</li>
 * </ul>
 * 后续接入 JWT 登录后，改为从 token 解析即可。
 */
@Component
public class CurrentUserInterceptor implements HandlerInterceptor {

    private final AppUserRepository userRepository;

    public CurrentUserInterceptor(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("X-User-Id");

        // 账号开通接口允许匿名访问（用于创建首个用户）
        if (isAnonymousAllowed(request)) {
            trySetUserIdIfValid(header);
            return true;
        }

        if (header == null || header.isBlank()) {
            throw new BusinessException(401, "请先登录（请求头携带 X-User-Id）");
        }

        long userId;
        try {
            userId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(401, "登录态无效：X-User-Id 必须为数字");
        }

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(401, "登录用户不存在或已失效: X-User-Id=" + userId);
        }

        CurrentUserContext.setUserId(userId);
        return true;
    }

    /** 匿名接口：带了合法请求头就顺带识别，否则忽略（不报错） */
    private void trySetUserIdIfValid(String header) {
        if (header == null || header.isBlank()) {
            return;
        }
        try {
            long userId = Long.parseLong(header.trim());
            if (userRepository.existsById(userId)) {
                CurrentUserContext.setUserId(userId);
            }
        } catch (NumberFormatException ignored) {
            // 匿名接口忽略非法请求头
        }
    }

    private boolean isAnonymousAllowed(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/users".equals(request.getRequestURI());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
