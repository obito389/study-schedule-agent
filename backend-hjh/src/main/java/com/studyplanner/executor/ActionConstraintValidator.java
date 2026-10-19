package com.studyplanner.executor;

import java.util.List;

/**
 * 硬约束校验器。
 * 负责在执行器 preview / apply 之前校验一批动作是否违反排程硬约束。
 *
 * <p>对接说明：熊翔交付的 ActionSpec / 排程硬约束模块到位后，只需提供一个本接口的新实现
 * （配合 {@code @Primary} 或 {@code @ConditionalOnMissingBean}）替换
 * {@link DefaultConstraintValidator}，{@code ActionExecutor} 入口无需任何改动。</p>
 */
public interface ActionConstraintValidator {

    /**
     * 校验一批动作。
     *
     * @param userId  归属用户
     * @param actions 待校验动作
     * @return 违规信息列表，空列表表示全部通过
     */
    List<String> validate(Long userId, List<ActionCommand> actions);
}
