package com.studyplanner.common;

/**
 * 当前登录用户上下文（ThreadLocal）
 * 由拦截器从请求头 X-User-Id 解析后写入，Controller/Service 读取
 */
public class CurrentUserContext {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static boolean isLogin() {
        return CURRENT_USER_ID.get() != null;
    }

    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
