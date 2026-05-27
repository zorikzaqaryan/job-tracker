package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.job.JobStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiDecisionServiceTest {

    private AiDecisionService decisionService;
    private AiProperties aiProperties;

    @BeforeEach
    void setUp() {
        decisionService = new AiDecisionService();
        aiProperties    = mock(AiProperties.class);
        when(aiProperties.minQualificationScore()).thenReturn(80);
    }

    @Test
    void qualifiedResult_returnsAiMatched() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 88, true, true, true,
                List.of("Java", "Spring Boot"), List.of(), List.of(),
                "QUALIFIED", "Strong match");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_MATCHED);
    }

    @Test
    void notRelevant_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                false, 85, true, true, true,
                List.of(), List.of(), List.of(),
                "NOT_QUALIFIED", "Not relevant");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void scoreBelowThreshold_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 60, true, true, true,
                List.of("Java"), List.of(), List.of(),
                "NEEDS_REVIEW", "Low score");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void usOnlyFlag_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 90, false, false, true,
                List.of("Java"), List.of(), List.of("US_ONLY"),
                "REJECTED_LOCATION", "US only");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void onsiteOnlyFlag_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 90, false, false, true,
                List.of(), List.of(), List.of("ONSITE_ONLY"),
                "REJECTED_LOCATION", "Onsite only");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void internshipFlag_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 82, true, true, false,
                List.of(), List.of(), List.of("INTERNSHIP"),
                "REJECTED_SENIORITY", "Internship");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void notRemoteCompatible_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 85, false, true, true,
                List.of("Java"), List.of(), List.of(),
                "REJECTED_REMOTE_POLICY", "Not remote");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void notLocationCompatible_returnsAiRejected() {
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 85, true, false, true,
                List.of("Java"), List.of(), List.of(),
                "REJECTED_LOCATION", "Wrong location");

        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_REJECTED);
    }

    @Test
    void unclearRemoteWithoutCriticalFlags_needsReviewButDoesNotAutoReject() {
        // REMOTE_POLICY_UNCLEAR is not in the critical flags list
        // so it should still be decided as matched if all other criteria pass
        JobAiAnalysisResult result = new JobAiAnalysisResult(
                true, 82, true, true, true,
                List.of("Java"), List.of(), List.of("REMOTE_POLICY_UNCLEAR"),
                "NEEDS_REVIEW", "Unclear remote policy");

        // REMOTE_POLICY_UNCLEAR is not a critical flag → should be AI_MATCHED
        assertThat(decisionService.decide(result, aiProperties)).isEqualTo(JobStatus.AI_MATCHED);
    }
}
