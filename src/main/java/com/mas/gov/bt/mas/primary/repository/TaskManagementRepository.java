package com.mas.gov.bt.mas.primary.repository;

import com.mas.gov.bt.mas.primary.entity.TaskManagement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskManagementRepository extends JpaRepository<TaskManagement, Long> {

    List<TaskManagement> findByApplicationNumber(String applicationNumber);

    Page<TaskManagement> findByAssignedToUserId(Long assignedToUserId, Pageable pageable);

    Page<TaskManagement> findByAssignedToUserIdAndTaskStatus(Long assignedToUserId, String taskStatus, Pageable pageable);

    Page<TaskManagement> findByAssignedToRoleAndTaskStatus(String role, String taskStatus, Pageable pageable);

    @Query("SELECT t FROM TaskManagement t WHERE t.assignedToRole = :role AND t.taskStatus = 'PENDING' ORDER BY t.assignedAt ASC")
    Page<TaskManagement> findPendingTasksByRole(@Param("role") String role, Pageable pageable);

    @Query("SELECT t FROM TaskManagement t WHERE t.assignedToUserId = :userId AND t.taskStatus = 'PENDING' ORDER BY t.assignedAt ASC")
    Page<TaskManagement> findPendingTasksByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COUNT(t) FROM TaskManagement t WHERE t.assignedToUserId = :userId AND t.taskStatus = 'PENDING'")
    Long countPendingTasksByUser(@Param("userId") Long userId);

    @Query("SELECT COUNT(t) FROM TaskManagement t WHERE t.assignedToRole = :role AND t.taskStatus = 'PENDING'")
    Long countPendingTasksByRole(@Param("role") String role);

    @Query("SELECT t FROM TaskManagement t WHERE t.taskStatus = 'PENDING' AND t.deadlineDate IS NOT NULL AND t.deadlineDate < :now")
    List<TaskManagement> findOverdueTasks(@Param("now") LocalDateTime now);

    List<TaskManagement> findByApplicationNumberAndTaskStatusAndAssignedToRole(String applicationNumber, String assigned, String geologist);

    List<TaskManagement> findByApplicationNumberAndTaskStatusInAndAssignedToRole(String applicationNumber, List<String> status, String director);

    List<TaskManagement> findByApplicationNumberAndTaskStatusAndAssignedToRoleIn(String applicationNumber, String assigned, List<String> geologist);
}
