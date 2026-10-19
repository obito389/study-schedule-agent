package com.studyplanner.web;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.agent.*;
import com.studyplanner.business.ExecutionReceipt;
@RestController @RequestMapping("/api/v1/ai")
public class AiController {
    private final LiveAiService ai;
    private final AiDraftService drafts;
    public AiController(LiveAiService ai,AiDraftService drafts){this.ai=ai;this.drafts=drafts;}
    public record Input(@NotBlank @Size(max=1500) String instruction) {}
    @GetMapping("/status") public LiveAiService.Status status(){return ai.status();}
    @PostMapping("/previews") public AiPreview preview(@Valid @RequestBody Input input){return ai.preview(input.instruction());}
    @PostMapping("/drafts/{id}/confirm") public ResponseEntity<ExecutionReceipt> confirm(@PathVariable String id){
        var receipt=drafts.confirm(id);return ResponseEntity.status(receipt.httpStatus()).body(receipt);
    }
}
