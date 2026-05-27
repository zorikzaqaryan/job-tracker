package com.zak.jobhunter.ai;

import com.zak.jobhunter.job.JobPost;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "job_ai_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobAiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private JobPost job;

    @Column(nullable = false, length = 100)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private boolean relevant;

    @Column(name = "qualification_score", nullable = false)
    private int qualificationScore;

    @Column(name = "remote_compatible")
    private Boolean remoteCompatible;

    @Column(name = "location_compatible")
    private Boolean locationCompatible;

    @Column(name = "seniority_compatible")
    private Boolean seniorityCompatible;

    /** Comma-separated list of matched skills. */
    @Column(name = "matched_skills", columnDefinition = "TEXT")
    private String matchedSkills;

    /** Comma-separated list of missing important skills. */
    @Column(name = "missing_important_skills", columnDefinition = "TEXT")
    private String missingImportantSkills;

    /** Comma-separated list of risk flags. */
    @Column(name = "risk_flags", columnDefinition = "TEXT")
    private String riskFlags;

    @Column(nullable = false, length = 50)
    private String decision;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_request", columnDefinition = "jsonb")
    private String rawRequest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "jsonb")
    private String rawResponse;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
