package com.studyplanner.undo;
import java.util.List;
import com.studyplanner.business.AgentAction;
@FunctionalInterface public interface UndoConstraintGuard {void validate(Long userId,List<AgentAction> actions);}
