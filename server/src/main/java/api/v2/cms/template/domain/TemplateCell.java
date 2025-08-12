package api.v2.cms.template.domain;

import lombok.*;
import api.v2.cms.template.converter.SpanJsonConverter;

import javax.persistence.*;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "template_cells")
public class TemplateCell {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cellId;

    @Column(nullable = false)
    private int ordinal;

    @Convert(converter = SpanJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Integer> span;

    private Long widgetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "row_id", nullable = false)
    private TemplateRow row;
}