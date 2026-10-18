package com.studyplanner.business;
import java.util.List;
import com.studyplanner.contract.ContractError;
public record ExecutionReceipt(String executionId, String operationKey, String type, String status,
        long planningRevision, int httpStatus, String message, List<ContractError> errors, boolean undoable) {}
