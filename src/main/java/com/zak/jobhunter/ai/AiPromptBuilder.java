package com.zak.jobhunter.ai;

import com.zak.jobhunter.ai.dto.CandidateContextDto;
import com.zak.jobhunter.ai.dto.JobContextDto;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds structured prompts for the AI qualification analysis.
 *
 * <p>Design principles:
 * <ul>
 *   <li>System prompt establishes role and decision rules.</li>
 *   <li>User prompt supplies candidate + job context and requests JSON output.</li>
 *   <li>JSON schema is embedded in the prompt to guide structured output.</li>
 *   <li>Personal data (email, phone) is never included.</li>
 * </ul>
 */
@Component
public class AiPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            You are a job qualification assistant for a software engineer based in Yerevan, Armenia.

            Your task:
            Analyze whether the given job posting is worth applying to for this candidate.

            Decision rules (apply in order):
            1. REJECT if the role is US-only, UK-only, or requires on-site presence outside Armenia.
            2. REJECT if the role is unpaid or an internship.
            3. REJECT if the role is clearly junior-only and the candidate has 5+ years of experience.
            4. REJECT if the tech stack is completely unrelated (e.g. pure PHP, pure mobile, pure frontend only).
            5. If remote policy is unclear, set decision=NEEDS_REVIEW and add REMOTE_POLICY_UNCLEAR to riskFlags.
            6. Increase score for: Java, Spring Boot, backend, microservices, Kafka, RabbitMQ, PostgreSQL, AWS, GCP, Kubernetes, Angular, senior/lead roles, worldwide/Europe-compatible remote.
            7. Decrease score for: frontend-only, JavaScript-only, non-Java primary stack.

            Output rules:
            - Return ONLY valid JSON. No markdown, no code blocks, no extra text.
            - Do not invent facts not present in the job description.
            - If information is missing, add a risk flag instead of guessing.
            - qualificationScore must be between 0 and 100.

            Required JSON shape:
            {
              "relevant": true,
              "qualificationScore": 87,
              "remoteCompatible": true,
              "locationCompatible": true,
              "seniorityCompatible": true,
              "matchedSkills": ["Java", "Spring Boot"],
              "missingImportantSkills": ["Kotlin"],
              "riskFlags": [],
              "decision": "QUALIFIED",
              "reason": "Strong Java backend match with worldwide remote compatibility."
            }

            Allowed decision values: QUALIFIED, NOT_QUALIFIED, NEEDS_REVIEW,
            REJECTED_LOCATION, REJECTED_SENIORITY, REJECTED_TECH_STACK, REJECTED_REMOTE_POLICY
            """;

    /** Builds the system prompt (role + rules + JSON schema). */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /** Builds the user prompt containing candidate and job context. */
    public String buildUserPrompt(JobContextDto job, CandidateContextDto candidate) {
        StringBuilder sb = new StringBuilder();

        // ── Candidate context ─────────────────────────────────────────────
        sb.append("=== CANDIDATE ===\n");
        sb.append("Current title: ").append(safe(candidate.currentTitle())).append("\n");
        sb.append("Location: ").append(safe(candidate.location())).append("\n");
        sb.append("Years of experience: ").append(safe(candidate.yearsOfExperience())).append("\n");
        sb.append("Preferred job titles: ").append(safe(candidate.preferredJobTitles())).append("\n");
        sb.append("Preferred locations: ").append(safe(candidate.preferredLocations())).append("\n");
        sb.append("Preferred work modes: ").append(safe(candidate.preferredWorkModes())).append("\n");

        if (candidate.summary() != null && !candidate.summary().isBlank()) {
            sb.append("Summary: ").append(candidate.summary()).append("\n");
        }

        if (candidate.skills() != null && !candidate.skills().isEmpty()) {
            sb.append("Skills: ").append(String.join(", ", candidate.skills())).append("\n");
        }

        if (candidate.avoidRules() != null && !candidate.avoidRules().isBlank()) {
            sb.append("Avoid: ").append(candidate.avoidRules()).append("\n");
        }

        // ── Job context ───────────────────────────────────────────────────
        sb.append("\n=== JOB POSTING ===\n");
        sb.append("Title: ").append(safe(job.title())).append("\n");
        sb.append("Company: ").append(safe(job.company())).append("\n");
        sb.append("Location: ").append(safe(job.location())).append("\n");

        if (job.url() != null && !job.url().isBlank()) {
            sb.append("URL: ").append(job.url()).append("\n");
        }

        sb.append("\nDescription:\n").append(safe(job.description())).append("\n");

        if (job.enrichedText() != null && !job.enrichedText().isBlank()) {
            sb.append("\nAdditional page content (from URL fetch):\n")
              .append(job.enrichedText()).append("\n");
        }

        // ── Rule-based signals ────────────────────────────────────────────
        sb.append("\nRule-based score from keyword engine: ").append(job.ruleScore()).append("\n");
        if (job.matchedKeywords() != null && !job.matchedKeywords().isEmpty()) {
            sb.append("Matched keywords: ").append(String.join(", ", job.matchedKeywords())).append("\n");
        }

        sb.append("\nAnalyze this job and return ONLY the JSON response described above.");
        return sb.toString();
    }

    private String safe(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
