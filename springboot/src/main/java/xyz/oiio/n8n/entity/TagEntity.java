package xyz.oiio.n8n.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags")
public class TagEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Size(max = 36)
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "created_by")
    private String createdBy;

    @ManyToMany(mappedBy = "tags")
    private List<WorkflowEntity> workflows;
}