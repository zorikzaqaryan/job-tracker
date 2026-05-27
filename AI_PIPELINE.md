# AI Job Qualification Pipeline

## Overview

Jobs pass through a **two-stage qualification** process before reaching your Telegram output channel:

```
Raw Telegram Message
       ↓
Normalize + Deduplicate
       ↓
Rule-based MatchingService
       ↓
  score < threshold?
       ↓ YES         ↓ NO
  NOT_MATCHED    MATCHED_BY_RULES
                      ↓
              AI enabled?
              ↓ YES         ↓ NO
      AI_ANALYSIS_PENDING   MATCHED → Telegram
              ↓
     RabbitMQ: ai-job-analysis-requests
              ↓
      AiAnalysisListener
              ↓
  Build prompt + Call AI provider
              ↓
     AiDecisionService
              ↓
  AI_MATCHED          AI_REJECTED
      ↓
🤖 Telegram notification
```

## Why Two Stages?

1. **Rule engine is fast** — filters 90%+ of irrelevant posts with zero API cost.
2. **AI is expensive** — only called for jobs that already passed keyword matching.
3. **AI is asynchronous** — does not block the ingestion pipeline.
4. **AI recommends, business logic decides** — the `AiDecisionService` applies deterministic rules on top of the AI score.

## Setting up Gemini API

1. Get a free API key at [https://aistudio.google.com/app/apikey](https://aistudio.google.com/app/apikey)
2. Add to your `.env` file:
   ```env
   GEMINI_API_KEY=your_key_here
   APP_AI_PROVIDER=gemini
   APP_AI_ENABLED=true
   ```
3. Restart the application.

## Using the Mock Provider (no API key needed)

```env
APP_AI_PROVIDER=mock
APP_AI_ENABLED=true
```

The mock analyzer uses deterministic rules:
- Job contains Java + Remote → QUALIFIED (score 88)
- Job contains "US only" or "onsite only" → REJECTED_LOCATION
- Job is internship or unpaid → NOT_QUALIFIED
- Job has Java but no remote signal → NEEDS_REVIEW

## Disabling AI entirely

```env
APP_AI_ENABLED=false
```

When disabled, jobs matched by the rule engine are sent directly to Telegram (legacy behaviour).

## Creating a Candidate Profile

The AI needs your profile to reason about job fit.

```bash
POST /api/candidate/profile
Content-Type: application/json

{
  "name": "Your Name",
  "currentTitle": "Senior Java Backend Developer",
  "location": "Yerevan, Armenia",
  "yearsOfExperience": 9,
  "summary": "Backend specialist with Java 21, Spring Boot, Kafka, PostgreSQL",
  "preferredJobTitles": "Senior Java Backend Engineer, Lead Backend Engineer",
  "preferredLocations": "Remote Worldwide, Europe, Armenia",
  "preferredWorkModes": "Remote, Full Remote",
  "avoidRules": "US-only, onsite-only, internship, unpaid, junior roles",
  "skills": [
    {"skillName": "Java", "level": "Expert", "yearsOfExperience": 9},
    {"skillName": "Spring Boot", "level": "Expert", "yearsOfExperience": 7},
    {"skillName": "Kafka", "level": "Intermediate", "yearsOfExperience": 3},
    {"skillName": "PostgreSQL", "level": "Expert", "yearsOfExperience": 7},
    {"skillName": "Docker", "level": "Intermediate"},
    {"skillName": "Kubernetes", "level": "Intermediate"},
    {"skillName": "Angular", "level": "Intermediate"}
  ]
}
```

## Manually Testing AI Analysis

```bash
POST /api/ai/test-job-match
Content-Type: application/json

{
  "title": "Senior Java Backend Engineer",
  "company": "Example Ltd",
  "location": "Remote Worldwide",
  "description": "We need Java 21, Spring Boot, Kafka, PostgreSQL. Fully remote."
}
```

## Manually Triggering AI Analysis for a Specific Job

```bash
POST /api/jobs/{jobId}/ai/analyze
```

## Viewing AI Analysis Results

```bash
GET /api/jobs/{jobId}/ai-analyses
GET /api/ai-analyses/{analysisId}
```

## RabbitMQ Queues

| Queue | Purpose |
|---|---|
| `ai-job-analysis-requests` | Jobs waiting for AI qualification |
| `ai-job-analysis-requests.dlq` | Failed requests after max retries |
| `ai-job-analysis-results` | (reserved for future async result reporting) |
| `job-url-enrichment-requests` | Jobs waiting for URL content fetch |
| `job-url-enrichment-requests.dlq` | Failed enrichment requests |

## AI Retry Behaviour

The AI listener uses a dedicated RabbitMQ container factory with aggressive back-off:
- Attempt 1: immediate
- Attempt 2: after 60 seconds
- Attempt 3: after 180 seconds
- After 3 failures: routed to DLQ, job status = `AI_ANALYSIS_FAILED`

Retry AI analysis manually:
```bash
POST /api/jobs/{jobId}/ai/retry
```

## Decision Rules

The `AiDecisionService` ignores the AI's `decision` string and applies its own deterministic rules:

| Condition | Result |
|---|---|
| `relevant = false` | AI_REJECTED |
| `qualificationScore < threshold` (default: 80) | AI_REJECTED |
| `remoteCompatible = false` | AI_REJECTED |
| `locationCompatible = false` | AI_REJECTED |
| Risk flag: US_ONLY | AI_REJECTED |
| Risk flag: ONSITE_ONLY | AI_REJECTED |
| Risk flag: INTERNSHIP | AI_REJECTED |
| Risk flag: UNPAID | AI_REJECTED |
| Risk flag: JUNIOR_ONLY | AI_REJECTED |
| All checks pass | AI_MATCHED → Telegram |

## Why AI Does Not Browse by Itself

LLMs cannot reliably fetch URLs — they may hallucinate content or the URL may be behind auth/CAPTCHA.

Instead, **our application fetches the page** and passes the extracted text to the AI:
1. `UrlEnrichmentService` fetches the job URL using Jsoup
2. `ReadableTextExtractor` strips navigation/ads and extracts readable text
3. Extracted text is stored in `job_url_enrichments` table
4. `AiAnalysisListener` loads this text and includes it in the AI prompt

Enable URL enrichment:
```env
APP_URL_ENRICHMENT_ENABLED=true
```

Trigger manually:
```bash
POST /api/jobs/{jobId}/enrich-url
```

## Human Approval

The pipeline intentionally does NOT auto-apply to jobs.

When `app.ai.require-human-approval=true` (default), `AI_MATCHED` is the final automated status. You review the job in your Telegram channel, then decide whether to apply.

Future: Set `APPLICATION_CANDIDATE` status via API to track jobs you intend to apply to.

## Adding a New AI Provider

1. Add a new value to `AiProvider` enum.
2. Create a new class implementing `JobAiAnalyzer`.
3. Annotate with `@Component` and `@ConditionalOnProperty(name = "app.ai.provider", havingValue = "ollama")`.
4. Update `APP_AI_PROVIDER` in `.env`.
