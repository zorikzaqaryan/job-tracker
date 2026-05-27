package com.zak.jobhunter.channel;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "job_sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    private SourceType sourceType;

    @Column(nullable = false)
    private String name;

    @Column(name = "telegram_username")
    private String telegramUsername;

    @Column(name = "telegram_channel_id", length = 100)
    private String telegramChannelId;

    @Column(columnDefinition = "TEXT")
    private String url;

    /** Link to open the channel in Telegram (invite, @username, or t.me/c/...). */
    @Column(name = "telegram_channel_url", columnDefinition = "TEXT")
    private String telegramChannelUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "last_external_message_id")
    private String lastExternalMessageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
