package com.lu4mic.workflow_engine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskAttemptStatus;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.model.Workflow;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowTask;
import com.lu4mic.workflow_engine.service.TaskAttemptService;
import com.lu4mic.workflow_engine.service.TaskRunService;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TaskRunService.class, TaskAttemptService.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TaskLeasePostgresIntegrationTest {
    private static final List<WorkflowRunStatus> EXECUTABLE_WORKFLOW_STATUSES = List.of(
            WorkflowRunStatus.PENDING, WorkflowRunStatus.RUNNING);

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private TaskRunRepository taskRunRepository;
    @Autowired private TaskAttemptRepository taskAttemptRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private WorkflowRunRepository workflowRunRepository;
    @Autowired private TaskRunService taskRunService;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void twoWorkersRacingTheSameReadyTaskProduceExactlyOneClaim() throws Exception {
        UUID taskRunId = createReadyTaskRun();
        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> claimA = executor.submit(() -> claimAfterBarrier(taskRunId, workerA, ready, start));
            Future<Integer> claimB = executor.submit(() -> claimAfterBarrier(taskRunId, workerB, ready, start));
            ready.await();
            start.countDown();

            assertEquals(1, claimA.get() + claimB.get());
        }

        TaskRun claimed = taskRunRepository.findById(taskRunId).orElseThrow();
        assertEquals(TaskRunStatus.READY, claimed.getStatus());
        assertNotNull(claimed.getLeaseExpiresAt());
        assertTrue(claimed.getLeaseOwner().equals(workerA) || claimed.getLeaseOwner().equals(workerB));
    }

    @Test
    void staleWorkerCannotCompleteAfterItsLeaseExpires() {
        UUID workerA = UUID.randomUUID();
        UUID taskRunId = createAndStartTaskRun(workerA);
        expireLease(taskRunId, LocalDateTime.now().minusSeconds(1));

        assertThrows(IllegalStateException.class,
                () -> taskRunService.completeTaskRun(taskRunId, workerA));

        TaskRun unchanged = taskRunRepository.findById(taskRunId).orElseThrow();
        TaskAttempt attempt = taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(unchanged).orElseThrow();
        assertEquals(TaskRunStatus.RUNNING, unchanged.getStatus());
        assertEquals(TaskAttemptStatus.RUNNING, attempt.getStatus());
    }

    @Test
    void competingRecoveryWorkersRecoverAnExpiredRunningTaskExactlyOnce() throws Exception {
        UUID taskRunId = createAndStartTaskRun(UUID.randomUUID());
        LocalDateTime recoveryTime = LocalDateTime.now();
        expireLease(taskRunId, recoveryTime.minusSeconds(1));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> recoveryA = executor.submit(() -> recoverAfterBarrier(taskRunId, recoveryTime, ready, start));
            Future<?> recoveryB = executor.submit(() -> recoverAfterBarrier(taskRunId, recoveryTime, ready, start));
            ready.await();
            start.countDown();
            recoveryA.get();
            recoveryB.get();
        }

        TaskRun recovered = taskRunRepository.findById(taskRunId).orElseThrow();
        List<TaskAttempt> attempts = taskAttemptRepository.findAllByTaskRun_Id(taskRunId);
        assertEquals(TaskRunStatus.READY, recovered.getStatus());
        assertNull(recovered.getLeaseOwner());
        assertNull(recovered.getLeaseExpiresAt());
        assertNotNull(recovered.getNextAttemptAt());
        assertEquals(1, attempts.size());
        assertEquals(TaskAttemptStatus.FAILED, attempts.getFirst().getStatus());
    }

    private int claimAfterBarrier(UUID taskRunId, UUID workerId,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return claim(taskRunId, workerId, LocalDateTime.now());
    }

    private void recoverAfterBarrier(UUID taskRunId, LocalDateTime recoveryTime,
            CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await();
            taskRunService.recoverExpiredTaskRun(taskRunId, recoveryTime);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private int claim(UUID taskRunId, UUID workerId, LocalDateTime now) {
        return new TransactionTemplate(transactionManager).execute(status ->
                taskRunRepository.claimIfEligible(
                        taskRunId, workerId, TaskRunStatus.READY,
                        EXECUTABLE_WORKFLOW_STATUSES, now, now.plusSeconds(30)));
    }

    private UUID createAndStartTaskRun(UUID workerId) {
        UUID taskRunId = createReadyTaskRun();
        assertEquals(1, claim(taskRunId, workerId, LocalDateTime.now()));
        taskRunService.startDelayTaskRun(taskRunId, workerId);
        return taskRunId;
    }

    private void expireLease(UUID taskRunId, LocalDateTime expiredAt) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            TaskRun taskRun = taskRunRepository.findByIdForUpdate(taskRunId).orElseThrow();
            ReflectionTestUtils.setField(taskRun, "leaseExpiresAt", expiredAt);
        });
    }

    private UUID createReadyTaskRun() {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Workflow workflow = workflowRepository.save(new Workflow("Workflow", "PostgreSQL lease test"));
            WorkflowTask task = workflowTaskRepository.save(new WorkflowTask(
                    "task", "Task", TaskType.DELAY, 1L, null, null, workflow));
            WorkflowRun workflowRun = workflowRunRepository.save(new WorkflowRun(workflow));
            TaskRun taskRun = new TaskRun(workflowRun, task);
            taskRun.markReady();
            return taskRunRepository.saveAndFlush(taskRun).getId();
        });
    }
}
