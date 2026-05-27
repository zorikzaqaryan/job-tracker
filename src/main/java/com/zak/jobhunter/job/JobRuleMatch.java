package com.zak.jobhunter.job;

import com.zak.jobhunter.filter.FilterRule;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "job_rule_matches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobRuleMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobPost job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id")
    private FilterRule rule;

    @Column(name = "matched_field", nullable = false, length = 50)
    private String matchedField;

    @Column(name = "matched_text", columnDefinition = "TEXT")
    private String matchedText;

    @Column(nullable = false)
    private int weight;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
