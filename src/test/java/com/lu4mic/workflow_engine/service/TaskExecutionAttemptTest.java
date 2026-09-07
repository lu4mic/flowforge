package com.lu4mic.workflow_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.lu4mic.workflow_engine.exception.DelayTaskExecutionException;
import com.lu4mic.workflow_engine.exception.HttpTaskExecutionException;
import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskAttemptStatus;
import com.lu4mic.workflow_engine.model.TaskHttpMethod;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.model.Workflow;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.repository.TaskAttemptRepository;
import com.lu4mic.workflow_engine.repository.TaskRunRepository;
import com.lu4mic.workflow_engine.repository.WorkflowDependencyRepository;

@ExtendWith(MockitoExtension.class)
class TaskExecutionAttemptTest {
    private static final String URL = "https://example.test/task";

    @Mock
    private TaskAttemptRepository attemptRepository;
    @Mock
    private TaskRunRepository runRepository;
    @Mock
    private WorkflowDependencyRepository dependencyRepository;

    private TaskExecutionService executionService;
    private MockRestServiceServer server;
    private TaskRun taskRun;
    private final AtomicReference<TaskAttempt> savedAttempt = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        TaskAttemptService attemptService = new TaskAttemptService(attemptRepository);
        TaskRunService runService = new TaskRunService(
                dependencyRepository, runRepository, attemptRepository, attemptService);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        executionService = new TaskExecutionService(runService, builder.build());
    }

    @ParameterizedTest
    @ValueSource(ints = { 200, 204, 400, 500 })
    void httpOutcomeCompletesTheOriginalAttempt(int status) {
        prepareTask(TaskType.HTTP);
        boolean succeeded = status < 300;
        if (succeeded) {
            prepareWorkflowCompletion();
        }
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(status)));

        executionService.executeHttpTask(taskRun.getId());

        assertOutcome(succeeded);
        server.verify();
    }

    @Test
    void httpConnectionFailureFailsTheOriginalAttempt() {
        prepareTask(TaskType.HTTP);
        server.expect(requestTo(URL)).andRespond(withException(new IOException("Connection failed")));

        assertThrows(HttpTaskExecutionException.class,
                () -> executionService.executeHttpTask(taskRun.getId()));

        assertOutcome(false);
        server.verify();
    }

    @Test
    void httpTimeoutFailsAttemptAndLeavesTaskReadyWhenRetryRemains() {
        prepareTask(TaskType.HTTP);
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(HttpTaskExecutionException.class,
                () -> executionService.executeHttpTask(taskRun.getId()));

        assertOutcome(false);
        server.verify();
    }

    @Test
    void httpTimeoutOnThirdAttemptFailsTaskAndWorkflow() {
        prepareTask(TaskType.HTTP);
        TaskAttempt secondAttempt = mock(TaskAttempt.class);
        when(secondAttempt.getAttemptNumber()).thenReturn(2);
        savedAttempt.set(secondAttempt);
        server.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThrows(HttpTaskExecutionException.class,
                () -> executionService.executeHttpTask(taskRun.getId()));

        TaskAttempt thirdAttempt = savedAttempt.get();
        assertEquals(3, thirdAttempt.getAttemptNumber());
        assertEquals(TaskAttemptStatus.FAILED, thirdAttempt.getStatus());
        assertNotNull(thirdAttempt.getCompletedAt());
        assertEquals(TaskRunStatus.FAILED, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.FAILED, taskRun.getWorkflowRun().getStatus());
        server.verify();
    }

    @Test
    void successfulHttpRetryCreatesNextAttemptAndCompletesNormally() {
        prepareTask(TaskType.HTTP);
        prepareWorkflowCompletion();
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(500)));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatusCode.valueOf(200)));

        executionService.executeHttpTask(taskRun.getId());

        TaskAttempt firstAttempt = savedAttempt.get();
        assertEquals(1, firstAttempt.getAttemptNumber());
        assertEquals(TaskAttemptStatus.FAILED, firstAttempt.getStatus());
        assertEquals(TaskRunStatus.READY, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, taskRun.getWorkflowRun().getStatus());

        executionService.executeHttpTask(taskRun.getId());

        TaskAttempt secondAttempt = savedAttempt.get();
        assertEquals(2, secondAttempt.getAttemptNumber());
        assertEquals(TaskAttemptStatus.SUCCEEDED, secondAttempt.getStatus());
        assertNotNull(secondAttempt.getCompletedAt());
        assertEquals(TaskRunStatus.SUCCEEDED, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.SUCCEEDED, taskRun.getWorkflowRun().getStatus());
        verify(attemptRepository, times(2)).save(any(TaskAttempt.class));
        verify(attemptRepository, times(4)).findTopByTaskRunOrderByAttemptNumberDesc(taskRun);
        server.verify();
    }

    @Test
    void successfulDelayCompletesTheOriginalAttempt() {
        prepareTask(TaskType.DELAY);
        prepareWorkflowCompletion();

        executionService.executeDelayTask(taskRun.getId());

        assertOutcome(true);
    }

    @Test
    void interruptedDelayFailsTheOriginalAttemptAndPreservesInterrupt() {
        prepareTask(TaskType.DELAY);
        try {
            Thread.currentThread().interrupt();
            assertThrows(DelayTaskExecutionException.class,
                    () -> executionService.executeDelayTask(taskRun.getId()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertOutcome(false);
    }

    private void prepareTask(TaskType type) {
        Workflow workflow = new Workflow("Workflow", "Execution test");
        WorkflowRun workflowRun = new WorkflowRun(workflow);
        ReflectionTestUtils.setField(workflowRun, "id", UUID.randomUUID());
        WorkflowTask task = new WorkflowTask("task", "Task", type,
                type == TaskType.DELAY ? 1L : null,
                type == TaskType.HTTP ? TaskHttpMethod.GET : null,
                type == TaskType.HTTP ? URL : null, workflow);
        ReflectionTestUtils.setField(task, "id", UUID.randomUUID());
        taskRun = new TaskRun(workflowRun, task);
        ReflectionTestUtils.setField(taskRun, "id", UUID.randomUUID());
        taskRun.markReady();
        when(runRepository.findById(taskRun.getId())).thenReturn(Optional.of(taskRun));
        when(attemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenAnswer(invocation -> Optional.ofNullable(savedAttempt.get()));
        when(attemptRepository.save(any(TaskAttempt.class))).thenAnswer(invocation -> {
            TaskAttempt attempt = invocation.getArgument(0);
            savedAttempt.set(attempt);
            return attempt;
        });
    }

    private void prepareWorkflowCompletion() {
        when(runRepository.findAllByWorkflowRun_Id(taskRun.getWorkflowRun().getId()))
                .thenReturn(List.of(taskRun));
    }

    private void assertOutcome(boolean succeeded) {
        TaskAttempt attempt = savedAttempt.get();
        assertNotNull(attempt);
        assertEquals(1, attempt.getAttemptNumber());
        assertEquals(succeeded ? TaskAttemptStatus.SUCCEEDED : TaskAttemptStatus.FAILED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
        assertEquals(succeeded ? TaskRunStatus.SUCCEEDED : TaskRunStatus.READY, taskRun.getStatus());
        assertEquals(succeeded ? WorkflowRunStatus.SUCCEEDED : WorkflowRunStatus.RUNNING,
                taskRun.getWorkflowRun().getStatus());
        verify(attemptRepository).save(attempt);
        verify(attemptRepository, times(2)).findTopByTaskRunOrderByAttemptNumberDesc(taskRun);
        verifyNoMoreInteractions(attemptRepository);
        if (!succeeded) {
            verifyNoInteractions(dependencyRepository);
        }
    }
}
