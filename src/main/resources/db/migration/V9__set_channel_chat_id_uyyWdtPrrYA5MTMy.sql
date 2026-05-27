-- Numeric chat id from Telegram Web: https://web.telegram.org/k/#-3981546775
-- Avoids invite-link resolution at startup (works immediately with TDLib).
UPDATE job_sources
SET telegram_channel_id = '-3981546775'
WHERE url = 'https://t.me/+uyyWdtPrrYA5MTMy'
   OR telegram_username = '+uyyWdtPrrYA5MTMy';
