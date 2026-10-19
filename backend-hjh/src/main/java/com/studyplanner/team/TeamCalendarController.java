package com.studyplanner.team;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.studyplanner.common.ApiResponse;
@RestController
@RequestMapping("/api/v1/workspace/calendar")
public class TeamCalendarController {
    private final TeamCalendarService service;
    public TeamCalendarController(TeamCalendarService service) { this.service=service; }
    @GetMapping public ApiResponse<?> list() { return ApiResponse.success(service.list()); }
    @PostMapping("/courses") public ApiResponse<?> course(@Valid @RequestBody TeamCalendarService.Input input) {
        return ApiResponse.success(service.course(input));
    }
    @PostMapping("/windows") public ApiResponse<?> window(@Valid @RequestBody TeamCalendarService.Input input) {
        return ApiResponse.success(service.window(input));
    }
    @DeleteMapping("/courses/{id}") public ApiResponse<?> removeCourse(@PathVariable Long id,@RequestParam long baseRevision) {
        return ApiResponse.success(service.removeCourse(id,baseRevision));
    }
    @DeleteMapping("/windows/{id}") public ApiResponse<?> removeWindow(@PathVariable Long id,@RequestParam long baseRevision) {
        return ApiResponse.success(service.removeWindow(id,baseRevision));
    }
}
