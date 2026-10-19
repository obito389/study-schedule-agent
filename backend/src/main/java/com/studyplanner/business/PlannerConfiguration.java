package com.studyplanner.business;
import java.time.Clock;
import org.springframework.context.annotation.*;
import com.studyplanner.planning.*;
import com.studyplanner.profile.*;
@Configuration
public class PlannerConfiguration {
    @Bean public HardConstraintValidator hardConstraintValidator() { return new HardConstraintValidator(); }
    @Bean public GreedyScheduler greedyScheduler(HardConstraintValidator validator, Clock clock) {
        return new GreedyScheduler(validator, clock);
    }
    @Bean public ProfileService profileService(ProfileFactPort port, Clock clock) {
        return new ProfileService(port, new DurationEstimator(), clock);
    }
}
