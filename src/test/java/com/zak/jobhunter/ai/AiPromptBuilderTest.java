package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobContextDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPromptBuilderTest {

    private AiPromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new AiPromptBuilder();
    }

    @Test
    void systemPrompt_containsDecisionRules() {
        String prompt = promptBuilder.buildSystemPrompt();
        assertThat(prompt)
                .contains("REJECT if the role is US-only")
                .contains("REJECT if the role is unpaid or an internship")
                .contains("qualificationScore")
                .contains("QUALIFIED");
    }

    @Test
    void userPrompt_includesCandidateSkills() {
        CandidateContextDto candidate = new CandidateContextDto(
                "Senior Java Developer", "Yerevan, Armenia", 9,
                "Backend specialist", "Senior Backend Engineer",
                "Worldwide, Europe", "Remote",
                "US-only, onsite",
                List.of("Java (Expert)", "Spring Boot (Expert)", "Kafka (Intermediate)")
        );

        JobContextDto job = new JobContextDto(
                1L, "Senior Java Engineer", "Acme Corp",
                "Remote Worldwide", "We need Java and Spring Boot.",
                "https://jobs.example.com/1", null, 15,
                List.of("java", "spring boot", "remote")
        );

        String prompt = promptBuilder.buildUserPrompt(job, candidate);

        assertThat(prompt)
                .contains("Java (Expert)")
                .contains("Spring Boot (Expert)")
                .contains("Kafka (Intermediate)")
                .contains("Senior Java Developer")
                .contains("Yerevan, Armenia");
    }

    @Test
    void userPrompt_includesJobDescription() {
        CandidateContextDto candidate = new CandidateContextDto(
                "Developer", "Armenia", 5, null,
                "Backend", "Worldwide", "Remote", null, List.of("Java")
        );

        JobContextDto job = new JobContextDto(
                2L, "Java Backend Engineer", "TechCo",
                "Remote", "Experience with Kafka and PostgreSQL required.",
                null, null, 12, List.of("kafka", "postgresql")
        );

        String prompt = promptBuilder.buildUserPrompt(job, candidate);

        assertThat(prompt)
                .contains("Java Backend Engineer")
                .contains("TechCo")
                .contains("Kafka and PostgreSQL")
                .contains("kafka, postgresql");
    }

    @Test
    void userPrompt_includesEnrichedTextWhenPresent() {
        CandidateContextDto candidate = new CandidateContextDto(
                "Developer", "Armenia", 5, null, "Backend", "Worldwide", "Remote",
                null, List.of("Java")
        );

        JobContextDto job = new JobContextDto(
                3L, "Backend Engineer", "StartupXYZ",
                "Remote", "Java Spring Boot role.",
                "https://example.com/job", "Full page content about the role...",
                10, List.of("java")
        );

        String prompt = promptBuilder.buildUserPrompt(job, candidate);

        assertThat(prompt).contains("Full page content about the role...");
    }

    @Test
    void userPrompt_includesRuleScore() {
        CandidateContextDto candidate = new CandidateContextDto(
                "Developer", "Armenia", 5, null, "Backend", "Worldwide", "Remote",
                null, List.of("Java")
        );

        JobContextDto job = new JobContextDto(
                4L, "Java Developer", "Corp", "Remote",
                "Java role.", null, null, 18, List.of("java", "remote")
        );

        String prompt = promptBuilder.buildUserPrompt(job, candidate);

        assertThat(prompt).contains("Rule-based score from keyword engine: 18");
        assertThat(prompt).contains("java, remote");
    }
}
