package com.studyplanner.team;
import java.time.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import com.studyplanner.agent.*;
import com.studyplanner.planning.*;
import com.studyplanner.profile.*;
@Configuration public class TeamConfiguration {
 @Bean public com.studyplanner.undo.UndoConstraintGuard undoGuard(TeamConstraintProjection projection){return projection::validateUndo;}
 @Bean public HardConstraintValidator sharedHardConstraints(){return new HardConstraintValidator();}
 @Bean public GreedyScheduler teamScheduler(HardConstraintValidator validator,Clock clock){return new GreedyScheduler(validator,clock);}
 @Bean public ProfileService coreProfileService(ProfileFactPort port,Clock clock){return new ProfileService(port,new DurationEstimator(),clock);}
 @Bean public AgentToolRegistry teamTools(GreedyScheduler scheduler){return new AgentToolRegistry(scheduler);}
 @Bean public ModelAdapter teamModel(ObjectMapper mapper,@Value("${team.ai.api-key:}")String key,@Value("${team.ai.base-url:https://api.deepseek.com}")String url,@Value("${team.ai.model:deepseek-flash}")String model){return new DeepSeekModelAdapter(mapper,key,url,model,Duration.ofSeconds(50));}
 @Bean public AgentService teamAgent(ModelAdapter adapter,AgentToolRegistry tools){return new AgentService(adapter,tools);}
}
