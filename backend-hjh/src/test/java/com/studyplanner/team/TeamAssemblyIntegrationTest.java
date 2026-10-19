package com.studyplanner.team;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import com.studyplanner.StudyPlannerApplication;
import com.studyplanner.agent.ModelAdapter;
import com.studyplanner.contract.ActionType;

/** Opt in with TEAM_TEST_DB_URL pointing to a dedicated, disposable MySQL schema. */
@SpringBootTest(classes={StudyPlannerApplication.class, TeamAssemblyIntegrationTest.TestClock.class}, properties={
    "spring.datasource.url=${TEAM_TEST_DB_URL}", "spring.datasource.username=${TEAM_DB_USER:root}",
    "spring.datasource.password=${TEAM_DB_PASSWORD:}", "team.ai.api-key=test"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="TEAM_TEST_DB_URL", matches=".+")
class TeamAssemblyIntegrationTest {
    @TestConfiguration static class TestClock {
        @Bean @Primary Clock fixedClock() { return Clock.fixed(Instant.parse("2026-10-20T00:00:00Z"), ZoneId.of("Asia/Shanghai")); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate db;
    @MockitoBean ModelAdapter model;
    long user;
    @BeforeEach void newUser() {
        String name="integration-"+UUID.randomUUID();
        db.update("INSERT INTO app_user(username,password_hash,timezone,planning_revision,profile_version,learning_enabled,created_at) VALUES (?, 'test', 'Asia/Shanghai', 0, 0, 1, NOW(3))", name);
        user=db.queryForObject("SELECT id FROM app_user WHERE username=?", Long.class, name);
    }
    JsonNode result(MvcResult r) throws Exception {
        JsonNode root=json.readTree(r.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        return root.has("data")?root.get("data"):root;
    }
    JsonNode getData(String path) throws Exception {
        return result(mvc.perform(get("/api/v1"+path).header("X-User-Id",user)).andExpect(status().isOk()).andReturn());
    }
    JsonNode postData(String path,Object body) throws Exception {
        return result(mvc.perform(post("/api/v1"+path).header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isOk()).andReturn());
    }
    long revision() throws Exception { return getData("/schedule/snapshot").path("planningRevision").asLong(); }
    JsonNode create() throws Exception {
        return postData("/workspace/tasks", Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",revision(),"title","成员拼装任务","deadline","2026-10-22T13:00:00Z","durationMinutes",60,"priority",2));
    }
    long count(String table) {return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE user_id=?",Long.class,user);}
    JsonNode preview() throws Exception {return postData("/actions/plan",Map.of("baseRevision",revision()));}
    JsonNode confirm(JsonNode a) throws Exception {return postData("/actions/"+a.path("operationKey").asText()+"/confirm",Map.of("changes",Map.of("title","不可篡改")));}
    JsonNode undo(JsonNode a,String key,long base) throws Exception {return postData("/actions/undo",Map.of("operationKey",key,"executionId",a.path("operationKey").asText(),"baseRevision",base));}

    @Test void rulePreviewConfirmUndoUsesMemberTablesAndIsIdempotent() throws Exception {
        create(); var a=preview();
        assertThat(a.path("status").asText()).isEqualTo("NEEDS_CONFIRMATION");
        assertThat(count("plan_draft")).isEqualTo(1);assertThat(count("time_block")).isZero();
        confirm(a);assertThat(count("time_block")).isGreaterThan(0);
        long rev=revision();confirm(a);assertThat(revision()).isEqualTo(rev);
        String key=UUID.randomUUID().toString();undo(a,key,rev);assertThat(count("time_block")).isZero();
        long after=revision();undo(a,key,rev);assertThat(revision()).isEqualTo(after);
        assertThat(count("agent_action")).isGreaterThanOrEqualTo(2);
    }
    @Test void stalePreviewCannotBeConfirmed() throws Exception {
        create();var a=preview();create();
        mvc.perform(post("/api/v1/actions/"+a.path("operationKey").asText()+"/confirm").header("X-User-Id",user)).andExpect(status().isConflict());
        assertThat(count("time_block")).isZero();
    }
    void modelCreatesScheduledTask() {
        var fields=new LinkedHashMap<String,JsonNode>();
        fields.put("title",json.valueToTree("AI 指定时段"));fields.put("deadline",json.valueToTree("2026-10-22T13:00:00Z"));
        fields.put("durationMinutes",json.valueToTree(60));fields.put("start",json.valueToTree("2026-10-20T02:00:00Z"));fields.put("end",json.valueToTree("2026-10-20T03:00:00Z"));
        when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.CREATE_TASK,List.of(),fields,List.of(),List.of()));
    }
    @Test void aiCreationWithBlockHasAuditsAndCanUndoInReverseOrder() throws Exception {
        modelCreatesScheduledTask();var a=postData("/actions",Map.of("prompt","创建任务","baseRevision",0));
        assertThat(count("task")).isZero();confirm(a);
        assertThat(count("task")).isEqualTo(1);assertThat(count("time_block")).isEqualTo(1);assertThat(count("agent_action")).isEqualTo(2);
        undo(a,UUID.randomUUID().toString(),revision());assertThat(count("task")).isZero();assertThat(count("time_block")).isZero();
    }
    @Test void laterChildPreventsUnsafeUndo() throws Exception {
        modelCreatesScheduledTask();var a=postData("/actions",Map.of("prompt","创建任务","baseRevision",0));confirm(a);
        String id=getData("/schedule/snapshot").path("tasks").get(0).path("id").asText();
        postData("/workspace/tasks/"+id+"/steps",Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",revision(),"title","后来添加的步骤","orderNo",1,"minutes",30));
        mvc.perform(post("/api/v1/actions/undo").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(Map.of("operationKey",UUID.randomUUID().toString(),"executionId",a.path("operationKey").asText(),"baseRevision",revision())))).andExpect(status().isConflict());
        assertThat(count("task")).isEqualTo(1);assertThat(count("time_block")).isEqualTo(1);
    }
    @Test void completionPersistsFeedbackFactsEventsAndStatsExactlyOnce() throws Exception {
        String id=create().path("tasks").get(0).path("id").asText();confirm(preview());
        var input=Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",revision(),"actualMinutes",90);
        postData("/workspace/tasks/"+id+"/complete",input);postData("/workspace/tasks/"+id+"/complete",input);
        assertThat(count("execution_log")).isEqualTo(1);assertThat(count("profile_fact")).isEqualTo(1);
        assertThat(count("profile_event")).isEqualTo(1);assertThat(count("learning_stat")).isEqualTo(1);
        assertThat(getData("/schedule/snapshot").path("tasks").get(0).path("scheduledBlocks").size()).isZero();
        create();confirm(preview()); // Completed slots are released consistently by scheduler and executor.
    }
    @Test void importReviewIsDurableAndConfirmationCreatesLinkedTaskOnce() throws Exception {
        var upload=multipart("/api/v1/imports").file(new MockMultipartFile("file","assignment.png","image/png",new byte[]{1,2,3})).param("sourceType","SCREENSHOT").header("X-Request-Key","import-"+UUID.randomUUID()).header("X-User-Id",user);
        var job=result(mvc.perform(upload).andExpect(status().isOk()).andReturn());
        assertThat(count("import_job")).isEqualTo(1);assertThat(count("task")).isZero();
        var p=(com.fasterxml.jackson.databind.node.ObjectNode)job.path("proposal");p.put("title","校对后的作业");p.put("deadline","2026-10-22T13:00:00Z");p.put("durationMinutes",60);
        p.set("ambiguities",json.createArrayNode());p.set("errors",json.createArrayNode());
        String path="/imports/"+job.path("requestKey").asText()+"/confirm";
        assertThat(postData(path,p).path("status").asText()).isEqualTo("COMMITTED");postData(path,p);
        assertThat(count("task")).isEqualTo(1);
        assertThat(db.queryForObject("SELECT import_job_id FROM task WHERE user_id=?",Long.class,user)).isNotNull();
    }
    @Test void profileRevisionAndFactsArePersistedAndStaleUpdatesFail() throws Exception {
        var req=put("/api/v1/profile/facts/planning.dailyLimitMinutes").header("X-User-Id",user).contentType("application/json").content("{\"value\":180,\"version\":0,\"baseRevision\":0}");
        mvc.perform(req).andExpect(status().isOk());assertThat(revision()).isEqualTo(1);
        mvc.perform(req).andExpect(status().isConflict());assertThat(count("profile_fact")).isEqualTo(1);assertThat(count("profile_event")).isEqualTo(1);
    }
    @Test void anotherUserCannotConfirmDraftAndLegacyWritesAreBlocked() throws Exception {
        create();var a=preview();
        mvc.perform(post("/api/v1/actions/"+a.path("operationKey").asText()+"/confirm").header("X-User-Id",1)).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/tasks").header("X-User-Id",user).contentType("application/json").content("{}")).andExpect(status().isConflict());
    }
    @Test void pastDeadlineRejectsWithoutPartialWrites() throws Exception {
        mvc.perform(post("/api/v1/workspace/tasks").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",0,"title","过去任务","deadline","2020-01-01T00:00:00Z","durationMinutes",60,"priority",2)))).andExpect(status().is4xxClientError());
        assertThat(count("task")).isZero();assertThat(count("agent_action")).isZero();assertThat(revision()).isZero();
    }
    @Test void memberCoursesAndWindowsParticipateInSharedConstraints() throws Exception {
        var input=new LinkedHashMap<String,Object>(Map.of("baseRevision",0,"title","成员课程","firstDate","2026-10-20","startTime","10:00","endTime","11:00","weeks",16));
        var snapshot=postData("/workspace/calendar/courses",input);
        assertThat(snapshot.path("courses").size()).isEqualTo(1);
        assertThat(snapshot.path("courses").get(0).path("meetingBlocks").size()).isGreaterThan(1);
        modelCreatesScheduledTask();
        mvc.perform(post("/api/v1/actions").header("X-User-Id",user).contentType("application/json").content("{\"prompt\":\"创建重叠任务\",\"baseRevision\":1}")).andExpect(status().is4xxClientError());
        assertThat(count("task")).isZero();assertThat(count("plan_draft")).isZero();
        input.put("baseRevision",1);input.put("startTime","14:00");input.put("endTime","15:00");
        assertThat(postData("/workspace/calendar/windows",input).path("lockedBlocks").size()).isEqualTo(1);
    }
    @Test void conflictingFixedWindowRollsBackAndProfileLimitCannotInvalidateSchedule() throws Exception {
        create();confirm(preview());var block=getData("/schedule/snapshot").path("tasks").get(0).path("scheduledBlocks").get(0);
        var start=Instant.parse(block.path("start").asText()).atZone(ZoneId.of("Asia/Shanghai"));
        var end=Instant.parse(block.path("end").asText()).atZone(ZoneId.of("Asia/Shanghai"));
        mvc.perform(post("/api/v1/workspace/calendar/windows").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(Map.of("baseRevision",revision(),"firstDate",start.toLocalDate().toString(),"startTime",start.toLocalTime().toString(),"endTime",end.toLocalTime().toString(),"weeks",1)))).andExpect(status().isUnprocessableEntity());
        assertThat(count("availability_window")).isZero();
        mvc.perform(put("/api/v1/profile/facts/planning.dailyLimitMinutes").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(Map.of("value",30,"baseRevision",revision(),"version",0)))).andExpect(status().isUnprocessableEntity());
        assertThat(count("profile_fact")).isZero();
    }
    @Test void deniedDurationEvidenceRefreshesStatistics() throws Exception {
        String id=create().path("tasks").get(0).path("id").asText();
        postData("/workspace/tasks/"+id+"/complete",Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",revision(),"actualMinutes",90));
        var fact=getData("/profile/facts").get(0);
        postData("/profile/facts/"+fact.path("key").asText()+"/deny",Map.of("baseRevision",revision(),"version",fact.path("version").asLong()));
        assertThat(db.queryForObject("SELECT sample_count FROM learning_stat WHERE user_id=?",Integer.class,user)).isZero();
        assertThat(count("profile_fact")).isEqualTo(2);
    }
    Map<String,Object> command(String key,String type,Object payload,long revision) {
        return Map.of("operationKey",key,"baseRevision",revision,"type",type,"authorizationScope","schedule:write:self","payload",payload);
    }
    @Test void finalAiSchedulingHonorsExplicitWindow() throws Exception {
        create();when(model.generate(anyString(),any())).thenReturn(new ModelAdapter.ModelResponse(ActionType.GENERATE_SCHEDULE,List.of(),Map.of("start",json.valueToTree("2026-10-21T10:00:00Z"),"end",json.valueToTree("2026-10-21T12:00:00Z")),List.of(),List.of()));
        var a=postData("/actions",Map.of("prompt","明天晚上18至20点安排任务","baseRevision",revision()));
        for(var b:a.path("changes").path("blocks")){assertThat(Instant.parse(b.path("start").asText())).isAfterOrEqualTo(Instant.parse("2026-10-21T10:00:00Z"));assertThat(Instant.parse(b.path("end").asText())).isBeforeOrEqualTo(Instant.parse("2026-10-21T12:00:00Z"));}
        confirm(a);
    }
    Map<String,Object> taskPayload(String title) {
        return Map.of("title",title,"deadline","2026-10-22T13:00:00Z","durationMinutes",60,"priority",2,"courseId","");
    }
    @Test void finalManualCommandRetriesAreBoundAndUndoPersists() throws Exception {
        String key=UUID.randomUUID().toString();var input=command(key,"CREATE_TASK",taskPayload("最终版任务"),0);
        var receipt=postData("/workspace/commands",input);postData("/workspace/commands",input);
        assertThat(count("task")).isEqualTo(1);assertThat(count("plan_draft")).isEqualTo(1);
        mvc.perform(post("/api/v1/workspace/commands").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(command(key,"CREATE_TASK",taskPayload("篡改"),0)))) .andExpect(status().isConflict());
        postData("/workspace/commands",command(UUID.randomUUID().toString(),"UNDO",Map.of("executionId",receipt.path("executionId").asText()),revision()));
        assertThat(count("task")).isZero();assertThat(getData("/actions").get(1).path("undoable").asBoolean()).isFalse();
    }
    @Test void finalRulePreviewAndReceiptUndoShareMemberExecutor() throws Exception {
        postData("/workspace/commands",command(UUID.randomUUID().toString(),"CREATE_TASK",taskPayload("排程任务"),0));
        var p=postData("/planning/previews",Map.of());assertThat(count("time_block")).isZero();
        var r=postData("/workspace/commands",command(UUID.randomUUID().toString(),"APPLY_PLAN",Map.of("previewId",p.path("previewId").asText()),revision()));
        assertThat(count("time_block")).isEqualTo(2);
        postData("/workspace/commands",command(UUID.randomUUID().toString(),"UNDO",Map.of("executionId",r.path("executionId").asText()),revision()));
        assertThat(count("time_block")).isZero();
    }
    @Test void finalAiPreviewConfirmCannotBeTampered() throws Exception {
        modelCreatesScheduledTask();var p=postData("/ai/previews",Map.of("instruction","请安排任务"));
        assertThat(p.path("status").asText()).isEqualTo("NEEDS_CONFIRMATION");assertThat(count("task")).isZero();
        postData("/ai/drafts/"+p.path("draftId").asText()+"/confirm",Map.of("title","恶意替换"));
        assertThat(getData("/schedule").path("tasks").get(0).path("title").asText()).isEqualTo("AI 指定时段");
    }
    @Test void finalCompleteFeedbackCannotBeUndoneThroughReceipts() throws Exception {
        var s=postData("/workspace/commands",command(UUID.randomUUID().toString(),"CREATE_TASK",taskPayload("反馈任务"),0));
        var id=getData("/schedule").path("tasks").get(0).path("id").asText();
        var r=postData("/workspace/commands",command(UUID.randomUUID().toString(),"COMPLETE_TASK",Map.of("taskId",id,"actualMinutes",80),revision()));
        assertThat(r.path("undoable").asBoolean()).isFalse();assertThat(count("execution_log")).isEqualTo(1);
    }
    @Test void finalCourseAndFixedAdaptersUseRealMemberRows() throws Exception {
        postData("/workspace/fixed",Map.of("name","软件工程","kind","COURSE","start","2026-10-21T02:00:00Z","end","2026-10-21T03:00:00Z","baseRevision",0));
        var course=getData("/schedule").path("courses").get(0).path("id").asText();
        mvc.perform(delete("/api/v1/workspace/fixed/COURSE/"+course).header("X-User-Id",user).param("baseRevision",String.valueOf(revision()))).andExpect(status().isOk());
        assertThat(getData("/schedule").path("courses").size()).isZero();
    }
    @Test void finalCommandsRejectUnsupportedTypeAndWrongScope() throws Exception {
        for(var c:List.of(command(UUID.randomUUID().toString(),"DELETE_DATABASE",Map.of(),0),Map.of("operationKey",UUID.randomUUID().toString(),"baseRevision",0,"type","CREATE_TASK","authorizationScope","admin","payload",taskPayload("越权")))){
            mvc.perform(post("/api/v1/workspace/commands").header("X-User-Id",user).contentType("application/json").content(json.writeValueAsBytes(c))).andExpect(status().is4xxClientError());
        }
        assertThat(count("task")).isZero();assertThat(revision()).isZero();
    }
}
