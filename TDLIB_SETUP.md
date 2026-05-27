# TDLib / TDLight setup (Telegram user collector)

Your app reads job channels with **your personal Telegram account** (not the bot). That requires TDLib. This project uses **[TDLight Java](https://github.com/tdlight-team/tdlight-java)** — it bundles the native `tdjni` library for Windows, so you do **not** need to compile TDLib from source.

---

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **Java 21** (64-bit) | Must match `windows_amd64` natives |
| **Microsoft Visual C++ Redistributable** | [Download](https://learn.microsoft.com/en-us/cpp/windows/latest-supported-vc-redist) if you see `UnsatisfiedLinkError` |
| **Telegram API credentials** | From [my.telegram.org](https://my.telegram.org/apps) |
| **Your phone number** | International format, e.g. `+37499123456` |
| **Subscribed channels** | Join channels in the Telegram app first; the collector only sees chats your account can read |

---

## Step 1 — Create Telegram API application

1. Open [https://my.telegram.org](https://my.telegram.org) and log in.
2. Go to **API development tools**.
3. Create an app (any title/description).
4. Copy **api_id** (integer) and **api_hash** (string).

---

## Step 2 — Configure `.env`

```env
TELEGRAM_API_ID=12345678
TELEGRAM_API_HASH=your_api_hash_here
TELEGRAM_PHONE_NUMBER=+37499123456
```

Keep existing bot settings for the **output** channel:

```env
TELEGRAM_BOT_TOKEN=...
TELEGRAM_OUTPUT_CHANNEL_ID=-100...
```

---

## Step 3 — Enable the collector in `application.yml`

```yaml
app:
  telegram:
    user:
      enabled: true
      api-id: ${TELEGRAM_API_ID:0}
      api-hash: ${TELEGRAM_API_HASH:}
      phone-number: ${TELEGRAM_PHONE_NUMBER:}
      database-directory: ./tdlib-db
      files-directory: ./tdlib-files
```

Session data is stored under `./tdlib-db/session-<phone>/` and `./tdlib-files/session-<phone>/` (already in `.gitignore`).

### “Trusted” / no more prompts?

Telegram **cannot** skip the first login on a new session (OTP + 2FA if enabled). That is enforced by Telegram, not by this app.

After **one successful** login, TDLib saves the session locally. **Every restart** with the same session folder should connect **without** asking for code or password — that is the equivalent of a “trusted device”.

**Do not delete** `tdlib-db/session-*` between runs if you want silent restarts.

### `PASSWORD_HASH_INVALID`

You entered the wrong **Cloud Password**. The log line `Hint: 080555` is only a **reminder** Telegram shows you when you set the password — it is **not** the password itself.

Enter the real password from **Telegram → Settings → Privacy → Two-Step Verification**.

If login failed mid-way, stop the app, delete the partial session folder (e.g. `tdlib-db/session-37477552575`), and start again with the correct password.

---

## Step 4 — Register channels in the database

Each source needs either a **numeric chat id** or a **public @username**.

| Field | Example | When to use |
|-------|---------|-------------|
| `telegram_username` | `remoteyeah` | Public channels (resolved at startup via TDLib) |
| `telegram_channel_id` | `-1001234567890` | If you already know the numeric id |

**API example:**

```http
POST http://localhost:8080/api/channels
Content-Type: application/json

{
  "sourceType": "TELEGRAM",
  "name": "remoteyeah",
  "telegramUsername": "remoteyeah",
  "enabled": true
}
```

For **private** channels you are subscribed to, TDLib still receives messages, but you should store the correct **numeric chat id** in `telegram_channel_id`.

**How to find the chat id:** open the channel in [Telegram Web](https://web.telegram.org) — the URL looks like `https://web.telegram.org/k/#-3981546775`. Use that number (including the minus sign) as `telegram_channel_id`. Example for this project:

| Field | Value |
|-------|--------|
| `url` | `https://t.me/+uyyWdtPrrYA5MTMy` |
| `telegram_channel_id` | `-3981546775` |

---

## Step 5 — Start infrastructure and the app

```powershell
docker compose up -d
cd C:\Users\zorikz\Documents\PROJECTS\job-tracker
$env:JAVA_HOME = "C:\Users\zorikz\.jdks\ms-21.0.10"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
# Load .env into the shell (or use your IDE env injection)
mvn spring-boot:run
```

**Run in a real terminal** (not a detached background job) so you can enter the login code.

---

## Step 6 — First-time login (OTP)

On first run, watch the console:

```
[TDLib] *** Enter the login code in this console (Telegram app → message from Telegram) ***
```

1. Telegram sends a code to your **Telegram app** (or SMS).
2. Type the code in the **same terminal** where `spring-boot:run` is running and press Enter.
3. If you use 2FA, enter your cloud password when prompted.

When login succeeds:

```
[TDLib] Logged in successfully
[TDLib] Channel 'remoteyeah' (@remoteyeah) → chatId=-100...
[TDLib] Tracking N channel(s) for new messages
```

New posts in tracked channels will log like:

```
[TDLib] ▶ Message from channel='remoteyeah' chatId=-100... msgId=12345 text='...'
```

---

## Step 7 — Verify the pipeline

1. **Prometheus/Grafana** (optional): ingestion counters per channel.
2. **RabbitMQ**: [http://localhost:15672](http://localhost:15672) — queue `raw-job-messages` should get messages.
3. **REST**: `GET http://localhost:8080/api/jobs` — jobs appear after matching/AI.

---

## Troubleshooting

### `UnsatisfiedLinkError: tdjni.windows_amd64`

- Install **VC++ Redistributable** (64-bit).
- Ensure **64-bit Java 21** (`java -version`).
- Rebuild: `mvn clean compile` (Maven downloads `tdlight-natives` with classifier `windows_amd64`).

### `TELEGRAM_API_ID is missing or invalid`

- Set variables in `.env` and ensure they are loaded when starting the app.

### `Could not resolve channel … : null` after ~60 seconds

This was usually a **deadlock**: channel resolution ran on the TDLib update thread while blocking with `.get(30s)`. TDLib could not process the response, so each call timed out (30s + 30s ≈ 60s) and the error message was `null`.

**Fixed in code:** resolution now runs on a dedicated `tdlib-api-worker` thread. Restart the app after pulling the latest code.

### No messages from a channel

- Confirm you **joined** the channel in the Telegram app.
- Confirm the channel is **enabled** in `job_sources` (`GET /api/channels?enabled=true`).
- Check logs for `Could not resolve channel` — fix `telegram_username` or `telegram_channel_id`.
- **Muted channels still deliver messages** to TDLib (mute only affects notifications).

### Collector disabled

If `app.telegram.user.enabled=false`, the no-op stub runs and messages must be ingested via `POST /api/raw-messages`.

---

## Linux / macOS

Change the native classifier in `pom.xml`:

| OS | Classifier |
|----|------------|
| Windows 64-bit | `windows_amd64` (default in this project) |
| Linux 64-bit | `linux_amd64_gnu_ssl3` or `linux_amd64_gnu_ssl1` |
| macOS Apple Silicon | `macos_arm64` |
| macOS Intel | `macos_amd64` |

Only one classifier should be active for your platform.

---

## Security notes

- **Never commit** `.env`, `tdlib-db/`, or `tdlib-files/`.
- The session in `tdlib-db` is equivalent to your Telegram login — treat it like a password.
- Use a dedicated Telegram account if you prefer isolation from your main account.
