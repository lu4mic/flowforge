package com.lu4mic.workflow_engine.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    private final TaskRunService taskRunService;
    private final TaskLeaseService taskLeaseService;
    private final WorkerIdentity workerIdentity;
    private final RestClient restClient;

    @Autowired
    public TaskExecutionService(
            TaskRunService taskRunService,
            TaskLeaseService taskLeaseService,
            WorkerIdentity workerIdentity) {
        this.taskRunService = taskRunService;
        this.taskLeaseService = taskLeaseService;
        this.workerIdentity = workerIdentity;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HTTP_TIMEOUT);
        requestFactory.setReadTimeout(HTTP_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    TaskExecutionService(
            TaskRunService taskRunService,
            TaskLeaseService taskLeaseService,
            WorkerIdentity workerIdentity,
            RestClient restClient) {
        this.taskRunService = taskRunService;
        this.taskLeaseService = taskLeaseService;
        this.workerIdentity = workerIdentity;
        this.restClient = restClient;
    }

    public void executeDelayTask(UUID taskRunId) {
        long delayDurationMs = taskRunService.startDelayTaskRun(taskRunId, workerIdentity.getId());

        try {
            Thread.sleep(delayDurationMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            taskRunService.failTaskRun(taskRunId, workerIdentity.getId());
            throw new DelayTaskExecutionException(taskRunId, exception);
        }

        taskRunService.completeTaskRun(taskRunId, workerIdentity.getId());
    }

    public void executeHttpTask(UUID taskRunId) {
        HttpTaskExecutionResponse config = taskRunService.startHttpTaskRun(taskRunId, workerIdentity.getId());
        HttpMethod httpMethod = HttpMethod.valueOf(config.method().name());

        try {
            HttpStatusCode httpStatusCode = restClient
                    .method(httpMethod)
                    .uri(config.url()).exchange((request, response) -> response.getStatusCode());

            if (httpStatusCode.is2xxSuccessful()) {
                taskRunService.completeTaskRun(taskRunId, workerIdentity.getId());
            } else {
                taskRunService.failTaskRun(taskRunId, workerIdentity.getId());
            }
        } catch (ResourceAccessException exception) {
            taskRunService.failTaskRun(taskRunId, workerIdentity.getId());
            throw new HttpTaskExecutionException(
                    taskRunId,
                    "downstream service connection failed or timed out",
                    exception);
        } catch (RestClientException exception) {
            taskRunService.failTaskRun(taskRunId, workerIdentity.getId());
            throw new HttpTaskExecutionException(
                    taskRunId,
                    "unexpected RestClient failure",
                    exception);
        }
    }

    public void executeReadyTaskRuns() {
        List<UUID> claimedTaskRunIds = taskLeaseService.claimEligibleTaskRuns();
        for (UUID taskRunId : claimedTaskRunIds) {
            try {
                executeTaskRun(taskRunId);
            } catch (TaskExecutionException exception) {
                LOGGER.error("TaskRun {} failed during execution", taskRunId, exception);
            } catch (IllegalStateException exception) {
                LOGGER.warn("TaskRun {} could not be started from its claimed state", taskRunId, exception);
            }
        }
    }

    private void executeTaskRun(UUID taskRunId) {
        TaskRun taskRun = taskRunService.findTaskRun(taskRunId);
        switch (taskRun.getWorkflowTask().getTaskType()) {
            case HTTP -> executeHttpTask(taskRunId);
            case DELAY -> executeDelayTask(taskRunId);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void pollReadyTaskRuns() {
        executeReadyTaskRuns();
    }

}
