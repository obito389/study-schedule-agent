package com.studyplanner.business;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.*;
import com.studyplanner.common.ApiException;
import com.studyplanner.contract.*;

/** Backward-compatible API facade. All writes and revisions belong to ActionExecutor. */
@Service
public class CalendarService {
    private final ScheduleStore store;
    private final ActionExecutor executor;
    private final ObjectMapper mapper;
    public CalendarService(ScheduleStore store,ActionExecutor executor,ObjectMapper mapper) {
        this.store=store;this.executor=executor;this.mapper=mapper;
    }
    public ScheduleSnapshot addCourse(String name,Instant start,Instant end,long baseRevision) {
        execute("CREATE_COURSE",baseRevision,mapper.createObjectNode().put("name",name).put("start",start.toString()).put("end",end.toString()));
        return store.snapshot();
    }
    public ScheduleSnapshot addBlock(String title,Instant start,Instant end,boolean unavailable,long baseRevision) {
        execute("CREATE_FIXED_BLOCK",baseRevision,mapper.createObjectNode().put("title",title).put("start",start.toString()).put("end",end.toString()).put("unavailable",unavailable));
        return store.snapshot();
    }
    public ScheduleSnapshot deleteCourse(String id,long baseRevision) {
        execute("DELETE_COURSE",baseRevision,mapper.createObjectNode().put("courseId",id));return store.snapshot();
    }
    public ScheduleSnapshot deleteBlock(String id,long baseRevision) {
        execute("DELETE_FIXED_BLOCK",baseRevision,mapper.createObjectNode().put("blockId",id));return store.snapshot();
    }
    public List<ProfileFact> preference(String key,JsonNode value,String action,long baseRevision) {
        var payload=mapper.createObjectNode().put("key",key).put("action",action);payload.set("value",value);
        execute("SET_PREFERENCE",baseRevision,payload);return store.profile().history();
    }
    private void execute(String type,long revision,JsonNode payload) {
        var receipt=executor.execute(new ActionCommand(UUID.randomUUID().toString(),revision,type,"schedule:write:self",payload));
        if(receipt.httpStatus()!=200)throw new ApiException(receipt.httpStatus(),receipt.errors().getFirst().code(),receipt.message());
    }
}
