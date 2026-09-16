package com.studyplanner.agent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Agent 工具注册表。初版只注册五类工具：查询日程、生成排程、创建任务、
 * 移动任务、取消任务。
 */
public class AgentToolRegistry {

    private final Map<String, String> tools = new LinkedHashMap<>();

    public AgentToolRegistry() {
        register("query_schedule", "查询当前日程");
        register("generate_schedule", "生成排程");
        register("create_task", "创建任务");
        register("move_task", "移动任务");
        register("cancel_task", "取消任务");
    }

    public void register(String key, String description) {
        tools.put(key, description);
    }

    public Set<String> toolKeys() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    public String describe(String key) {
        return tools.get(key);
    }

    public int size() {
        return tools.size();
    }
}
