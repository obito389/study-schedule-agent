package com.studyplanner.config;

import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 全局配置：给所有接口统一补充 X-User-Id 请求头参数。
 * 头部解析由 CurrentUserInterceptor 全局完成，各 Controller 无需逐个声明；
 * 此处仅用于让 Swagger UI 每个接口都出现 X-User-Id 输入框，便于测试数据隔离。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenApiCustomizer userIdHeaderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        List<Parameter> params = operation.getParameters();
                        boolean exists = params != null && params.stream().anyMatch(p ->
                                "X-User-Id".equals(p.getName()) && "header".equals(p.getIn()));
                        if (!exists) {
                            operation.addParametersItem(new HeaderParameter()
                                    .name("X-User-Id")
                                    .required(false)
                                    .description("当前登录用户 id（数据隔离/执行器鉴权用）")
                                    .schema(new IntegerSchema().format("int64")));
                        }
                    }));
        };
    }
}
