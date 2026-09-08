package com.lu4mic.workflow_engine.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.repository.TaskRunRepository;

@Service
public class TaskRecoveryService {
    private static final int RECOVERY_BATCH_SIZE = 10;

    private final TaskRunRepository taskRunRepository;
    private final TaskRunService taskRunService;

    public TaskRecoveryService(TaskRunRepository taskRunRepository, TaskRunService taskRunService) {
        this.taskRunRepository = taskRunRepository;
        this.taskRunService = taskRunService;
    }

    @Scheduled(fixedDelay = 5_000)
    public void recoverExpiredTaskRuns() {
        LocalDateTime now = LocalDateTime.now();
        List<UUID> expiredIds = taskRunRepository.findExpiredLeaseIds(
                TaskRunStatus.RUNNING,
                now,
                PageRequest.of(0, RECOVERY_BATCH_SIZE));

        for (UUID taskRunId : expiredIds) {
            taskRunService.recoverExpiredTaskRun(taskRunId, now);
        }
    }
}
