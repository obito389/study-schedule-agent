package com.studyplanner.team;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.contract.ImportProposal;
import com.studyplanner.importing.ImportCommitPort;
import com.studyplanner.business.*;
import com.studyplanner.executor.*;
@Component @Primary public class TeamImportCommitPort implements ImportCommitPort {
 private final AppUserRepository users;private final AgentActionRepository actions;private final ActionExecutor executor;private final TeamJson json;private final ImportJobRepository imports;
 public TeamImportCommitPort(AppUserRepository users,AgentActionRepository actions,ActionExecutor executor,TeamJson json,ImportJobRepository imports){this.users=users;this.actions=actions;this.executor=executor;this.json=json;this.imports=imports;}
 @Override @Transactional public void commit(ImportProposal p){
  if(!p.kind().equals("ASSIGNMENT"))throw new IllegalArgumentException("当前支持任务导入");
  Long user=TeamIdentity.user();var u=users.findByIdForUpdate(user).orElseThrow();String key="import-"+UUID.nameUUIDFromBytes(p.proposalId().getBytes(StandardCharsets.UTF_8));
  var old=actions.findByUserIdAndOperationKey(user,key).orElse(null);long revision=old!=null&&old.getStatus()==com.studyplanner.enums.ActionStatus.APPLIED?old.getBaseRevision():u.getPlanningRevision();
  var a=TeamActionService.command(key,com.studyplanner.enums.ActionType.CREATE_TASK);a.setTitle(p.title());a.setDeadline(TeamSnapshotService.local(p.deadline()));a.setRemainingMin(p.durationMinutes());a.setInitialEstimateMin(p.durationMinutes());a.setPriority(2);
  imports.findByUserId(user).stream().filter(job -> job.getProposalJson()!=null && !job.getProposalJson().equals("null"))
   .filter(job -> p.proposalId().equals(json.read(job.getProposalJson(),ImportProposal.class).proposalId())).findFirst().ifPresent(job -> a.setImportJobId(job.getId()));
  var request=new ActionBatchRequest();request.setUserId(user);request.setBaseRevision(revision);request.setAuthorization(json.write(p));request.setActions(List.of(a));executor.apply(request);
 }
}
