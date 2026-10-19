package com.studyplanner.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.business.*;
import com.studyplanner.enums.*;
import com.studyplanner.executor.*;
import com.studyplanner.undo.*;
import org.junit.jupiter.api.Test;
import com.studyplanner.common.BusinessException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Regression coverage for the PR 7 executor review. */
class ExecutorReviewRegressionTest {
    final AppUserRepository users = mock(AppUserRepository.class);
    final TaskRepository tasks = mock(TaskRepository.class);
    final TaskStepRepository steps = mock(TaskStepRepository.class);
    final TimeBlockRepository blocks = mock(TimeBlockRepository.class);
    final AgentActionRepository records = mock(AgentActionRepository.class);
    final ActionConstraintValidator validator = mock(ActionConstraintValidator.class);
    final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    ActionExecutor executor() { return new ActionExecutor(users,tasks,steps,blocks,records,validator,mapper); }
    AppUser user(long revision) { AppUser u=new AppUser(); u.setId(1L); u.setPlanningRevision(revision); return u; }
    Task task(String title) { Task t=new Task(); t.setId(7L); t.setUserId(1L); t.setTitle(title); return t; }
    ActionCommand command(ActionType type, String key) { ActionCommand a=new ActionCommand(); a.setType(type); a.setOperationKey(key); return a; }
    ActionBatchRequest request(ActionCommand cmd, long revision) { ActionBatchRequest r=new ActionBatchRequest(); r.setUserId(1L); r.setBaseRevision(revision); r.setActions(List.of(cmd)); return r; }

    @Test void rejectedForeignTaskPreviewReturnsNoSnapshots() {
        Task foreign=task("FOREIGN-PRIVATE-TITLE"); foreign.setUserId(2L);
        when(users.findById(1L)).thenReturn(Optional.of(user(0)));
        when(tasks.findById(7L)).thenReturn(Optional.of(foreign));
        when(validator.validate(eq(1L),anyList())).thenReturn(new ArrayList<>(List.of("无权操作他人任务")));
        ActionCommand cmd=command(ActionType.UPDATE_TASK,"foreign-preview"); cmd.setTargetId(7L); cmd.setTitle("replacement");
        ActionPreviewResult result=executor().preview(request(cmd,0));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getItems()).isEmpty();
        assertThat(foreign.getTitle()).isEqualTo("FOREIGN-PRIVATE-TITLE");
    }

    @Test void sameKeyWithChangedPayloadIsRejected() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1)));
        AgentAction previous=new AgentAction(); previous.setUserId(1L); previous.setOperationKey("same-key"); previous.setType(ActionType.CREATE_TASK); previous.setStatus(ActionStatus.APPLIED); previous.setAfterJson("{\"title\":\"original\"}");
        when(records.findByUserIdAndOperationKeyIn(eq(1L),anyList())).thenReturn(List.of(previous));
        ActionCommand cmd=command(ActionType.CREATE_TASK,"same-key"); cmd.setTitle("different payload");
        assertThatThrownBy(() -> executor().apply(request(cmd,0))).isInstanceOf(BusinessException.class);
        verifyNoInteractions(validator);
    }

    @Test void movingLockedBlockIsRejected() {
        TimeBlock locked=new TimeBlock(); locked.setId(7L); locked.setUserId(1L); locked.setLocked(true);
        when(blocks.findById(7L)).thenReturn(Optional.of(locked));
        ActionCommand cmd=command(ActionType.UPDATE_BLOCK,"move-locked"); cmd.setTargetId(7L);
        cmd.setStartAt(LocalDateTime.parse("2026-10-20T10:00:00")); cmd.setEndAt(LocalDateTime.parse("2026-10-20T11:00:00"));
        DefaultConstraintValidator actual=new DefaultConstraintValidator(tasks,steps,blocks,mock(CourseRepository.class));
        assertThat(actual.validate(1L,List.of(cmd))).anyMatch(message -> message.contains("锁定"));
    }

    @Test void undoingOldUpdatePreservesALaterEdit() throws Exception {
        AppUser currentUser=user(2);
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(currentUser));
        Task current=task("LATER-EDIT-TO-PRESERVE");
        when(tasks.findById(7L)).thenReturn(Optional.of(current));
        AgentAction oldAction=new AgentAction(); oldAction.setId(10L); oldAction.setUserId(1L); oldAction.setOperationKey("old-update"); oldAction.setType(ActionType.UPDATE_TASK); oldAction.setStatus(ActionStatus.APPLIED);
        oldAction.setBeforeJson(mapper.writeValueAsString(task("original")));
        oldAction.setAfterJson(mapper.writeValueAsString(task("first edit")));
        when(records.findByUserIdAndOperationKey(1L,"old-update")).thenReturn(Optional.of(oldAction));
        UndoRequest request=new UndoRequest(); request.setUserId(1L); request.setBaseRevision(2L); request.setOperationKey("old-update");
        assertThatThrownBy(() -> new UndoExecutor(users,tasks,steps,blocks,records,mapper).undoOne(request))
                .isInstanceOf(BusinessException.class);
        assertThat(current.getTitle()).isEqualTo("LATER-EDIT-TO-PRESERVE");
        assertThat(currentUser.getPlanningRevision()).isEqualTo(2L);
        verify(records, never()).save(any());
    }

    @Test void validPreviewDoesNotMutateManagedTask() {
        when(users.findById(1L)).thenReturn(Optional.of(user(0)));
        Task original=task("original");
        when(tasks.findById(7L)).thenReturn(Optional.of(original));
        when(validator.validate(eq(1L),anyList())).thenReturn(List.of());
        ActionCommand cmd=command(ActionType.UPDATE_TASK,"preview"); cmd.setTargetId(7L); cmd.setTitle("suggestion");
        var result=executor().preview(request(cmd,0));
        assertThat(result.isValid()).isTrue();
        assertThat(result.getItems().getFirst().getAfterJson()).contains("suggestion");
        assertThat(original.getTitle()).isEqualTo("original");
        verify(tasks,never()).save(any());
    }

    @Test void stalePreviewDoesNotReadEntitySnapshots() {
        when(users.findById(1L)).thenReturn(Optional.of(user(1)));
        when(validator.validate(eq(1L),anyList())).thenReturn(List.of());
        var result=executor().preview(request(command(ActionType.UPDATE_TASK,"stale"),0));
        assertThat(result.isValid()).isFalse();
        assertThat(result.getItems()).isEmpty();
        verifyNoInteractions(tasks);
    }

    @Test void exactRetryReturnsOriginalResultWithoutWriting() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(4)));
        ActionCommand cmd=command(ActionType.CREATE_TASK,"retry"); cmd.setTitle("original");
        var request=request(cmd,0);
        AgentAction previous=new AgentAction(); previous.setUserId(1L); previous.setOperationKey("retry");
        previous.setType(ActionType.CREATE_TASK); previous.setStatus(ActionStatus.APPLIED);
        previous.setRequestHash(RequestFingerprint.of(request,mapper)); previous.setAfterJson("{\"title\":\"original\"}");
        when(records.findByUserIdAndOperationKeyIn(eq(1L),anyList())).thenReturn(List.of(previous));
        var result=executor().apply(request);
        assertThat(result.isIdempotent()).isTrue();
        assertThat(result.getItems().getFirst().getAfterJson()).contains("original");
        verifyNoInteractions(validator,tasks);
        verify(users,never()).save(any());
        verify(records,never()).save(any());
    }

    @Test void changedPayloadWithBoundKeyIsRejected() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1)));
        var cmd=command(ActionType.CREATE_TASK,"bound");cmd.setTitle("original");
        var request=request(cmd,0);
        AgentAction previous=new AgentAction();previous.setOperationKey("bound");previous.setStatus(ActionStatus.APPLIED);
        previous.setRequestHash(RequestFingerprint.of(request,mapper));
        when(records.findByUserIdAndOperationKeyIn(eq(1L),anyList())).thenReturn(List.of(previous));
        cmd.setTitle("changed");
        assertThatThrownBy(() -> executor().apply(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tasks,validator);
    }

    @Test void undoneKeyCannotBeAppliedAgain() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(0)));
        var request=request(command(ActionType.CREATE_TASK,"undone"),0);
        AgentAction previous=new AgentAction();previous.setOperationKey("undone");previous.setStatus(ActionStatus.UNDONE);
        previous.setRequestHash(RequestFingerprint.of(request,mapper));
        when(records.findByUserIdAndOperationKeyIn(eq(1L),anyList())).thenReturn(List.of(previous));
        assertThatThrownBy(() -> executor().apply(request)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(tasks,validator);
    }

    @Test void newApplyPersistsFingerprintAndAdvancesRevisionOnce() {
        AppUser user=user(0);when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(tasks.save(any())).thenAnswer(call -> { Task t=call.getArgument(0);t.setId(7L);return t; });
        var cmd=command(ActionType.CREATE_TASK,"new");cmd.setTitle("original");
        var request=request(cmd,0);
        var result=executor().apply(request);
        assertThat(result.isIdempotent()).isFalse();assertThat(user.getPlanningRevision()).isEqualTo(1);
        verify(records).save(argThat(a -> RequestFingerprint.of(request,mapper).equals(a.getRequestHash())
                && a.getStatus()==ActionStatus.APPLIED));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=ActionType.class,names={"UPDATE_BLOCK","DELETE_BLOCK"})
    void lockedBlockCannotBeUnlockedOrDeleted(ActionType type) {
        TimeBlock block=new TimeBlock();block.setId(7L);block.setUserId(1L);block.setLocked(true);
        when(blocks.findById(7L)).thenReturn(Optional.of(block));
        var cmd=command(type,"locked");cmd.setTargetId(7L);cmd.setLocked(false);
        var actual=new DefaultConstraintValidator(tasks,steps,blocks,mock(CourseRepository.class));
        assertThat(actual.validate(1L,List.of(cmd))).anyMatch(message -> message.contains("锁定"));
    }

    @Test void unlockedBlockCanBeMoved() {
        TimeBlock block=new TimeBlock();block.setId(7L);block.setUserId(1L);block.setLocked(false);
        when(blocks.findById(7L)).thenReturn(Optional.of(block));
        var cmd=command(ActionType.UPDATE_BLOCK,"move");cmd.setTargetId(7L);
        cmd.setStartAt(LocalDateTime.parse("2026-10-20T10:00:00"));cmd.setEndAt(LocalDateTime.parse("2026-10-20T11:00:00"));
        var actual=new DefaultConstraintValidator(tasks,steps,blocks,mock(CourseRepository.class));
        assertThat(actual.validate(1L,List.of(cmd))).isEmpty();
    }

    @Test void unchangedTaskCanBeUndoneAndActionIsReadAfterLock() throws Exception {
        var user=user(1);when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        Task current=task("first edit");current.setUpdatedAt(LocalDateTime.parse("2026-10-20T10:00:00"));
        when(tasks.findById(7L)).thenReturn(Optional.of(current));
        var old=new AgentAction();old.setId(10L);old.setUserId(1L);old.setOperationKey("undo");
        old.setType(ActionType.UPDATE_TASK);old.setStatus(ActionStatus.APPLIED);
        old.setBeforeJson(mapper.writeValueAsString(task("original")));old.setAfterJson(mapper.writeValueAsString(task("first edit")));
        when(records.findByUserIdAndOperationKey(1L,"undo")).thenReturn(Optional.of(old));
        var request=new UndoRequest();request.setUserId(1L);request.setBaseRevision(1L);request.setOperationKey("undo");
        new UndoExecutor(users,tasks,steps,blocks,records,mapper).undoOne(request);
        assertThat(current.getTitle()).isEqualTo("original");assertThat(user.getPlanningRevision()).isEqualTo(2);
        assertThat(old.getStatus()).isEqualTo(ActionStatus.UNDONE);
        var order=inOrder(users,records);order.verify(users).findByIdForUpdate(1L);
        order.verify(records).findByUserIdAndOperationKey(1L,"undo");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=ActionStatus.class,names={"APPLIED","UNDONE"})
    void failureAuditCannotOverwriteTerminalAction(ActionStatus status) {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(1)));
        var cmd=command(ActionType.CREATE_TASK,"audit");var request=request(cmd,0);
        AgentAction previous=new AgentAction();previous.setOperationKey("audit");previous.setStatus(status);
        previous.setAfterJson("original-snapshot");
        when(records.findByUserIdAndOperationKey(1L,"audit")).thenReturn(Optional.of(previous));
        new ActionAuditService(records,users,mapper).recordFailure(1L,request,"ERROR");
        assertThat(previous.getStatus()).isEqualTo(status);
        assertThat(previous.getAfterJson()).isEqualTo("original-snapshot");
        verify(records,never()).save(any());
    }

    @Test void newFailureBindsItsKeyUnderUserLock() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(0)));
        var request=request(command(ActionType.CREATE_TASK,"failure"),0);
        new ActionAuditService(records,users,mapper).recordFailure(1L,request,"CONSTRAINT");
        var order=inOrder(users,records);order.verify(users).findByIdForUpdate(1L);
        order.verify(records).findByUserIdAndOperationKey(1L,"failure");
        verify(records).save(argThat(a -> a.getStatus()==ActionStatus.FAILED
                && RequestFingerprint.of(request,mapper).equals(a.getRequestHash())));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value=ActionType.class,names={"CREATE_TASK","UPDATE_TASK","CREATE_STEP","UPDATE_STEP","CREATE_BLOCK","UPDATE_BLOCK"})
    void undoRejectsLaterChangesForEveryEntityType(ActionType type) throws Exception {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(2)));
        Object current;Object applied;
        if (type.name().endsWith("TASK")) {
            current=task("later");applied=task("applied");
            when(tasks.findById(7L)).thenReturn(Optional.of((Task)current));
        } else if (type.name().endsWith("STEP")) {
            TaskStep cur=new TaskStep();cur.setId(7L);cur.setTitle("later");current=cur;
            TaskStep old=new TaskStep();old.setId(7L);old.setTitle("applied");applied=old;
            when(steps.findById(7L)).thenReturn(Optional.of(cur));
        } else {
            TimeBlock cur=new TimeBlock();cur.setId(7L);cur.setUserId(1L);cur.setStartAt(LocalDateTime.parse("2026-10-20T10:00:00"));current=cur;
            TimeBlock old=new TimeBlock();old.setId(7L);old.setUserId(1L);old.setStartAt(LocalDateTime.parse("2026-10-20T09:00:00"));applied=old;
            when(blocks.findById(7L)).thenReturn(Optional.of(cur));
        }
        AgentAction action=new AgentAction();action.setUserId(1L);action.setType(type);action.setStatus(ActionStatus.APPLIED);
        action.setOperationKey("changed");action.setBeforeJson(mapper.writeValueAsString(applied));action.setAfterJson(mapper.writeValueAsString(applied));
        when(records.findByUserIdAndOperationKey(1L,"changed")).thenReturn(Optional.of(action));
        var request=new UndoRequest();request.setUserId(1L);request.setBaseRevision(2L);request.setOperationKey("changed");
        assertThatThrownBy(() -> new UndoExecutor(users,tasks,steps,blocks,records,mapper).undoOne(request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("后续操作修改");
        verify(records,never()).save(any());verify(tasks,never()).delete(any());
        verify(steps,never()).delete(any());verify(blocks,never()).delete(any());
    }

    @Test void batchOrderAndAuthorizationArePartOfRequestIdentity() {
        var first=command(ActionType.CREATE_TASK,"first");first.setTitle("one");
        var second=command(ActionType.CREATE_TASK,"second");second.setTitle("two");
        var request=request(first,0);request.setActions(List.of(first,second));
        String original=RequestFingerprint.of(request,mapper);
        request.setActions(List.of(second,first));
        assertThat(RequestFingerprint.of(request,mapper)).isNotEqualTo(original);
        request.setActions(List.of(first,second));request.setAuthorization("changed scope");
        assertThat(RequestFingerprint.of(request,mapper)).isNotEqualTo(original);
    }

    @Test void lateFailureCannotRebindFailedKeyToAnotherPayload() {
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(user(0)));
        var request=request(command(ActionType.CREATE_TASK,"failure"),0);
        AgentAction previous=new AgentAction();previous.setId(3L);previous.setStatus(ActionStatus.FAILED);
        previous.setRequestHash("original-fingerprint");previous.setErrorCode("ORIGINAL");
        when(records.findByUserIdAndOperationKey(1L,"failure")).thenReturn(Optional.of(previous));
        new ActionAuditService(records,users,mapper).recordFailure(1L,request,"ERROR");
        assertThat(previous.getRequestHash()).isEqualTo("original-fingerprint");
        assertThat(previous.getErrorCode()).isEqualTo("ORIGINAL");verify(records,never()).save(any());
    }
}
