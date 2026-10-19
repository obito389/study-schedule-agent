package com.studyplanner.importing;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import com.studyplanner.agent.ModelAdapter;
import com.studyplanner.business.ScheduleStore;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImportConfiguration {

    @Bean
    public Clock applicationClock() {
        return Clock.system(ZoneId.of("Asia/Shanghai"));
    }

    @Bean
    public ImportModelPort importModelPort(@Qualifier("liveModelAdapter") ModelAdapter model,ScheduleStore store,
            @Value("${app.ai.api-key:${DEEPSEEK_API_KEY:}}") String key) {
        return new TextImportModelAdapter(model,store,key!=null && !key.isBlank());
    }

    @Bean
    public ImportService importService(
            ImportModelPort modelPort,
            ImportJobRepository repository,
            ImportCommitPort commitPort,
            Clock clock) {
        return new ImportService(modelPort, repository, commitPort, clock);
    }
}
