package com.lu4mic.workflow_engine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ProblemDetail handleWorkflowNotFound(WorkflowNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage());
        problem.setTitle("Workflow not found");
        return problem;
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage());
        problem.setTitle("Task not found");
        return problem;
    }

    @ExceptionHandler(InvalidWorkflowDependencyException.class)
    public ProblemDetail handleInvalidWorkflowDependency(InvalidWorkflowDependencyException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Invalid workflow dependency");
        return problem;
    }

    @ExceptionHandler(WorkflowDependencyAlreadyExistsException.class)
    public ProblemDetail handleWorkflowDependencyAlreadyExists(
            WorkflowDependencyAlreadyExistsException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage());
        problem.setTitle("Workflow dependency already exists");
        return problem;
    }

    @ExceptionHandler(InvalidWorkflowTaskException.class)
    public ProblemDetail handleInvalidWorkflowTask(InvalidWorkflowTaskException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
        problem.setTitle("Invalid workflow task");
        return problem;
    }
}
