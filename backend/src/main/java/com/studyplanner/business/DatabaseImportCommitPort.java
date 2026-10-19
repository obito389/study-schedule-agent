package com.studyplanner.business;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.contract.ImportProposal;
import com.studyplanner.importing.ImportCommitPort;
@Component
public class DatabaseImportCommitPort implements ImportCommitPort {
    private final ScheduleStore store;
    private final ActionExecutor executor;
    private final ObjectMapper mapper;
    public DatabaseImportCommitPort(ScheduleStore store,ActionExecutor executor,ObjectMapper mapper) {
        this.store=store;this.executor=executor;this.mapper=mapper;
    }
    @Override @Transactional
    public void commit(ImportProposal proposal) {
        if (!proposal.kind().equals("ASSIGNMENT")) throw new IllegalArgumentException("当前导入确认支持 ASSIGNMENT 任务");
        long revision=store.lock().revision;
        String key="import-"+UUID.nameUUIDFromBytes(proposal.proposalId().getBytes(StandardCharsets.UTF_8));
        var p=mapper.createObjectNode().put("title",proposal.title()).put("deadline",proposal.deadline().toString())
                .put("durationMinutes",proposal.durationMinutes()).put("priority",3);
        // Retain all proposal content in the command fingerprint to reject ID reuse with changed evidence.
        p.set("importProposal",mapper.valueToTree(proposal));
        // A committed receipt must replay even after revision changes. Reuse its original base revision.
        var old=executor.history().stream().filter(r -> r.operationKey().equals(key)).findFirst().orElse(null);
        if (old!=null && old.status().equals("APPLIED")) revision=old.planningRevision()-1;
        var receipt=executor.execute(new ActionCommand(key,revision,"CREATE_TASK","schedule:write:self",p));
        if (receipt.httpStatus()!=200) throw new IllegalStateException(receipt.message());
    }
}
