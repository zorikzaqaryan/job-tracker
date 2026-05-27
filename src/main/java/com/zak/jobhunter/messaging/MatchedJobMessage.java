package com.zak.jobhunter.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.io.Serializable;

/**
 * Message published to {@code matched-jobs} queue after a job post has been
 * scored and met the configured threshold.  The Telegram-sender service
 * consumes this queue and dispatches the notification.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchedJobMessage implements Serializable {

    private Long   jobId;
    private String title;
    private String company;
    private String location;
    private String url;
    private int    score;
    private String sourceName;
}
