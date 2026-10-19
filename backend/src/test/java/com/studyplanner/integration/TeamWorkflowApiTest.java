package com.studyplanner.integration;

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
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyplanner.business.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class TeamWorkflowApiTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock teamClock() { return Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"),ZoneId.of("Asia/Shanghai")); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired ScheduleStore store;
    @Autowired ActionExecutor executor;
    @Autowired CalendarService calendar;
    @BeforeEach void reset() {
        for (String table:List.of("ai_draft","action_execution","plan_preview","calendar_block","study_task","study_course","profile_fact_record","import_job")) jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE user_schedule SET revision=0 WHERE id='demo'");
    }
    ObjectNode task(String title) { return mapper.createObjectNode().put("title",title).put("deadline","2026-10-23T00:00:00Z").put("durationMinutes",60).put("priority",3); }
    ObjectNode batch(String key,long revision) {
        var body=mapper.createObjectNode().put("operationKey",key).put("baseRevision",revision);
        var actions=body.putArray("actions");
        actions.addObject().put("type","CREATE_TASK").set("payload",task("数据库"));
        actions.addObject().put("type","CREATE_TASK").set("payload",task("软件工程"));
        return body;
    }
    JsonNode postJson(String path,Object body,int expected) throws Exception {
        var result=mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body)))
                .andExpect(status().is(expected)).andReturn();
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }
    @Test void batchPreviewIsReadOnlyApplyIsAtomicRetryAndUndoWork() throws Exception {
        var body=batch("batch",0);
        var preview=postJson("/api/v1/executor/preview",body,200);
        assertThat(preview.path("valid").asBoolean()).isTrue();
        assertThat(store.snapshot().tasks()).isEmpty();
        var receipt=postJson("/api/v1/executor/apply",body,200);
        assertThat(store.snapshot().tasks()).hasSize(2);assertThat(store.snapshot().planningRevision()).isEqualTo(1);
        assertThat(postJson("/api/v1/executor/apply",body,200)).isEqualTo(receipt);
        ((ObjectNode)body.withArray("actions").get(1).get("payload")).put("title","不同请求");
        postJson("/api/v1/executor/apply",body,409);
        assertThat(store.snapshot().tasks()).hasSize(2);
        postJson("/api/v1/actions/"+receipt.path("executionId").asText()+"/undo",Map.of("operationKey","undo-batch","baseRevision",1),200);
        assertThat(store.snapshot().tasks()).isEmpty();assertThat(store.snapshot().planningRevision()).isEqualTo(2);
    }
    @Test void invalidSecondActionLeavesNoFirstActionOrRevision() throws Exception {
        var body=batch("invalid-batch",0);((ObjectNode)body.withArray("actions").get(1).get("payload")).put("durationMinutes",0);
        var preview=postJson("/api/v1/executor/preview",body,200);
        assertThat(preview.path("valid").asBoolean()).isFalse();assertThat(preview.path("afterTasks").isEmpty()).isTrue();
        postJson("/api/v1/executor/apply",body,400);
        assertThat(store.snapshot().tasks()).isEmpty();assertThat(store.snapshot().planningRevision()).isZero();
        assertThat(executor.history()).hasSize(1);assertThat(executor.history().getFirst().status()).isEqualTo("FAILED");
    }
    @Test void calendarAndPreferenceWritesGoThroughAuditedExecutor() {
        calendar.addCourse("数据库",Instant.parse("2026-10-21T01:00:00Z"),Instant.parse("2026-10-21T02:00:00Z"),0);
        calendar.addBlock("休息",Instant.parse("2026-10-21T02:00:00Z"),Instant.parse("2026-10-21T03:00:00Z"),true,1);
        calendar.preference("planning.dailyLimitMinutes",mapper.getNodeFactory().numberNode(120),"SET",2);
        assertThat(executor.history()).hasSize(3);
        assertThat(executor.history()).allMatch(r -> r.status().equals("APPLIED"));
        assertThat(store.snapshot().planningRevision()).isEqualTo(3);
    }
    @Test void undoRevalidatesAgainstCoursesAddedAfterTheOriginalMove() {
        var payload=task("撤销约束验收").put("start","2026-10-20T01:00:00Z").put("end","2026-10-20T02:00:00Z");
        executor.execute(new ActionCommand("undo-create",0,"CREATE_TASK","schedule:write:self",payload));
        var task=store.snapshot().tasks().getFirst();
        var move=mapper.createObjectNode().put("taskId",task.id()).put("blockId",task.scheduledBlocks().getFirst().id())
                .put("start","2026-10-20T03:00:00Z").put("end","2026-10-20T04:00:00Z");
        var receipt=executor.execute(new ActionCommand("undo-move",1,"MOVE_TASK","schedule:write:self",move));
        calendar.addCourse("后来添加的课程",Instant.parse("2026-10-20T01:00:00Z"),Instant.parse("2026-10-20T02:00:00Z"),2);
        var undo=executor.execute(new ActionCommand("undo-conflict",3,"UNDO","schedule:write:self",mapper.createObjectNode().put("executionId",receipt.executionId())));
        assertThat(undo.httpStatus()).isEqualTo(422);
        assertThat(store.snapshot().planningRevision()).isEqualTo(3);
        assertThat(store.snapshot().tasks().getFirst().scheduledBlocks().getFirst().start()).isEqualTo(Instant.parse("2026-10-20T03:00:00Z"));
    }
}
