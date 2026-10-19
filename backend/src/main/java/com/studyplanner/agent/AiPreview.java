package com.studyplanner.agent;
import java.util.List;
import com.studyplanner.contract.ActionSpec;
import com.studyplanner.planning.ScheduleCandidate;
public record AiPreview(String draftId,String status,String provider,String model,long baseRevision,
        long elapsedMillis,List<String> explanations,List<String> clarifications,ActionSpec action,
        ScheduleCandidate candidate) {}
