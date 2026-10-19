package com.studyplanner.team;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.studyplanner.agent.ModelAdapter;
import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ImportProposal;
import com.studyplanner.contract.ImportStatus;
import com.studyplanner.importing.*;

/** Bridges Zhao Huan's review workflow to the model without committing its output. */
@Component
@Primary
public class TeamImportModelPort implements ImportModelPort {
    private final ModelAdapter model;
    private final TeamSnapshotService snapshots;
    private final boolean configured;

    public TeamImportModelPort(ModelAdapter model, TeamSnapshotService snapshots, @Value("${team.ai.api-key:}") String key) {
        this.model = model;
        this.snapshots = snapshots;
        this.configured = key != null && !key.isBlank();
    }

    @Override
    public ImportProposal propose(ImportSource source) {
        if (source.sourceType() != ImportSourceType.TEXT || !configured) {
            return new ImportProposal(1, "proposal-" + source.requestKey(), "ASSIGNMENT",
                    source.sourceType() == ImportSourceType.SCREENSHOT ? "截图导入待核对" : source.sourceType() == ImportSourceType.AUDIO ? "录音导入待核对" : "文字导入待核对",
                    null, null, null, List.of(source.fileName()),
                    List.of("此来源当前需要人工填写任务名称、截止时间和耗时"), ImportStatus.REVIEW, List.of());
        }
        if (source.contentLength() > 12000) throw new IllegalArgumentException("文字内容过长");
        var result = model.generate("将以下文字提取为一个 CREATE_TASK 待校对任务，不排时间；信息不全则列出歧义：\n"
                + new String(source.content(), StandardCharsets.UTF_8), snapshots.snapshot());
        var fields = result.changes();
        boolean create = result.type() == ActionType.CREATE_TASK;
        return new ImportProposal(1, "proposal-" + source.requestKey(), "ASSIGNMENT",
                create && fields.containsKey("title") ? fields.get("title").asText() : "文字导入待核对",
                null,
                create && fields.containsKey("deadline") && !fields.get("deadline").isNull()
                        ? Instant.parse(fields.get("deadline").asText()) : null,
                create && fields.containsKey("durationMinutes") ? fields.get("durationMinutes").asInt() : null,
                List.of(source.fileName()), result.ambiguities(), ImportStatus.REVIEW, List.of());
    }
}
