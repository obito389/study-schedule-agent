export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE" | "CANCELLED";
export interface TimeBlock {
  id: string;
  ownerId: string;
  start: string;
  end: string;
  locked: boolean;
  kind: "COURSE" | "TASK" | "PERSONAL" | "UNAVAILABLE";
}
export interface CourseSnapshot {
  id: string;
  name: string;
  meetingBlocks: TimeBlock[];
}
export interface TaskSnapshot {
  id: string;
  courseId: string | null;
  title: string;
  deadline: string;
  estimatedDurationMinutes: number;
  priority: number;
  status: TaskStatus;
  scheduledBlocks: TimeBlock[];
}
export interface ProfileFact {
  schemaVersion: number;
  key: string;
  value: unknown;
  scope: string;
  strength: number;
  source: string;
  state: "ACTIVE" | "DENIED" | "DELETED";
  evidenceIds: string[];
  version: number;
}
export interface ScheduleSnapshot {
  schemaVersion: number;
  now: string;
  timezone: string;
  courses: CourseSnapshot[];
  tasks: TaskSnapshot[];
  lockedBlocks: TimeBlock[];
  activeFacts: ProfileFact[];
  planningRevision: number;
}
export interface ContractError {
  code: string;
  message: string;
  objectIds: string[];
}
export type ImportStatus =
  "UPLOADED" | "PROCESSING" | "REVIEW" | "COMMITTED" | "FAILED" | "EXPIRED";
export interface ImportProposal {
  schemaVersion: number;
  proposalId: string;
  kind: string;
  title: string;
  courseCandidate: string | null;
  deadline: string | null;
  durationMinutes: number | null;
  sourceEvidence: string[];
  ambiguities: string[];
  status: ImportStatus;
  errors: ContractError[];
}
export interface ImportJob {
  requestKey: string;
  sourceType: string;
  fileName: string;
  status: ImportStatus;
  proposal: ImportProposal | null;
  errors: ContractError[];
  createdAt: string;
  updatedAt: string;
}
export type ActionStatus =
  "NEEDS_CLARIFICATION" | "NEEDS_CONFIRMATION" | "APPLIED" | "FAILED";
export interface ActionSpec {
  schemaVersion: number;
  operationKey: string;
  baseRevision: number;
  type:
    | "QUERY_SCHEDULE"
    | "GENERATE_SCHEDULE"
    | "CREATE_TASK"
    | "MOVE_TASK"
    | "CANCEL_TASK";
  targets: string[];
  changes: Record<string, unknown>;
  authorizationScope: string;
  reasonFacts: string[];
  status: ActionStatus;
  errors: ContractError[];
}
export interface Receipt {
  executionId: string | null;
  operationKey: string;
  type: string;
  status: "APPLIED" | "FAILED";
  planningRevision: number;
  httpStatus: number;
  message: string;
  errors: ContractError[];
  undoable: boolean;
}
export interface Preview {
  previewId: string;
  baseRevision: number;
  mode: string;
  candidate: {
    blocks: { taskId: string; start: string; end: string }[];
    gaps: {
      taskId: string;
      requiredMinutes: number;
      availableMinutes: number;
      reason: string;
    }[];
  };
}
export interface Command {
  operationKey: string;
  baseRevision: number;
  type: string;
  authorizationScope: string;
  payload: Record<string, unknown>;
}
