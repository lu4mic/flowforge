package com.lu4mic.workflow_engine.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

class TaskAttemptTest {

    @Test
    void newAttemptStartsRunning() {
        TaskRun taskRun = taskRun();

        TaskAttempt attempt = new TaskAttempt(taskRun, 1);

        assertEquals(taskRun, attempt.getTaskRun());
        assertEquals(1, attempt.getAttemptNumber());
        assertEquals(TaskAttemptStatus.RUNNING, attempt.getStatus());
        assertNotNull(attempt.getCreatedAt());
        assertNotNull(attempt.getStartedAt());
        assertNull(attempt.getCompletedAt());
    }

    @Test
    void taskRunAndAttemptNumberAreRequired() {
        assertThrows(NullPointerException.class, () -> new TaskAttempt(null, 1));
        assertThrows(NullPointerException.class, () -> new TaskAttempt(taskRun(), null));
    }

    @Test
    void attemptNumberMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new TaskAttempt(taskRun(), 0));
        assertThrows(IllegalArgumentException.class, () -> new TaskAttempt(taskRun(), -3));
    }

    @Test
    void succeedCompletesRunningAttempt() {
        TaskAttempt attempt = new TaskAttempt(taskRun(), 1);

        attempt.succeed();

        assertEquals(TaskAttemptStatus.SUCCEEDED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
    }

    @Test
    void failCompletesRunningAttempt() {
        TaskAttempt attempt = new TaskAttempt(taskRun(), 1);

        attempt.fail();

        assertEquals(TaskAttemptStatus.FAILED, attempt.getStatus());
        assertNotNull(attempt.getCompletedAt());
    }

    @Test
    void completedAttemptCannotTransitionAgain() {
        TaskAttempt succeeded = new TaskAttempt(taskRun(), 1);
        TaskAttempt failed = new TaskAttempt(taskRun(), 2);
        succeeded.succeed();
        failed.fail();

        assertThrows(IllegalStateException.class, succeeded::succeed);
        assertThrows(IllegalStateException.class, succeeded::fail);
        assertThrows(IllegalStateException.class, failed::succeed);
        assertThrows(IllegalStateException.class, failed::fail);
    }

    @Test
    void persistenceMetadataEnforcesAttemptRules() throws NoSuchFieldException {
        Table table = TaskAttempt.class.getAnnotation(Table.class);

        assertEquals("task_attempts", table.name());
        assertEquals(1, table.uniqueConstraints().length);
        assertArrayEquals(
                new String[] { "task_run_id", "attempt_number" },
                table.uniqueConstraints()[0].columnNames());

        ManyToOne relationship = TaskAttempt.class.getDeclaredField("taskRun").getAnnotation(ManyToOne.class);
        JoinColumn taskRunColumn = TaskAttempt.class.getDeclaredField("taskRun").getAnnotation(JoinColumn.class);
        Column attemptNumberColumn = TaskAttempt.class.getDeclaredField("attemptNumber").getAnnotation(Column.class);
        Column statusColumn = TaskAttempt.class.getDeclaredField("status").getAnnotation(Column.class);
        Column createdAtColumn = TaskAttempt.class.getDeclaredField("createdAt").getAnnotation(Column.class);
        Column startedAtColumn = TaskAttempt.class.getDeclaredField("startedAt").getAnnotation(Column.class);
        Column completedAtColumn = TaskAttempt.class.getDeclaredField("completedAt").getAnnotation(Column.class);

        assertFalse(relationship.optional());
        assertFalse(taskRunColumn.nullable());
        assertFalse(attemptNumberColumn.nullable());
        assertFalse(statusColumn.nullable());
        assertFalse(createdAtColumn.nullable());
        assertFalse(startedAtColumn.nullable());
        assertTrue(completedAtColumn.nullable());
    }

    private TaskRun taskRun() {
        return new TaskRun(null, null);
    }
}
