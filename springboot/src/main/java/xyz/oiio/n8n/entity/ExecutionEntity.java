package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "executions")
public class ExecutionEntity extends BaseTimeEntity {

    @Id
    @Column(name = "id", columnDefinition = "VARCHAR(36) NOT NULL")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private ExecutionMode mode = ExecutionMode.WEBHOOK;

    @Column(name = "retry_of")
    private String retryOf;

    @Column(name = "retry_success_id")
    private String retrySuccessId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExecutionStatus status = ExecutionStatus.WAITING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", referencedColumnName = "id")
    private WorkflowEntity workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Column(name = "wait_till")
    private LocalDateTime waitTill;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "retry_of_execution_id")
    private String retryOfExecutionId;

    @Column(name = "workflow_data")
    private String workflowData;

    @Column(name = "data", columnDefinition = "JSON")
    private String data;

    @Column(name = "workflow_id_path")
    private String workflowIdPath;

    public enum ExecutionMode {
        MANUAL,
        WEBHOOK,
        RETRY,
        CLI,
        TRIGGER,
        STATIC_TRIGGER
    }

    public enum ExecutionStatus {
        NEW,
        WAITING,
        PREPARING,
        RUNNING,
        SUCCESS,
        ERROR,
        CANCELED,
        CRASHED,
        UNKNOWN,
        KILLED,
        UNEXPECTED
    }
}