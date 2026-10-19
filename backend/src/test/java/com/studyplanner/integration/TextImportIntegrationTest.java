package com.studyplanner.integration;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fasterxml.jackson.databind.*;
import com.studyplanner.agent.*;
import com.studyplanner.business.*;
import com.studyplanner.contract.*;
import com.studyplanner.importing.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties={"app.ai.api-key=test-key"})
class TextImportIntegrationTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock textClock(){return Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"),ZoneId.of("Asia/Shanghai"));}
    }
    @MockitoBean(name="liveModelAdapter") ModelAdapter model;
    @Autowired ImportService imports;
    @Autowired ScheduleStore store;
    @Autowired ActionExecutor executor;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @BeforeEach void clean() {
        for(String table:List.of("ai_draft","action_execution","plan_preview","calendar_block","study_task","study_course","profile_fact_record","import_job"))jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE user_schedule SET revision=0 WHERE id='demo'");
    }
    ImportSource source(String key){return new ImportSource(key,ImportSourceType.TEXT,"学习要求.txt","复习数据库，明晚截止，预计60分钟".getBytes(StandardCharsets.UTF_8));}
    @Test void modelInterpretationIsOutsideTransactionAndConfirmationUsesExecutorExactlyOnce() {
        when(model.generate(anyString(),any())).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new ModelAdapter.ModelResponse(ActionType.CREATE_TASK,List.of(),Map.of(
                "title",mapper.getNodeFactory().textNode("文本识别数据库"),"deadline",mapper.getNodeFactory().textNode("2026-10-23T00:00:00Z"),
                "durationMinutes",mapper.getNodeFactory().numberNode(60)),List.of("原始文字"),List.of());
        });
        var proposal=imports.submit(source("text")).proposal();
        assertThat(proposal.title()).isEqualTo("文本识别数据库");assertThat(store.snapshot().tasks()).isEmpty();
        assertThat(imports.confirm("text",proposal).status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(imports.confirm("text",proposal).status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(store.snapshot().tasks()).hasSize(1);assertThat(executor.history()).hasSize(1);
        verify(model,times(1)).generate(anyString(),any());
    }
    @Test void providerFailureIsExplicitAndCanRetryWithoutCreatingTasks() {
        when(model.generate(anyString(),any())).thenThrow(new IllegalStateException("offline"));
        assertThat(imports.submit(source("failed")).status()).isEqualTo(ImportStatus.FAILED);
        assertThat(imports.submit(source("failed")).status()).isEqualTo(ImportStatus.FAILED);
        verify(model,times(2)).generate(anyString(),any());assertThat(store.snapshot().tasks()).isEmpty();
    }
    @Test void mediaWithoutRecognitionIsManualAndNeverSentToTextModel() {
        var proposal=imports.submit(new ImportSource("image",ImportSourceType.SCREENSHOT,"screen.png",new byte[]{1})).proposal();
        assertThat(proposal.ambiguities()).isNotEmpty();assertThat(proposal.deadline()).isNull();verifyNoInteractions(model);
    }
}
