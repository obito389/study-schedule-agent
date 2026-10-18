package com.studyplanner.agent;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
@Configuration
public class AiConfiguration {
    @Bean("liveModelAdapter")
    public ModelAdapter liveModelAdapter(ObjectMapper mapper,
            @Value("${app.ai.api-key:${DEEPSEEK_API_KEY:}}") String key,
            @Value("${app.ai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${app.ai.model:deepseek-flash}") String model) {
        return new DeepSeekModelAdapter(mapper,key,baseUrl,model,Duration.ofSeconds(50));
    }
}
