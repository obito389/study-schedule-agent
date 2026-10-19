package com.studyplanner.team;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.common.ApiResponse;
@RestController @RequestMapping("/api/v1/profile") public class TeamProfileController {
 private final TeamProfileService service;public TeamProfileController(TeamProfileService service){this.service=service;}
 @GetMapping("/facts") public ApiResponse<?> list(){return ApiResponse.success(service.latest());}
 public record Change(JsonNode value,@NotNull @PositiveOrZero Long baseRevision,Long version){}
 @PutMapping("/facts/{key}")public ApiResponse<?> set(@PathVariable String key,@Valid @RequestBody Change body){return ApiResponse.success(service.change(key,body.value(),"SET",body.baseRevision(),body.version()));}
 @PostMapping("/facts/{key}/deny")public ApiResponse<?> deny(@PathVariable String key,@Valid @RequestBody Change body){return ApiResponse.success(service.change(key,null,"DENY",body.baseRevision(),body.version()));}
 @DeleteMapping("/facts/{key}")public ApiResponse<?> delete(@PathVariable String key,@RequestParam long baseRevision,@RequestParam(required=false)Long version){return ApiResponse.success(service.change(key,null,"DELETE",baseRevision,version));}
 @GetMapping("/duration-estimate")public ApiResponse<?> estimate(@RequestParam(defaultValue="60")int minutes){if(minutes<1||minutes>10080)throw new IllegalArgumentException("预计耗时范围无效");return ApiResponse.success(service.estimate(minutes));}
}
