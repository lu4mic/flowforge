package com.lu4mic.workflow_engine.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

import com.lu4mic.workflow_engine.dto.HttpTaskExecutionResponse;
import com.lu4mic.workflow_engine.exception.DelayTaskExecutionException;
import com.lu4mic.workflow_engine.exception.HttpTaskExecutionException;
import com.lu4mic.workflow_engine.exception.TaskExecutionException;
import com.lu4mic.workflow_engine.model.TaskRun;

@Service
public class TaskExecutionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutionService.class);

    private final TaskRunService taskRunService;
    private final RestClient restClient;

    public TaskExecutionService(TaskRunService taskRunService, RestClient.Builder restClientBuilder) {
        this.taskRunService = taskRunService;
        this.restClient = restClientBuilder.build();
    }

    public void executeDelayTask(UUID taskRunId) {
        long delayDurationMs = taskRunService.startDelayTaskRun(taskRunId);

        try {
            Thread.sleep(delayDurationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            taskRunService.failTaskRun(taskRunId);
            throw new DelayTaskExecutionException(taskRunId, exception);
        }

        taskRunService.completeTaskRun(taskRunId);
    }

    public void executeHttpTask(UUID taskRunId) {
        HttpTaskExecutionResponse config = taskRunService.startHttpTaskRun(taskRunId);
        HttpMethod httpMethod = HttpMethod.valueOf(config.method().name());

        try {
            HttpStatusCode httpStatusCode = restClient
                    .method(httpMethod)
                    .uri(config.url()).exchange((request, response) -> response.getStatusCode());

            if (httpStatusCode.is2xxSuccessful()) {
                taskRunService.completeTaskRun(taskRunId);
            } else {
                taskRunService.failTaskRun(taskRunId);
            }
        } catch (ResourceAccessException exception) {
            taskRunService.failTaskRun(taskRunId);
            throw new HttpTaskExecutionException(
                    taskRunId,
                    "could not connect to the downstream service",
                    exception);
        } catch (RestClientException exception) {
            taskRunService.failTaskRun(taskRunId);
            throw new HttpTaskExecutionException(
                    taskRunId,
                    "unexpected RestClient failure",
                    exception);
        }
    }

    public void executeReadyTaskRuns() {
        List<TaskRun> tasks = taskRunService.findReadyTaskRuns();
        for (TaskRun taskRun : tasks) {
            try {
                executeTaskRun(taskRun);
            } catch (TaskExecutionException exception) {
                LOGGER.error("TaskRun {} failed during execution", taskRun.getId(), exception);
            }
        }
    }

    private void executeTaskRun(TaskRun taskRun) {
        switch (taskRun.getWorkflowTask().getTaskType()) {
            case HTTP -> executeHttpTask(taskRun.getId());
            case DELAY -> executeDelayTask(taskRun.getId());
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void pollReadyTaskRuns() {
        executeReadyTaskRuns();
    }

}
