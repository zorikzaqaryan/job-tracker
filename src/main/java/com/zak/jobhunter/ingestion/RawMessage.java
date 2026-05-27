package com.zak.jobhunter.ingestion;

import com.zak.jobhunter.channel.JobSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "raw_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private JobSource source;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_name")
    private String sourceName;

    @Column(name = "source_channel_id", length = 100)
    private String sourceChannelId;

    @Column(name = "external_message_id", nullable = false)
    private String externalMessageId;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Builder.Default
    @Column(nullable = false)
    private boolean processed = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
