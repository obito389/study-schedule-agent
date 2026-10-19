package com.studyplanner.agent;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Service;
import com.studyplanner.business.ScheduleStore;
import com.studyplanner.common.ApiException;
@Service
public class LiveAiService {
    public record Status(boolean configured,String provider,String model,String mode) {}
    private final ModelAdapter adapter;
    private final ScheduleStore store;
    private final AiDraftService drafts;
    private final String key;
    private final String model;
    public LiveAiService(@Qualifier("liveModelAdapter") ModelAdapter adapter,ScheduleStore store,AiDraftService drafts,
            @Value("${app.ai.api-key:${DEEPSEEK_API_KEY:}}") String key,@Value("${app.ai.model:deepseek-flash}") String model) {
        this.adapter=adapter;this.store=store;this.drafts=drafts;this.key=key;this.model=model;
    }
    public Status status() {boolean configured=key!=null&&!key.isBlank();return new Status(configured,"DeepSeek",model,configured?"live":"unconfigured");}
    // Deliberately no @Transactional: provider HTTP never holds a DB transaction or row lock.
    public AiPreview preview(String instruction) {
        if(!status().configured())throw new ApiException(503,"AI_NOT_CONFIGURED","后端未配置 DeepSeek 密钥；规则排程仍可用");
        var snapshot=store.snapshot();
        if(snapshot.tasks().size()>100)throw new ApiException(422,"AI_CONTEXT_TOO_LARGE","演示版 AI 当前支持最多 100 个任务的日程上下文");
        long start=System.nanoTime();
        var response=adapter.generate(instruction,snapshot);
        return drafts.prepare(response,snapshot.planningRevision(),model,(System.nanoTime()-start)/1_000_000);
    }
}
