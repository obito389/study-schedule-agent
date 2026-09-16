package com.studyplanner.agent;

import com.studyplanner.contract.ActionType;
import com.studyplanner.contract.ScheduleSnapshot;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 确定性 Mock 模型：按简单规则解析指令。用于断网或未配置模型密钥时的
 * 演示与单元测试；真实模型接入时替换实现即可。
 */
public class MockModelAdapter implements ModelAdapter {

    private static final Pattern MOVE_PATTERN = Pattern.compile("移动任务\\s+(\\S+)\\s+到\\s+(.+)");

    @Override
    public Decision decide(String instruction, ScheduleSnapshot snapshot) {
        if (instruction == null || instruction.isBlank()) {
            return Decision.clarify("请描述你想做的事情，例如「创建任务 完成实验报告」");
        }
        if (instruction.contains("？") || instruction.contains("?")) {
            return Decision.clarify("信息不足，请补充任务内容、目标时间或任务 id");
        }
        if (instruction.contains("查询")) {
            return Decision.action(ActionType.QUERY_SCHEDULE, List.of(), Map.of());
        }
        if (instruction.contains("排程")) {
            return Decision.action(ActionType.GENERATE_SCHEDULE, List.of(), Map.of());
        }
        if (instruction.contains("创建任务")) {
            String title = instruction.replace("创建任务", "").trim();
            if (title.isBlank()) {
                return Decision.clarify("请提供任务标题，例如「创建任务 完成实验报告」");
            }
            return Decision.action(ActionType.CREATE_TASK, List.of(), Map.of("title", title));
        }
        if (instruction.contains("移动任务")) {
            Matcher m = MOVE_PATTERN.matcher(instruction);
            if (!m.matches()) {
                return Decision.clarify("请按「移动任务 任务id 到 目标时间」描述");
            }
            String id = m.group(1);
            if (snapshot.tasks().stream().noneMatch(t -> t.id().equals(id))) {
                return Decision.clarify("任务 " + id + " 不存在");
            }
            return Decision.action(ActionType.MOVE_TASK, List.of(id), Map.of("to", m.group(2)));
        }
        if (instruction.contains("取消任务")) {
            String id = instruction.replace("取消任务", "").trim();
            if (id.isBlank()) {
                return Decision.clarify("请提供要取消的任务 id");
            }
            if (snapshot.tasks().stream().noneMatch(t -> t.id().equals(id))) {
                return Decision.clarify("任务 " + id + " 不存在");
            }
            return Decision.action(ActionType.CANCEL_TASK, List.of(id), Map.of());
        }
        return Decision.clarify("无法理解该指令。支持：查询日程、生成排程、创建任务、移动任务、取消任务");
    }
}
