package com.studyplanner.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页参数配置：过滤 Swagger/前端传入的空白排序参数，避免 No property '' found 报错
 */
@Configuration
public class PageableConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SortHandlerMethodArgumentResolver sortResolver = new SortHandlerMethodArgumentResolver() {
            @Override
            public Sort resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                Sort sort = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
                List<Sort.Order> validOrders = new ArrayList<>();
                for (Sort.Order order : sort) {
                    if (order.getProperty() != null && !order.getProperty().isBlank()) {
                        validOrders.add(order);
                    }
                }
                return validOrders.isEmpty() ? Sort.unsorted() : Sort.by(validOrders);
            }
        };
        PageableHandlerMethodArgumentResolver pageableResolver =
                new PageableHandlerMethodArgumentResolver(sortResolver);
        pageableResolver.setOneIndexedParameters(false);
        resolvers.add(pageableResolver);
    }
}
