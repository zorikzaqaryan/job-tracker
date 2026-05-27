-- Deep links: open channel / open original Telegram message

ALTER TABLE job_sources
    ADD COLUMN IF NOT EXISTS telegram_channel_url TEXT;

ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS telegram_message_url TEXT;

-- Backfill channel URLs from invite/public links already stored in url
UPDATE job_sources
SET telegram_channel_url = url
WHERE telegram_channel_url IS NULL
  AND url IS NOT NULL
  AND url LIKE '%t.me%';
