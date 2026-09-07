package com.lu4mic.workflow_engine.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.lu4mic.workflow_engine.model.TaskAttempt;
import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.repository.TaskAttemptRepository;

import jakarta.transaction.Transactional;

@Service
public class TaskAttemptService {
    private final TaskAttemptRepository taskAttemptRepository;

    public TaskAttemptService(TaskAttemptRepository taskAttemptRepository) {
        this.taskAttemptRepository = taskAttemptRepository;
    }

    @Transactional
    public TaskAttempt createTaskAttempt(TaskRun taskRun) {
        Objects.requireNonNull(taskRun, "taskRun is required");

        int nextAttemptNumber = taskAttemptRepository
                .findTopByTaskRunOrderByAttemptNumberDesc(taskRun)
                .map(previousAttempt -> previousAttempt.getAttemptNumber() + 1)
                .orElse(1);

        TaskAttempt taskAttempt = new TaskAttempt(taskRun, nextAttemptNumber);
        return taskAttemptRepository.save(taskAttempt);
    }
}
