package com.studyplanner.importing;

import java.time.Clock;
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
    public ImportModelPort importModelPort() {
        return new MockImportModelAdapter();
    }

    @Bean
    public ImportJobRepository coreInMemoryImportJobRepository() {
        return new InMemoryImportJobRepository();
    }

    @Bean
    public ImportCommitPort coreInMemoryImportCommitPort() {
        return new InMemoryImportCommitPort();
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
