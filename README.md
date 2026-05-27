# Job Hunter

Personal job-search automation backend built with **Java 21** + **Spring Boot 3**.

Reads job posts from Telegram channels (via your user account using TDLib), filters and scores them against configurable keyword rules, optionally runs **AI qualification** (Gemini or mock), deduplicates them, stores everything in PostgreSQL, and forwards qualified jobs to your private Telegram channel via a bot.

**AI pipeline docs:** see [AI_PIPELINE.md](AI_PIPELINE.md) for setup (`GEMINI_API_KEY`, candidate profile, queues, mock provider).

---

## Architecture

```
Telegram Channels
      │  (TDLib user account)
      ▼
TdLibTelegramClient
      │  publishes RawJobMessage
      ▼
RabbitMQ  raw-job-messages
      │
      ▼
RawMessageListener
      │  1. persist RawMessage
      │  2. deduplicate (hash)
      │  3. normalize / extract fields
      │  4. apply MatchingService
      │  5. save JobPost + JobRuleMatches
      │  6. if score ≥ threshold → MATCHED_BY_RULES
      │  7. if AI enabled → RabbitMQ ai-job-analysis-requests → AI → AI_MATCHED → Bot API
      ▼
PostgreSQL                    Telegram Bot API
(jobs, raw_messages,          (output channel)
 filter_rules, job_sources,
 job_rule_matches)
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Docker + Docker Compose | latest |

---

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d
```

PostgreSQL is available at `localhost:5432`.  
RabbitMQ management UI is at <http://localhost:15672> (jobhunter / jobhunter).
Grafana  http://localhost:3000/?orgId=1&refresh=30s
Actuator prometeus http://host.docker.internal:8080/actuator/prometheus
### 2. Configure environment

```bash
cp .env.example .env
# Edit .env with your Telegram credentials
```

### 3. Run the application

```bash
mvn spring-boot:run
```

Swagger UI: <http://localhost:8080/swagger-ui.html>  
API docs: <http://localhost:8080/api-docs>  
Actuator health: <http://localhost:8080/actuator/health>

---

## Configuration

All settings live in `src/main/resources/application.yml`.  
Secrets are injected via environment variables (see `.env.example`).

| Property | Default | Description |
|----------|---------|-------------|
| `app.matching.threshold` | `7` | Minimum rule score for first stage (AI filters further) |
| `app.ai.enabled` | `true` | Enable second-stage AI analysis |
| `app.ai.provider` | `mock` | `mock` (local) or `gemini` (requires `GEMINI_API_KEY`) |
| `app.telegram.bot.token` | — | Telegram Bot API token |
| `app.telegram.bot.output-channel-id` | — | Destination channel ID |
| `app.telegram.user.enabled` | `false` | Enable TDLib user collector |
| `app.telegram.user.api-id` | — | Telegram API ID (my.telegram.org) |
| `app.telegram.user.api-hash` | — | Telegram API hash |
| `app.telegram.user.phone-number` | — | Your phone number |

---

## TDLib setup (Telegram user collector)

When `app.telegram.user.enabled=true`, the app uses **TDLight Java** to read channels as your user account.

**Full guide:** [TDLIB_SETUP.md](TDLIB_SETUP.md)

**Quick checklist:**
1. Create API app at [my.telegram.org](https://my.telegram.org/apps) → `TELEGRAM_API_ID`, `TELEGRAM_API_HASH`
2. Set `TELEGRAM_PHONE_NUMBER=+...` in `.env`
3. `app.telegram.user.enabled=true` in `application.yml`
4. Register channels via `POST /api/channels` (`telegramUsername` or `telegram_channel_id`)
5. Run `mvn spring-boot:run` in a **terminal** and enter the OTP when prompted (first login only)

Maven pulls Windows natives automatically (`tdlight-natives:windows_amd64`). No manual TDLib build required on Windows.

---

## REST API summary

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/channels` | List channels (filter by `sourceType`, `enabled`) |
| POST | `/api/channels` | Create channel |
| PUT | `/api/channels/{id}` | Update channel |
| PATCH | `/api/channels/{id}/enable` | Enable channel |
| PATCH | `/api/channels/{id}/disable` | Disable channel |
| GET | `/api/filter-rules` | List filter rules |
| POST | `/api/filter-rules` | Create rule |
| POST | `/api/filter-rules/test` | Test matching (dry-run) |
| POST | `/api/raw-messages` | Ingest a raw message |
| GET | `/api/jobs` | List all jobs (paginated) |
| GET | `/api/jobs/matched` | Matched jobs |
| GET | `/api/jobs/sent` | Sent jobs |
| PATCH | `/api/jobs/{id}/ignore` | Mark as IGNORED |
| POST | `/api/jobs/{id}/resend` | Resend to Telegram |

---

## Running tests

```bash
mvn test
```

Unit tests cover: `TextNormalizer`, `MatchingService`, `HashUtils`, `ChannelService`, `FilterRuleService`, `AiPromptBuilder`, `AiDecisionService`, `MockJobAiAnalyzer`.

---

## Package structure

```
com.zak.jobhunter
├── JobHunterApplication
├── channel/          — job source management
├── filter/           — filter rules + matching engine
├── ingestion/        — raw message ingest + RabbitMQ listener
├── job/              — job post lifecycle
├── telegram/         — TDLib collector + Bot API sender
├── messaging/        — RabbitMQ config + message POJOs
├── ai/               — AI qualification (Gemini, mock, listener)
├── candidate/        — candidate profile for AI context
├── enrichment/       — URL content fetch before AI
├── metrics/          — Micrometer custom metrics
├── config/           — OpenAPI, Jackson, app properties
└── common/           — error handling, hashing, clock
```
