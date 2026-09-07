package com.lu4mic.workflow_engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskAttemptStatus;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.repository.TaskAttemptRepository;

@ExtendWith(MockitoExtension.class)
class TaskAttemptServiceTest {

    @Mock
    private TaskAttemptRepository taskAttemptRepository;

    @InjectMocks
    private TaskAttemptService taskAttemptService;

    @Test
    void createsFirstAttemptWithNumberOne() {
        TaskRun taskRun = taskRun();
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.empty());
        when(taskAttemptRepository.save(any(TaskAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskAttempt createdAttempt = taskAttemptService.createTaskAttempt(taskRun);

        assertSame(taskRun, createdAttempt.getTaskRun());
        assertEquals(1, createdAttempt.getAttemptNumber());
        assertEquals(TaskAttemptStatus.RUNNING, createdAttempt.getStatus());
        verify(taskAttemptRepository).save(createdAttempt);
    }

    @Test
    void incrementsHighestExistingAttemptNumber() {
        TaskRun taskRun = taskRun();
        TaskAttempt previousAttempt = new TaskAttempt(taskRun, 2);
        when(taskAttemptRepository.findTopByTaskRunOrderByAttemptNumberDesc(taskRun))
                .thenReturn(Optional.of(previousAttempt));
        when(taskAttemptRepository.save(any(TaskAttempt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TaskAttempt createdAttempt = taskAttemptService.createTaskAttempt(taskRun);

        assertEquals(3, createdAttempt.getAttemptNumber());
        verify(taskAttemptRepository).save(createdAttempt);
    }

    @Test
    void rejectsMissingTaskRunBeforeQueryingRepository() {
        assertThrows(NullPointerException.class, () -> taskAttemptService.createTaskAttempt(null));
        verifyNoInteractions(taskAttemptRepository);
    }

    private TaskRun taskRun() {
        return new TaskRun(null, null);
    }
}
