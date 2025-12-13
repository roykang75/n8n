package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "projects")
public class Project extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @Builder.Default
    private ProjectType type = ProjectType.PERSONAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "icon", columnDefinition = "JSON")
    private ProjectIcon icon;

    @Size(max = 512)
    @Column(name = "description")
    private String description;

    @Column(name = "home_workflow_id")
    private String homeWorkflowId;

    @Column(name = "creator_id")
    private String creatorId;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRelation> relations;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowEntity> workflows;

    public enum ProjectType {
        PERSONAL,
        TEAM
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProjectIcon {
        private String type;
        private String value;
    }
}