package xyz.oiio.n8n.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "workflows")
public class WorkflowEntity extends BaseTimeEntity {

    @Id
    @GenericGenerator(name = "nanoid", strategy = "xyz.oiio.n8n.util.NanoIdGenerator")
    @GeneratedValue(generator = "nanoid")
    @Column(name = "id", columnDefinition = "VARCHAR(21) NOT NULL")
    private String id;

    @Version
    @Column(name = "entity_version")
    @lombok.Builder.Default
    private Long entityVersion = 0L;

    @Size(max = 128)
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active")
    @lombok.Builder.Default
    private Boolean active = false;

    @Column(name = "is_archived")
    @lombok.Builder.Default
    private Boolean isArchived = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "nodes", columnDefinition = "JSON")
    private List<Map<String, Object>> nodes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "connections", columnDefinition = "JSON")
    private Map<String, Object> connections;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "JSON")
    private WorkflowSettings settings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "static_data", columnDefinition = "JSON")
    private Map<String, Object> staticData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta", columnDefinition = "JSON")
    private WorkflowMeta meta;

    @Column(name = "version_id", length = 36)
    private String versionId;

    @Column(name = "active_version_id", length = 36)
    private String activeVersionId;

    @Column(name = "version_counter")
    private Integer versionCounter = 0;

    @Column(name = "trigger_count")
    private Integer triggerCount = 0;

    @Column(name = "parent_folder_id")
    private String parentFolderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pin_data", columnDefinition = "JSON")
    private Map<String, Object> pinData;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id", referencedColumnName = "id", nullable = true)
    private User owner;

    @Column(name = "project_id", columnDefinition = "BIGINT")
    private Long projectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Project project;

    @ManyToMany
    @JoinTable(name = "workflow_tags", joinColumns = @JoinColumn(name = "workflow_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<TagEntity> tags;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ExecutionEntity> executions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowSettings {
        private Boolean saveManualExecutions = true;
        private Boolean saveErrorWorkflow = true;
        private Boolean callerPolicyDefaultOption;
        private Boolean errorWorkflowDefaultOption;
        private String timezone = "UTC";
        private String executionOrder = "queue";
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowMeta {
        private String templateId;
        private String templateCreds;
        private String instanceId;
    }
}