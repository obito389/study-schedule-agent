package com.studyplanner.team;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.importing.*;
import com.studyplanner.enums.*;
@Component @Primary public class TeamImportRepository implements ImportJobRepository {
 private final com.studyplanner.business.ImportJobRepository repository;private final TeamJson json;
 public TeamImportRepository(com.studyplanner.business.ImportJobRepository repository,TeamJson json){this.repository=repository;this.json=json;}
 @Override @Transactional(readOnly=true) public Optional<ImportJob> findByRequestKey(String key){return repository.findByUserId(TeamIdentity.user()).stream().filter(e -> e.getRequestKey().equals(key)&&e.getContractJson()!=null).findFirst().map(e -> json.read(e.getContractJson(),ImportJob.class));}
 @Override @Transactional public ImportJob save(ImportJob job){
  if(job.requestKey().length()>64)throw new IllegalArgumentException("requestKey 不能超过64字符");
  Long user=TeamIdentity.user();var e=repository.findByUserId(user).stream().filter(row -> row.getRequestKey().equals(job.requestKey())).findFirst().orElseGet(com.studyplanner.business.ImportJob::new);
  e.setUserId(user);e.setRequestKey(job.requestKey());e.setModality(job.sourceType()==ImportSourceType.SCREENSHOT?Modality.IMAGE:Modality.valueOf(job.sourceType().name()));
  e.setState(ImportState.valueOf(job.status().name()));e.setSourceRef(job.fileName());e.setProposalJson(json.write(job.proposal()));e.setContractJson(json.write(job));e.setCreatedAt(TeamSnapshotService.local(job.createdAt()));repository.save(e);return job;
 }
}
