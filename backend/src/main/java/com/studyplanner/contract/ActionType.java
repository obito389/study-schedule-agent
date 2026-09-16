package com.studyplanner.contract;

/**
 * Agent 可生成的动作类型。初版只注册查询、排程、创建、移动、取消五类工具，
 * 对应五类动作；用户事实更新预留。
 */
public enum ActionType {
    QUERY_SCHEDULE,
    GENERATE_SCHEDULE,
    CREATE_TASK,
    MOVE_TASK,
    CANCEL_TASK,
    UPDATE_PROFILE
}
