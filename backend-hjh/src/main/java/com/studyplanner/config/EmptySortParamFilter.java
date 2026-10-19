package com.studyplanner.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 过滤空白的 sort 请求参数
 * 解决 Swagger 排序框留空时传入 sort= 空字符串，导致 No property '' found 报错的问题
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmptySortParamFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public String[] getParameterValues(String name) {
                String[] values = super.getParameterValues(name);
                if ("sort".equals(name) && values != null) {
                    List<String> nonEmpty = new ArrayList<>();
                    for (String v : values) {
                        if (v != null && !v.isBlank()) {
                            nonEmpty.add(v);
                        }
                    }
                    return nonEmpty.isEmpty() ? null : nonEmpty.toArray(new String[0]);
                }
                return values;
            }

            @Override
            public String getParameter(String name) {
                if ("sort".equals(name)) {
                    String[] values = getParameterValues(name);
                    return (values == null || values.length == 0) ? null : values[0];
                }
                return super.getParameter(name);
            }
        }, response);
    }
}
