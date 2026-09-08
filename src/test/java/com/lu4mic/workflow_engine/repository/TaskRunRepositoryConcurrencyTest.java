package com.lu4mic.workflow_engine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.TaskType;
import com.lu4mic.workflow_engine.model.Workflow;
import com.lu4mic.workflow_engine.model.WorkflowRun;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowTask;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:claim-test;MODE=PostgreSQL;NON_KEYWORDS=RUN,KEY;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TaskRunRepositoryConcurrencyTest {
    private static final List<WorkflowRunStatus> EXECUTABLE_WORKFLOW_STATUSES = List.of(
            WorkflowRunStatus.PENDING, WorkflowRunStatus.RUNNING);

    @Autowired private TaskRunRepository taskRunRepository;
    @Autowired private WorkflowRepository workflowRepository;
    @Autowired private WorkflowTaskRepository workflowTaskRepository;
    @Autowired private WorkflowRunRepository workflowRunRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void concurrentWorkersCannotBothClaimTheSameReadyTask() throws Exception {
        UUID taskRunId = createReadyTaskRun(null);
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
        assertTrue(claimed.getLeaseOwner().equals(workerA) || claimed.getLeaseOwner().equals(workerB));
    }

    @Test
    void retryBackoffInTheFutureIsNotDiscoverableOrClaimable() {
        UUID taskRunId = createReadyTaskRun(LocalDateTime.now().plusMinutes(1));
        LocalDateTime now = LocalDateTime.now();

        List<UUID> eligible = taskRunRepository.findEligibleIds(
                TaskRunStatus.READY, EXECUTABLE_WORKFLOW_STATUSES, now, PageRequest.of(0, 10));
        int claimed = claim(taskRunId, UUID.randomUUID(), now);

        assertEquals(List.of(), eligible);
        assertEquals(0, claimed);
        assertNull(taskRunRepository.findById(taskRunId).orElseThrow().getLeaseOwner());
    }

    @Test
    void anotherWorkerCannotRenewTheLease() {
        UUID taskRunId = createReadyTaskRun(null);
        UUID workerA = UUID.randomUUID();
        UUID workerB = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        assertEquals(1, claim(taskRunId, workerA, now));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            TaskRun taskRun = taskRunRepository.findByIdForUpdate(taskRunId).orElseThrow();
            taskRun.start(workerA, now.plusNanos(1));
        });
        LocalDateTime originalExpiry = taskRunRepository.findById(taskRunId).orElseThrow().getLeaseExpiresAt();

        int renewed = new TransactionTemplate(transactionManager).execute(status ->
                taskRunRepository.renewOwnedLeases(
                        workerB, TaskRunStatus.RUNNING, now, now.plusMinutes(1)));

        assertEquals(0, renewed);
        TaskRun unchanged = taskRunRepository.findById(taskRunId).orElseThrow();
        assertEquals(workerA, unchanged.getLeaseOwner());
        assertEquals(originalExpiry, unchanged.getLeaseExpiresAt());
    }

    private int claimAfterBarrier(UUID taskRunId, UUID workerId,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        return claim(taskRunId, workerId, LocalDateTime.now());
    }

    private int claim(UUID taskRunId, UUID workerId, LocalDateTime now) {
        return new TransactionTemplate(transactionManager).execute(status ->
                taskRunRepository.claimIfEligible(
                        taskRunId, workerId, TaskRunStatus.READY,
                        EXECUTABLE_WORKFLOW_STATUSES, now, now.plusSeconds(30)));
    }

    private UUID createReadyTaskRun(LocalDateTime nextAttemptAt) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Workflow workflow = workflowRepository.save(new Workflow("Workflow", "Concurrency test"));
            WorkflowTask task = workflowTaskRepository.save(new WorkflowTask(
                    "task", "Task", TaskType.DELAY, 1L, null, null, workflow));
            WorkflowRun workflowRun = workflowRunRepository.save(new WorkflowRun(workflow));
            TaskRun taskRun = new TaskRun(workflowRun, task);
            taskRun.markReady();
            ReflectionTestUtils.setField(taskRun, "nextAttemptAt", nextAttemptAt);
            return taskRunRepository.saveAndFlush(taskRun).getId();
        });
    }
}
