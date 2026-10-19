import type {
  ActionSpec,
  ProfileFact,
  ScheduleSnapshot
} from '@/types/contract'
import { ActionStatus, ActionType, ProfileFactState, TaskStatus, TimeBlockKind } from '@/types/contract'

/**
 * mock 数据与 samples/contracts/*.json 保持一致，
 * 供后端未就绪时断网演示使用。
 */

export const mockScheduleSnapshot: ScheduleSnapshot = {
  schemaVersion: 1,
  now: '2026-09-13T01:00:00Z',
  timezone: 'Asia/Shanghai',
  courses: [
    {
      id: 'course-ds',
      name: '数据结构',
      meetingBlocks: [
        {
          id: 'course-block-001',
          ownerId: 'course-ds',
          start: '2026-09-14T00:00:00Z',
          end: '2026-09-14T01:30:00Z',
          locked: true,
          kind: TimeBlockKind.COURSE
        }
      ]
    }
  ],
  tasks: [
    {
      id: 'task-001',
      courseId: 'course-ds',
      title: '完成数据结构实验报告',
      deadline: '2026-09-16T12:00:00Z',
      estimatedDurationMinutes: 120,
      priority: 3,
      status: TaskStatus.TODO,
      scheduledBlocks: []
    }
  ],
  lockedBlocks: [],
  activeFacts: [],
  planningRevision: 7
}

export const mockActionSpec: ActionSpec = {
  schemaVersion: 1,
  operationKey: 'op-20260913-001',
  baseRevision: 7,
  type: ActionType.MOVE_TASK,
  targets: ['task-001'],
  changes: {
    start: '2026-09-14T11:00:00Z',
    end: '2026-09-14T13:00:00Z'
  },
  authorizationScope: 'schedule:write:self',
  reasonFacts: ['避开课程固定时间'],
  status: ActionStatus.NEEDS_CONFIRMATION,
  errors: []
}

export const mockProfileFacts: ProfileFact[] = [
  {
    schemaVersion: 1,
    key: 'planning.preferredStudyPeriod',
    value: 'EVENING',
    scope: 'GLOBAL',
    strength: 0.9,
    source: 'USER_EXPLICIT',
    state: ProfileFactState.ACTIVE,
    evidenceIds: ['conversation-20260913-turn-4'],
    version: 3
  },
  {
    schemaVersion: 1,
    key: 'planning.durationEstimate.multiplier',
    value: 1.2,
    scope: 'course-ds',
    strength: 0.6,
    source: 'SYSTEM_LEARNED',
    state: ProfileFactState.ACTIVE,
    evidenceIds: ['task-001', 'task-002'],
    version: 2
  },
  {
    schemaVersion: 1,
    key: 'planning.noStudyOnFriday',
    value: true,
    scope: 'GLOBAL',
    strength: 0.8,
    source: 'USER_EXPLICIT',
    state: ProfileFactState.DENIED,
    evidenceIds: ['conversation-20260910-turn-2'],
    version: 1
  }
]
