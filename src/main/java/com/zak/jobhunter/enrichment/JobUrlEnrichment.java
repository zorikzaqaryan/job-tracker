package com.zak.jobhunter.enrichment;

import com.zak.jobhunter.job.JobPost;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "job_url_enrichments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobUrlEnrichment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private JobPost job;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "fetch_status", nullable = false, length = 50)
    private String fetchStatus;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "extracted_title", columnDefinition = "TEXT")
    private String extractedTitle;

    @Column(name = "extracted_description", columnDefinition = "TEXT")
    private String extractedDescription;

    @Column(name = "extracted_apply_url", columnDefinition = "TEXT")
    private String extractedApplyUrl;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
