package com.lu4mic.workflow_engine.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class TaskExecutionService {
    private final TaskRunService taskRunService;

    public TaskExecutionService(TaskRunService taskRunService) {
        this.taskRunService = taskRunService;
    }

    public void executeDelayTask(UUID taskRunId) {
        long delayDurationMs = taskRunService.startDelayTaskRun(taskRunId);

        try {
            Thread.sleep(delayDurationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            taskRunService.failTaskRun(taskRunId);
            throw new IllegalStateException("DELAY task execution was interrupted", exception);
        }

        taskRunService.completeTaskRun(taskRunId);
    }
    /*
     * taskRunService.startHttpTaskRun(taskRunId)
     * ↓
     * returns HTTP configuration
     * ↓
     * TaskExecutionService performs request
     * ↓
     * success?
     * ├── yes → taskRunService.completeTaskRun(taskRunId)
     * └── no → taskRunService.failTaskRun(taskRunId)
     * 
     * 
     * 
     * TaskRun READY
     * ↓
     * start()
     * ↓
     * RUNNING
     * ↓
     * perform HTTP request
     * ↓
     * 2xx
     * ↓
     * SUCCEEDED
     * ↓
     * unlock dependent tasks
     */

}
