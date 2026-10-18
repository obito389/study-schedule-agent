package com.studyplanner.integration;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studyplanner.business.*;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;
import com.studyplanner.importing.*;
import com.studyplanner.profile.ProfileService;

@SpringBootTest @AutoConfigureMockMvc
class MinimumCompleteApiTest {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-10-19T00:00:00Z"),ZoneOffset.UTC); }
    }
    @Autowired ActionExecutor executor;
    @Autowired ScheduleStore store;
    @Autowired PlanningService planning;
    @Autowired CalendarService calendar;
    @Autowired ImportService importing;
    @Autowired ProfileService profile;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach void reset() {
        for (String table:List.of("ai_draft","action_execution","plan_preview","calendar_block","study_task","study_course","profile_fact_record","import_job"))
            jdbc.update("DELETE FROM "+table);
        jdbc.update("UPDATE user_schedule SET revision=0 WHERE id='demo'");
    }
    ObjectNode task(String title) {
        return mapper.createObjectNode().put("title",title).put("deadline","2026-10-21T00:00:00Z")
                .put("durationMinutes",60).put("priority",3);
    }
    ActionCommand command(String key,long revision,String type,ObjectNode payload) {
        return new ActionCommand(key,revision,type,"schedule:write:self",payload);
    }
    ExecutionReceipt create() { return executor.execute(command("create",0,"CREATE_TASK",task("数据结构作业"))); }
    ExecutionReceipt apply() {
        var preview=planning.preview();
        return executor.execute(command("apply",preview.baseRevision(),"APPLY_PLAN",mapper.createObjectNode().put("previewId",preview.previewId())));
    }
    @Test void createPlanConfirmReplayAndUndoPersistAsOneRevisionEach() {
        assertThat(create().status()).isEqualTo("APPLIED");
        var preview=planning.preview();
        assertThat(preview.candidate().blocks()).hasSize(2);
        assertThat(store.snapshot().tasks().getFirst().scheduledBlocks()).isEmpty();
        var command=command("save-plan",1,"APPLY_PLAN",mapper.createObjectNode().put("previewId",preview.previewId()));
        var receipt=executor.execute(command);
        assertThat(receipt.status()).isEqualTo("APPLIED");
        assertThat(executor.execute(command)).isEqualTo(receipt);
        assertThat(store.snapshot().planningRevision()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM calendar_block",Integer.class)).isEqualTo(2);
        var undo=command("undo",2,"UNDO",mapper.createObjectNode().put("executionId",receipt.executionId()));
        assertThat(executor.execute(undo).status()).isEqualTo("APPLIED");
        assertThat(executor.execute(undo).planningRevision()).isEqualTo(3);
        assertThat(store.snapshot().tasks().getFirst().scheduledBlocks()).isEmpty();
    }
    @Test void staleRevisionAndChangedOperationKeyNeverMutateTasks() {
        create();
        var stale=command("stale",0,"CREATE_TASK",task("另一个任务"));
        var failure=executor.execute(stale);
        assertThat(failure.errors().getFirst().code()).isEqualTo("REVISION_CONFLICT");
        assertThat(executor.execute(stale)).isEqualTo(failure);
        assertThat(executor.execute(command("create",1,"CREATE_TASK",task("改变"))).errors().getFirst().code())
                .isEqualTo("DUPLICATE_OPERATION_MISMATCH");
        assertThat(store.snapshot().tasks()).hasSize(1);
        assertThat(store.snapshot().planningRevision()).isEqualTo(1);
    }
    @Test void changingPreferenceInvalidatesSavedPreview() {
        create();var p=planning.preview();
        calendar.preference("planning.dailyLimitMinutes",mapper.getNodeFactory().numberNode(120),"SET",1);
        var r=executor.execute(command("old-preview",2,"APPLY_PLAN",mapper.createObjectNode().put("previewId",p.previewId())));
        assertThat(r.errors().getFirst().code()).isEqualTo("REVISION_CONFLICT");
        assertThat(store.snapshot().tasks().getFirst().scheduledBlocks()).isEmpty();
    }
    @Test void constraintFailureKeepsDatabaseAndRevisionUnchanged() {
        create();apply();var t=store.snapshot().tasks().getFirst();
        var block=t.scheduledBlocks().getFirst();
        var payload=mapper.createObjectNode().put("taskId",t.id()).put("blockId",block.id())
                .put("start","2026-10-22T00:00:00Z").put("end","2026-10-22T00:30:00Z");
        var r=executor.execute(command("bad-move",2,"MOVE_TASK",payload));
        assertThat(r.httpStatus()).isEqualTo(422);
        assertThat(r.errors().getFirst().code()).isEqualTo("DEADLINE_EXCEEDED");
        assertThat(store.snapshot().planningRevision()).isEqualTo(2);
        assertThat(store.snapshot().tasks().getFirst()).isEqualTo(t);
    }
    @Test void permissionsAndInvalidTaskAreRejectedWithStoredReceipts() {
        var denied=executor.execute(new ActionCommand("denied",0,"CREATE_TASK","schedule:write:other",task("task")));
        assertThat(denied.httpStatus()).isEqualTo(403);
        var invalid=task("bad").put("durationMinutes",0);
        assertThat(executor.execute(command("invalid",0,"CREATE_TASK",invalid)).httpStatus()).isEqualTo(400);
        assertThat(store.snapshot().tasks()).isEmpty();
    }
    @Test void completingTaskIsIdempotentAndRecordsExactlyOneDurationSample() {
        create();apply();String id=store.snapshot().tasks().getFirst().id();
        var c=command("complete",2,"COMPLETE_TASK",mapper.createObjectNode().put("taskId",id).put("actualMinutes",90));
        var receipt=executor.execute(c);assertThat(receipt.status()).isEqualTo("APPLIED");
        assertThat(executor.execute(c)).isEqualTo(receipt);
        assertThat(store.snapshot().tasks().getFirst().status()).isEqualTo(TaskStatus.DONE);
        assertThat(store.snapshot().tasks().getFirst().scheduledBlocks()).isEmpty();
        assertThat(profile.history()).hasSize(1);
        assertThat(receipt.undoable()).isFalse();
    }
    @Test void courseAndUnavailableBlocksParticipateInSchedulingAndPersist() {
        calendar.addCourse("课程",Instant.parse("2026-10-19T00:00:00Z"),Instant.parse("2026-10-19T01:00:00Z"),0);
        calendar.addBlock("休息",Instant.parse("2026-10-19T01:00:00Z"),Instant.parse("2026-10-19T02:00:00Z"),true,1);
        executor.execute(command("task",2,"CREATE_TASK",task("任务")));
        assertThat(planning.preview().candidate().blocks().getFirst().start()).isEqualTo(Instant.parse("2026-10-19T02:00:00Z"));
        assertThatThrownBy(() -> calendar.addBlock("冲突",Instant.parse("2026-10-19T00:30:00Z"),Instant.parse("2026-10-19T01:00:00Z"),true,3))
                .isInstanceOf(ApiException.class);
        assertThat(store.snapshot().planningRevision()).isEqualTo(3);
    }
    @Test void importsUseDatabaseAndCreateTaskOnlyAfterValidConfirmation() {
        var view=importing.submit(new ImportSource("import-test",ImportSourceType.SCREENSHOT,"screen.png",new byte[]{1}));
        assertThat(store.snapshot().tasks()).isEmpty();
        assertThatThrownBy(() -> importing.confirm("import-test",view.proposal())).isInstanceOf(IllegalArgumentException.class);
        var p=view.proposal();
        var confirmed=new ImportProposal(1,p.proposalId(),"ASSIGNMENT","导入任务",null,
                Instant.parse("2026-10-21T00:00:00Z"),60,p.sourceEvidence(),List.of(),ImportStatus.REVIEW,List.of());
        assertThat(importing.confirm("import-test",confirmed).status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(importing.confirm("import-test",confirmed).status()).isEqualTo(ImportStatus.COMMITTED);
        assertThat(store.snapshot().tasks()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM import_job",Integer.class)).isEqualTo(1);
    }
    @Test void concurrentWritersSerializeAndOneStaleWriterFails() throws Exception {
        try (var pool=Executors.newFixedThreadPool(2)) {
            var futures=pool.invokeAll(List.of(
                    () -> executor.execute(command("a",0,"CREATE_TASK",task("A"))),
                    () -> executor.execute(command("b",0,"CREATE_TASK",task("B")))));
            List<String> statuses=new ArrayList<>();for(var f:futures) statuses.add(((ExecutionReceipt)f.get()).status());
            assertThat(statuses).containsExactlyInAnyOrder("APPLIED","FAILED");
            assertThat(store.snapshot().tasks()).hasSize(1);
        }
    }
    @Test void httpEndpointsReturnUsableSnapshotAndConflict() throws Exception {
        mvc.perform(get("/api/v1/health")).andExpect(status().isOk()).andExpect(jsonPath("$.importMode").value("mock"));
        mvc.perform(post("/api/v1/actions").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(command("http",0,"CREATE_TASK",task("HTTP任务")))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPLIED"));
        mvc.perform(get("/api/v1/schedule")).andExpect(jsonPath("$.tasks[0].title").value("HTTP任务"));
        mvc.perform(post("/api/v1/actions").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(command("http-stale",0,"CREATE_TASK",task("过期")))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.errors[0].code").value("REVISION_CONFLICT"));
        mvc.perform(multipart("/api/v1/imports").file(new MockMultipartFile("file","sample.png","image/png",new byte[]{1}))
                .param("sourceType","SCREENSHOT").header("X-Request-Key","http-import"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVIEW"));
    }
    @Test void reorderedPayloadReplaysSameReceiptAndShortenedEstimateCannotOverbook() {
        var first=create();
        ObjectNode reordered=mapper.createObjectNode().put("priority",3).put("durationMinutes",60)
                .put("deadline","2026-10-21T00:00:00Z").put("title","数据结构作业");
        assertThat(executor.execute(command("create",0,"CREATE_TASK",reordered))).isEqualTo(first);
        apply();var t=store.snapshot().tasks().getFirst();
        var shortened=task(t.title()).put("taskId",t.id()).put("durationMinutes",30);
        assertThat(executor.execute(command("shorten",2,"UPDATE_TASK",shortened)).errors().getFirst().code())
                .isEqualTo("DURATION_BELOW_SCHEDULED");
        assertThat(store.snapshot().tasks().getFirst().estimatedDurationMinutes()).isEqualTo(60);
    }
    @Test void laterTaskChangesBlockUndoAndInvalidPreferenceRollsBack() {
        var created=create();
        String id=store.snapshot().tasks().getFirst().id();
        executor.execute(command("update",1,"UPDATE_TASK",task("修改后的任务").put("taskId",id)));
        var undo=executor.execute(command("undo-conflict",2,"UNDO",mapper.createObjectNode().put("executionId",created.executionId())));
        assertThat(undo.errors().getFirst().code()).isEqualTo("UNDO_CONFLICT");
        apply();
        assertThatThrownBy(() -> calendar.preference("planning.dailyLimitMinutes",mapper.getNodeFactory().numberNode(30),"SET",3))
                .isInstanceOf(ApiException.class);
        assertThat(profile.history()).isEmpty();assertThat(store.snapshot().planningRevision()).isEqualTo(3);
    }
    @Test void deletingDurationEvidenceRecomputesEstimate() {
        for(int i=0;i<3;i++) {
            long rev=store.snapshot().planningRevision();
            executor.execute(command("sample-create-"+i,rev,"CREATE_TASK",task("样本"+i)));
            String id=store.snapshot().tasks().stream().filter(t -> t.status()==TaskStatus.TODO).findFirst().orElseThrow().id();
            executor.execute(command("sample-complete-"+i,rev+1,"COMPLETE_TASK",mapper.createObjectNode().put("taskId",id).put("actualMinutes",120)));
        }
        assertThat(profile.estimateDuration("GLOBAL",60,false).multiplier()).isGreaterThan(1);
        var fact=profile.history().getFirst();
        calendar.preference(fact.key(),null,"DELETE",6);
        assertThat(profile.estimateDuration("GLOBAL",60,false).sampleCount()).isEqualTo(2);
        assertThat(profile.estimateDuration("GLOBAL",60,false).multiplier()).isEqualTo(1);
    }
}
