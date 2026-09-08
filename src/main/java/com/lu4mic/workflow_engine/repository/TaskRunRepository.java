package com.lu4mic.workflow_engine.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.lu4mic.workflow_engine.model.TaskRun;
import com.lu4mic.workflow_engine.model.TaskRunStatus;
import com.lu4mic.workflow_engine.model.WorkflowRunStatus;

import jakarta.persistence.LockModeType;

public interface TaskRunRepository extends JpaRepository<TaskRun, UUID> {

    Optional<TaskRun> findByWorkflowRun_IdAndWorkflowTask_Id(
            UUID workflowRunId,
            UUID workflowTaskId);

    List<TaskRun> findAllByWorkflowRun_Id(UUID workflowRunId);

    @Query("""
            select tr.id from TaskRun tr
            where tr.status = :status
              and (tr.nextAttemptAt is null or tr.nextAttemptAt <= :now)
              and (tr.leaseExpiresAt is null or tr.leaseExpiresAt <= :now)
              and tr.workflowRun.status in :workflowStatuses
            order by tr.readyAt, tr.createdAt
            """)
    List<UUID> findEligibleIds(
            @Param("status") TaskRunStatus status,
            @Param("workflowStatuses") Collection<WorkflowRunStatus> workflowStatuses,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TaskRun tr
               set tr.leaseOwner = :workerId, tr.leaseExpiresAt = :leaseExpiresAt
             where tr.id = :taskRunId
               and tr.status = :status
               and (tr.nextAttemptAt is null or tr.nextAttemptAt <= :now)
               and (tr.leaseExpiresAt is null or tr.leaseExpiresAt <= :now)
               and tr.workflowRun.id in (
                    select wr.id from WorkflowRun wr where wr.status in :workflowStatuses
               )
            """)
    int claimIfEligible(
            @Param("taskRunId") UUID taskRunId,
            @Param("workerId") UUID workerId,
            @Param("status") TaskRunStatus status,
            @Param("workflowStatuses") Collection<WorkflowRunStatus> workflowStatuses,
            @Param("now") LocalDateTime now,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TaskRun tr
               set tr.leaseExpiresAt = :leaseExpiresAt
             where tr.status = :status
               and tr.leaseOwner = :workerId
               and tr.leaseExpiresAt > :now
            """)
    int renewOwnedLeases(
            @Param("workerId") UUID workerId,
            @Param("status") TaskRunStatus status,
            @Param("now") LocalDateTime now,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt);

    @Query("""
            select tr.id from TaskRun tr
             where tr.status = :status
               and tr.leaseExpiresAt is not null
               and tr.leaseExpiresAt <= :now
             order by tr.leaseExpiresAt
            """)
    List<UUID> findExpiredLeaseIds(
            @Param("status") TaskRunStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tr from TaskRun tr where tr.id = :taskRunId")
    Optional<TaskRun> findByIdForUpdate(@Param("taskRunId") UUID taskRunId);
}
