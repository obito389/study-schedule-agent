package com.studyplanner.team;
import java.util.*;
import java.math.BigDecimal;
import com.studyplanner.business.AppUserRepository;
import com.studyplanner.business.ProfileFactRepository;
import com.studyplanner.contract.ProfileFact;
import com.studyplanner.contract.ProfileFactState;
import com.studyplanner.profile.ProfileFactPort;
import com.studyplanner.enums.*;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;
@Component @Primary public class TeamProfilePort implements ProfileFactPort {
 private final ProfileFactRepository facts;private final AppUserRepository users;private final TeamJson json;private final com.studyplanner.business.ProfileEventRepository events;
 public TeamProfilePort(ProfileFactRepository facts,AppUserRepository users,TeamJson json,com.studyplanner.business.ProfileEventRepository events){this.facts=facts;this.users=users;this.json=json;this.events=events;}
 @Override @Transactional(readOnly=true) public List<ProfileFact> findAll(){return facts.findByUserId(TeamIdentity.user()).stream().map(e -> {
  if(e.getContractJson()!=null)return json.read(e.getContractJson(),ProfileFact.class);
  var state=e.getState()==FactState.ACTIVE?ProfileFactState.ACTIVE:(e.getState()==FactState.EXPIRED?ProfileFactState.EXPIRED:ProfileFactState.DENIED);
  try{return new ProfileFact(1,e.getKey(),json.mapper.readTree(e.getValueJson()),"GLOBAL",e.getConfidence().doubleValue(),e.getSource().name(),state,List.of(),Math.max(1,e.getVersion()));}
  catch(Exception x){throw new IllegalArgumentException("画像事实格式错误");}
 }).sorted(Comparator.comparingLong(ProfileFact::version)).toList();}
 @Override @Transactional public ProfileFact save(ProfileFact fact){
  Long id=TeamIdentity.user();var user=users.findByIdForUpdate(id).orElseThrow();
  var e=new com.studyplanner.business.ProfileFact();e.setUserId(id);e.setKey(fact.key());e.setValueJson(json.write(fact.value()));
  e.setScopeJson(json.write(fact.scope()));e.setEvidenceIds(json.write(fact.evidenceIds()));e.setVersion(fact.version());
  e.setConfidence(BigDecimal.valueOf(fact.strength()));e.setSource(fact.source().equals("TASK_COMPLETION")?FactSource.LEARNED:FactSource.MANUAL);
  e.setState(fact.state()==ProfileFactState.ACTIVE?FactState.ACTIVE:(fact.state()==ProfileFactState.EXPIRED?FactState.EXPIRED:FactState.REJECTED));
  e.setContractJson(json.write(fact));facts.save(e);var event=new com.studyplanner.business.ProfileEvent();event.setUserId(id);event.setEventKey("fact-"+UUID.randomUUID());event.setType("PROFILE_"+fact.state().name());event.setPayloadJson(json.write(fact));events.save(event);user.setProfileVersion(fact.version());users.save(user);return fact;
 }
}
