package com.studyplanner.business;
import com.fasterxml.jackson.databind.JsonNode;
public record ActionCommand(String operationKey, long baseRevision, String type, String authorizationScope, JsonNode payload) {
    public ActionCommand {
        if (operationKey==null || operationKey.isBlank() || operationKey.length()>160 || baseRevision<0
                || type==null || authorizationScope==null || payload==null || !payload.isObject())
            throw new IllegalArgumentException("动作参数或幂等键无效");
    }
}
