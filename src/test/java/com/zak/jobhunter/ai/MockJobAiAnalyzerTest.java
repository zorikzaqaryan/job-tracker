package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.ai.dto.JobContextDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockJobAiAnalyzerTest {

    private MockJobAiAnalyzer analyzer;
    private CandidateContextDto candidate;

    @BeforeEach
    void setUp() {
        analyzer = new MockJobAiAnalyzer();
        candidate = new CandidateContextDto(
                "Senior Java Developer", "Yerevan, Armenia", 9,
                null, "Backend", "Worldwide, Europe, Armenia", "Remote",
                "US-only, onsite", List.of("Java", "Spring Boot", "Kafka")
        );
    }

    @Test
    void javaAndRemote_returnsQualified() {
        JobContextDto job = jobCtx("Senior Java Backend Engineer at Remote Corp",
                "We need Java Spring Boot and remote work.", "Remote Worldwide");

        JobAiAnalysisResult result = analyzer.analyze(job, candidate);

        assertThat(result.relevant()).isTrue();
        assertThat(result.qualificationScore()).isGreaterThanOrEqualTo(80);
        assertThat(result.decision()).isEqualTo("QUALIFIED");
        assertThat(result.remoteCompatible()).isTrue();
        assertThat(result.matchedSkills()).contains("java");
    }

    @Test
    void usOnly_returnsRejectedLocation() {
        JobContextDto job = jobCtx("Java Developer US Only",
                "Must be US citizen. US only. Java experience required.", "US only");

        JobAiAnalysisResult result = analyzer.analyze(job, candidate);

        assertThat(result.relevant()).isFalse();
        assertThat(result.decision()).isEqualTo("REJECTED_LOCATION");
        assertThat(result.riskFlags()).contains("US_ONLY");
    }

    @Test
    void internship_returnsNotQualified() {
        JobContextDto job = jobCtx("Java Internship",
                "This is a Java internship position. Unpaid internship.", "Remote");

        JobAiAnalysisResult result = analyzer.analyze(job, candidate);

        assertThat(result.decision()).isEqualTo("NOT_QUALIFIED");
        assertThat(result.riskFlags()).contains("INTERNSHIP");
    }

    @Test
    void javaWithoutRemote_returnsNeedsReview() {
        JobContextDto job = jobCtx("Senior Java Developer",
                "Java Spring Boot backend developer needed.", "Berlin, Germany");

        JobAiAnalysisResult result = analyzer.analyze(job, candidate);

        assertThat(result.decision()).isEqualTo("NEEDS_REVIEW");
        assertThat(result.riskFlags()).contains("REMOTE_POLICY_UNCLEAR");
    }

    @Test
    void noJavaNoRemote_returnsNotQualified() {
        JobContextDto job = jobCtx("WordPress Developer",
                "PHP WordPress developer needed. Onsite in London.", "London, UK");

        JobAiAnalysisResult result = analyzer.analyze(job, candidate);

        assertThat(result.qualificationScore()).isLessThan(80);
        assertThat(result.decision()).isEqualTo("NOT_QUALIFIED");
    }

    @Test
    void provider_returnsMock() {
        assertThat(analyzer.provider()).isEqualTo(AiProvider.MOCK);
    }

    private JobContextDto jobCtx(String title, String description, String location) {
        return new JobContextDto(1L, title, "TestCo", location, description,
                null, null, 10, List.of("java"));
    }
}
