package com.lu4mic.workflow_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskAttemptStatus;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.model.Workflow;
import com.lu4mic.workflow_engine.model.WorkflowDependency;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.repository.TaskAttemptRepository;
import com.lu4mic.workflow_engine.repository.TaskRunRepository;
import com.lu4mic.workflow_engine.repository.WorkflowDependencyRepository;

@ExtendWith(MockitoExtension.class)
class TaskRunServiceTest {
    @Mock
    private WorkflowDependencyRepository dependencyRepository;
    @Mock
    private TaskRunRepository taskRunRepository;
    @Mock
    private TaskAttemptRepository taskAttemptRepository;
    @Mock
    private TaskAttemptService taskAttemptService;
    @InjectMocks
    private TaskRunService service;

    private Workflow workflow;
    private WorkflowRun workflowRun;
    private TaskRun taskRun;
    private TaskAttempt attempt;

    @BeforeEach
    void setUp() {
        workflow = new Workflow("Workflow", "Test workflow");
        workflowRun = new WorkflowRun(workflow);
        ReflectionTestUtils.setField(workflowRun, "id", UUID.randomUUID());
        workflowRun.start();
        taskRun = newTaskRun("first");
        taskRun.markReady();
        taskRun.start();
        attempt = new TaskAttempt(taskRun, 1);
        when(taskRunRepository.findById(taskRun.getId())).thenReturn(Optional.of(taskRun));
    }

    @Test
    void completesAttemptAndWorkflowWhenLastTaskFinishes() {
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));
        when(taskRunRepository.findAllByWorkflowRun_Id(workflowRun.getId())).thenReturn(List.of(taskRun));

        service.completeTaskRun(taskRun.getId());

        assertEquals(TaskRunStatus.SUCCEEDED, taskRun.getStatus());
        assertEquals(TaskAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
        assertEquals(WorkflowRunStatus.SUCCEEDED, workflowRun.getStatus());
    }

    @Test
    void completesAttemptAndUnblocksDependentBeforeWorkflowFinishes() {
        TaskRun dependent = newTaskRun("second");
        WorkflowDependency dependency = new WorkflowDependency(
                taskRun.getWorkflowTask(), dependent.getWorkflowTask());
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));
        when(taskRunRepository.findAllByWorkflowRun_Id(workflowRun.getId()))
                .thenReturn(List.of(taskRun, dependent));
        when(dependencyRepository.findAllByPrerequisiteTask_Id(taskRun.getWorkflowTask().getId()))
                .thenReturn(List.of(dependency));
        when(dependencyRepository.findAllByDependentTask_Id(dependent.getWorkflowTask().getId()))
                .thenReturn(List.of(dependency));
        when(taskRunRepository.findByWorkflowRun_IdAndWorkflowTask_Id(
                workflowRun.getId(), taskRun.getWorkflowTask().getId())).thenReturn(Optional.of(taskRun));
        when(taskRunRepository.findByWorkflowRun_IdAndWorkflowTask_Id(
                workflowRun.getId(), dependent.getWorkflowTask().getId())).thenReturn(Optional.of(dependent));

        service.completeTaskRun(taskRun.getId());

        assertEquals(TaskAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
        assertEquals(TaskRunStatus.SUCCEEDED, taskRun.getStatus());
        assertEquals(TaskRunStatus.READY, dependent.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
    }

    @Test
    void rejectsMissingAttemptBeforeChangingTaskState() {
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.completeTaskRun(taskRun.getId()));

        assertEquals(TaskRunStatus.RUNNING, taskRun.getStatus());
        assertNull(taskRun.getCompletedAt());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
        verifyNoInteractions(dependencyRepository);
    }

    @Test
    void rejectsFailedLatestAttemptBeforeChangingTaskState() {
        attempt.fail();
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));

        assertThrows(IllegalStateException.class, () -> service.completeTaskRun(taskRun.getId()));

        assertEquals(TaskAttemptStatus.FAILED, attempt.getStatus());
        assertEquals(TaskRunStatus.RUNNING, taskRun.getStatus());
        assertNull(taskRun.getCompletedAt());
        verifyNoInteractions(dependencyRepository);
    }

    @Test
    void failureRejectsMissingAttemptWithoutCreatingOne() {
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> service.failTaskRun(taskRun.getId()));

        assertEquals(TaskRunStatus.RUNNING, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
        verifyNoInteractions(taskAttemptService, dependencyRepository);
    }

    @Test
    void failureRejectsCompletedAttemptAndPreservesCompletionTime() {
        attempt.succeed();
        var completedAt = attempt.getCompletedAt();
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));

        assertThrows(IllegalStateException.class, () -> service.failTaskRun(taskRun.getId()));

        assertEquals(TaskAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertEquals(completedAt, attempt.getCompletedAt());
        assertEquals(TaskRunStatus.RUNNING, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
        verifyNoInteractions(taskAttemptService, dependencyRepository);
    }

    @Test
    void failureWithRetriesRemainingReturnsTaskToReady() {
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));

        service.failTaskRun(taskRun.getId());

        assertEquals(TaskAttemptStatus.FAILED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
        assertEquals(TaskRunStatus.READY, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
        verifyNoInteractions(taskAttemptService, dependencyRepository);
    }

    @Test
    void secondFailureAlsoReturnsTaskToReady() {
        attempt = new TaskAttempt(taskRun, 2);
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));

        service.failTaskRun(taskRun.getId());

        assertEquals(TaskAttemptStatus.FAILED, attempt.getStatus());
        assertEquals(TaskRunStatus.READY, taskRun.getStatus());
        assertEquals(WorkflowRunStatus.RUNNING, workflowRun.getStatus());
    }

    @Test
    void thirdFailurePermanentlyFailsTaskAndWorkflow() {
        attempt = new TaskAttempt(taskRun, 3);
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(attempt));

        service.failTaskRun(taskRun.getId());

        assertEquals(TaskAttemptStatus.FAILED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
        assertEquals(TaskRunStatus.FAILED, taskRun.getStatus());
        assertNotNull(taskRun.getCompletedAt());
        assertEquals(WorkflowRunStatus.FAILED, workflowRun.getStatus());
        verifyNoInteractions(taskAttemptService, dependencyRepository);
    }

    private TaskRun newTaskRun(String key) {
        WorkflowTask task = new WorkflowTask(key, key, TaskType.DELAY, 1L, null, null, workflow);
        ReflectionTestUtils.setField(task, "id", UUID.randomUUID());
        TaskRun run = new TaskRun(workflowRun, task);
        ReflectionTestUtils.setField(run, "id", UUID.randomUUID());
        return run;
    }
}
