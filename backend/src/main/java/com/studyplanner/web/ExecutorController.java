package com.studyplanner.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyplanner.business.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Atomic batch adapter on the stable String-ID/Instant contracts. */
@RestController @RequestMapping("/api/v1/executor")
public class ExecutorController {
    public record Item(@NotBlank String type,@NotNull JsonNode payload) {}
    public record Batch(@NotBlank @Size(max=160) String operationKey,@PositiveOrZero long baseRevision,
                        @NotEmpty @Size(max=50) List<@Valid Item> actions) {}
    private final ActionExecutor executor;
    private final ObjectMapper mapper;
    public ExecutorController(ActionExecutor executor,ObjectMapper mapper) {this.executor=executor;this.mapper=mapper;}
    private ActionCommand command(Batch batch) {
        var payload=mapper.createObjectNode();payload.set("actions",mapper.valueToTree(batch.actions()));
        return new ActionCommand(batch.operationKey(),batch.baseRevision(),"BATCH_TASKS","schedule:write:self",payload);
    }
    @PostMapping("/preview") public ActionExecutor.Preview preview(@Valid @RequestBody Batch batch) {return executor.preview(command(batch));}
    @PostMapping("/apply") public ResponseEntity<ExecutionReceipt> apply(@Valid @RequestBody Batch batch) {
        var receipt=executor.execute(command(batch));return ResponseEntity.status(receipt.httpStatus()).body(receipt);
    }
}
