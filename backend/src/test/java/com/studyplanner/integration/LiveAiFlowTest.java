package com.studyplanner.integration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fasterxml.jackson.databind.*;
import com.studyplanner.agent.*;
import com.studyplanner.business.*;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;

@SpringBootTest(properties={"app.ai.api-key=test-key","app.ai.model=test-model"}) @AutoConfigureMockMvc
class LiveAiFlowTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testClock(){return Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"),ZoneOffset.UTC);}
    }
    @MockitoBean(name="liveModelAdapter") ModelAdapter model;
    @Autowired LiveAiService ai;
    @Autowired AiDraftService drafts;
    @Autowired ScheduleStore store;
    @Autowired ActionExecutor executor;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @BeforeEach void clean(){
        for(String table:List.of("ai_draft","action_execution","plan_preview","calendar_block","study_task","study_course","profile_fact_record","import_job"))jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE user_schedule SET revision=0 WHERE id='demo'");
    }
    ModelAdapter.ModelResponse create(boolean withTime){
        Map<String,JsonNode> changes=new LinkedHashMap<>();changes.put("title",mapper.getNodeFactory().textNode("AI复习数据库"));
        changes.put("deadline",mapper.getNodeFactory().textNode("2026-10-20T14:00:00Z"));
        changes.put("durationMinutes",mapper.getNodeFactory().numberNode(60));changes.put("priority",mapper.getNodeFactory().numberNode(4));
        if(withTime){changes.put("start",mapper.getNodeFactory().textNode("2026-10-20T12:00:00Z"));changes.put("end",mapper.getNodeFactory().textNode("2026-10-20T13:00:00Z"));}
        return new ModelAdapter.ModelResponse(ActionType.CREATE_TASK,List.of(),changes,List.of("理解为明晚20点学习1小时"),List.of());
    }
    @Test void actualDraftRequiresConfirmationAndReplaysWithoutAnotherModelCall(){
        when(model.generate(anyString(),any())).thenReturn(create(true));
        var preview=ai.preview("明晚八点学习数据库一个小时");
        assertThat(preview.status()).isEqualTo("NEEDS_CONFIRMATION");assertThat(preview.provider()).isEqualTo("DeepSeek");
        assertThat(preview.candidate().blocks()).hasSize(1);assertThat(store.snapshot().tasks()).isEmpty();
        var receipt=drafts.confirm(preview.draftId());assertThat(receipt.status()).isEqualTo("APPLIED");
        assertThat(drafts.confirm(preview.draftId())).isEqualTo(receipt);
        assertThat(store.snapshot().tasks()).hasSize(1);assertThat(store.snapshot().tasks().getFirst().scheduledBlocks()).hasSize(1);
        verify(model,times(1)).generate(anyString(),any());
    }
    @Test void missingInformationNeverProducesWritableDraft(){
        when(model.generate(anyString(),any())).thenReturn(ModelAdapter.ModelResponse.clarification(ActionType.CREATE_TASK,List.of("请说明预计耗时和截止时间")));
        var p=ai.preview("安排学习");assertThat(p.status()).isEqualTo("NEEDS_CLARIFICATION");assertThat(p.clarifications()).isNotEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_draft",Integer.class)).isZero();
        assertThat(store.snapshot().tasks()).isEmpty();
    }
    @Test void windowIsAppliedWithoutChangingFormalDeadline(){
        var payload=mapper.createObjectNode().put("title","已有任务").put("deadline","2026-10-22T14:00:00Z").put("durationMinutes",60).put("priority",3);
        executor.execute(new ActionCommand("manual",0,"CREATE_TASK","schedule:write:self",payload));
        when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.GENERATE_SCHEDULE,List.of(),Map.of(
                "start",mapper.getNodeFactory().textNode("2026-10-20T10:00:00Z"),"end",mapper.getNodeFactory().textNode("2026-10-20T14:00:00Z")),List.of("限定明晚18到22点"),List.of()));
        var p=ai.preview("明晚18到22点安排已有任务");assertThat(p.candidate().blocks()).hasSize(2);
        assertThat(p.candidate().blocks().getFirst().start()).isEqualTo(Instant.parse("2026-10-20T10:00:00Z"));
        assertThat(drafts.confirm(p.draftId()).status()).isEqualTo("APPLIED");
        assertThat(store.snapshot().tasks().getFirst().deadline()).isEqualTo(Instant.parse("2026-10-22T14:00:00Z"));
    }
    @Test void modelCallDoesNotHoldTransactionAndConcurrentChangeInvalidatesResponse(){
        when(model.generate(anyString(),any())).thenAnswer(invocation->{
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            var p=mapper.createObjectNode().put("title","并发任务").put("deadline","2026-10-22T14:00:00Z").put("durationMinutes",60).put("priority",3);
            executor.execute(new ActionCommand("concurrent",0,"CREATE_TASK","schedule:write:self",p));return create(false);
        });
        assertThatThrownBy(()->ai.preview("创建任务")).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("REVISION_CONFLICT"));
        assertThat(store.snapshot().tasks()).hasSize(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_draft",Integer.class)).isZero();
    }
    @Test void dangerousUnsupportedActionAndPastTimeAreRejected(){
        when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.CANCEL_TASK,List.of("task"),Map.of(),List.of(),List.of()));
        assertThatThrownBy(()->ai.preview("取消任务")).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_UNSUPPORTED_ACTION"));
        var response=create(true);var changes=new LinkedHashMap<>(response.changes());
        changes.put("start",mapper.getNodeFactory().textNode("2026-10-18T10:00:00Z"));changes.put("end",mapper.getNodeFactory().textNode("2026-10-18T11:00:00Z"));
        when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.CREATE_TASK,List.of(),changes,List.of(),List.of()));
        assertThatThrownBy(()->ai.preview("过去的时间")).isInstanceOf(ApiException.class);assertThat(store.snapshot().tasks()).isEmpty();
        changes.put("start",mapper.getNodeFactory().textNode("not-a-date"));
        when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.CREATE_TASK,List.of(),changes,List.of(),List.of()));
        assertThatThrownBy(()->ai.preview("错误日期")).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_INVALID_TASK"));
    }
    @Test void apiExposesRealFailureAndDoesNotFallbackToMock() throws Exception {
        when(model.generate(anyString(),any())).thenThrow(new ApiException(503,"AI_BALANCE_REQUIRED","AI 服务余额不足"));
        mvc.perform(post("/api/v1/ai/previews").contentType(MediaType.APPLICATION_JSON).content("{\"instruction\":\"创建任务\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_BALANCE_REQUIRED"));
        assertThat(store.snapshot().tasks()).isEmpty();
    }
    @Test void confirmUsesSavedCommandAndRejectsStaleRevision() throws Exception {
        when(model.generate(anyString(),any())).thenReturn(create(false));var p=ai.preview("创建任务");
        var other=mapper.createObjectNode().put("title","其他任务").put("deadline","2026-10-22T14:00:00Z").put("durationMinutes",60).put("priority",3);
        executor.execute(new ActionCommand("other",0,"CREATE_TASK","schedule:write:self",other));
        mvc.perform(post("/api/v1/ai/drafts/"+p.draftId()+"/confirm").contentType(MediaType.APPLICATION_JSON).content("{\"payload\":{\"title\":\"篡改\"}}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors[0].code").value("REVISION_CONFLICT"));
        assertThat(store.snapshot().tasks()).hasSize(1);
    }
    @Test void missingConfigurationIsHonest(){
        var unconfigured=new LiveAiService(model,store,drafts,"","test-model");assertThat(unconfigured.status().configured()).isFalse();
        assertThatThrownBy(()->unconfigured.preview("查询")).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.code).isEqualTo("AI_NOT_CONFIGURED"));
        verifyNoInteractions(model);
    }
}
