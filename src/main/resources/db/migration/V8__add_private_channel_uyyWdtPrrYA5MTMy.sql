-- Private Telegram channel (invite link)
INSERT INTO job_sources (source_type, name, telegram_username, telegram_channel_id, url, enabled)
SELECT 'TELEGRAM',
       'test_Jobs',
       '+uyyWdtPrrYA5MTMy',
       NULL,
       'https://t.me/+uyyWdtPrrYA5MTMy',
       TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM job_sources WHERE url = 'https://t.me/+uyyWdtPrrYA5MTMy'
);
