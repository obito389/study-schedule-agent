package com.studyplanner.business;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.studyplanner.common.JsonCodec;
import com.studyplanner.persistence.*;
import com.studyplanner.planning.*;
@Service
public class PlanningService {
    public record Preview(String previewId,long baseRevision,ScheduleCandidate candidate,String mode) {}
    private final ScheduleStore store;
    private final GreedyScheduler scheduler;
    private final PreviewRepository previews;
    private final JsonCodec json;
    private final Clock clock;
    public PlanningService(ScheduleStore store,GreedyScheduler scheduler,PreviewRepository previews,JsonCodec json,Clock clock) {
        this.store=store;this.scheduler=scheduler;this.previews=previews;this.json=json;this.clock=clock;
    }
    @Transactional
    public Preview preview() {
        store.lock(); var snapshot=store.snapshot(); var candidate=scheduler.schedule(snapshot);
        PreviewEntity e=new PreviewEntity();e.id=UUID.randomUUID().toString();e.userId=ScheduleStore.USER;
        e.baseRevision=snapshot.planningRevision();e.candidateJson=json.write(candidate);e.createdAt=clock.instant();previews.save(e);
        return new Preview(e.id,e.baseRevision,candidate,"mock");
    }
}
