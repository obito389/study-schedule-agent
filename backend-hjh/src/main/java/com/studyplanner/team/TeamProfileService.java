package com.studyplanner.team;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.business.AppUserRepository;
import com.studyplanner.profile.ProfileService;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.common.BusinessException;
@Service public class TeamProfileService {
 private final ProfileService profile;private final AppUserRepository users;private final TeamSnapshotService snapshots;private final TeamConstraintProjection projection;private final com.studyplanner.business.LearningStatRepository statistics;
 public TeamProfileService(ProfileService profile,AppUserRepository users,TeamSnapshotService snapshots,TeamConstraintProjection projection,com.studyplanner.business.LearningStatRepository statistics){this.profile=profile;this.users=users;this.snapshots=snapshots;this.projection=projection;this.statistics=statistics;}
 public List<ProfileFact> latest(){var latest=new LinkedHashMap<String,ProfileFact>();for(var f:profile.history())latest.put(f.key()+"\0"+f.scope(),f);return new ArrayList<>(latest.values());}
 @Transactional public ProfileFact change(String key,JsonNode value,String operation,long revision,Long factVersion){
  var u=users.findByIdForUpdate(TeamIdentity.user()).orElseThrow();if(u.getPlanningRevision()!=revision)throw new BusinessException(409,"日程已更新，请刷新偏好");
  var current=latest().stream().filter(f -> f.key().equals(key)&&f.scope().equals("GLOBAL")).findFirst().orElse(null);
  if(factVersion!=null&&((current==null?0:current.version())!=factVersion))throw new BusinessException(409,"偏好已被修改，请刷新");
  ProfileFact result;
  if(operation.equals("SET")){
   if(!key.equals("planning.dailyLimitMinutes")||value==null||!value.isIntegralNumber()||!value.canConvertToInt()||value.asInt()<30||value.asInt()>1440)throw new IllegalArgumentException("每日上限应为30至1440的整数分钟");
   result=profile.setExplicitPreference(key,value,"GLOBAL",List.of("manual"));
  }else if(operation.equals("DENY"))result=profile.denyFact(key,"GLOBAL",List.of("manual"));
  else result=profile.deleteFact(key,"GLOBAL",List.of("manual"));
  var snapshot=snapshots.snapshot();var violations=projection.validateState(snapshot,snapshot.tasks());if(!violations.isEmpty())throw new BusinessException(422,String.join("；",violations));
  if(key.startsWith(ProfileService.DURATION_SAMPLE_KEY_PREFIX))for(var stat:statistics.findByUserId(u.getId()))if("GLOBAL".equals(stat.getTaskType())){var estimate=profile.estimateDuration("GLOBAL",60,false);stat.setSampleCount(estimate.sampleCount());stat.setRatio(java.math.BigDecimal.valueOf(estimate.multiplier()));statistics.save(stat);}
  u.setPlanningRevision(u.getPlanningRevision()+1);users.save(u);return result;
 }
 public Object estimate(int minutes){return profile.estimateDuration("GLOBAL",minutes,false);}
}
