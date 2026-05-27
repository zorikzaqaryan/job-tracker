package com.zak.jobhunter.telegram;

/**
 * Builds public Telegram deep links for channels and messages.
 *
 * <p>Public channel: {@code https://t.me/username/123}
 * <p>Private supergroup/channel: {@code https://t.me/c/3813699756/123} (chat id without {@code -100} prefix)
 */
public final class TelegramLinkBuilder {

    private TelegramLinkBuilder() {}

    /** Opens the channel (invite link, @username, or best effort from numeric id). */
    public static String buildChannelUrl(String telegramUsername, String telegramChannelId, String existingUrl) {
        if (existingUrl != null && !existingUrl.isBlank() && existingUrl.contains("t.me")) {
            return existingUrl.trim();
        }
        String username = normalizePublicUsername(telegramUsername);
        if (username != null) {
            return "https://t.me/" + username;
        }
        return null;
    }

    /** Opens the exact Telegram message in the source channel. */
    public static String buildMessageUrl(String telegramUsername, String telegramChannelId, String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        String msgId = messageId.trim();

        String username = normalizePublicUsername(telegramUsername);
        if (username != null) {
            return "https://t.me/" + username + "/" + msgId;
        }

        String privatePath = toPrivateChatPath(telegramChannelId);
        if (privatePath != null) {
            return "https://t.me/c/" + privatePath + "/" + msgId;
        }
        return null;
    }

    /**
     * Converts Bot/TDLib chat id {@code -1003813699756} → {@code 3813699756} for {@code t.me/c/...} links.
     */
    static String toPrivateChatPath(String telegramChannelId) {
        if (telegramChannelId == null || telegramChannelId.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(telegramChannelId.trim());
            if (id >= 0) {
                return String.valueOf(id);
            }
            String abs = String.valueOf(-id);
            if (abs.length() > 3 && abs.startsWith("100")) {
                return abs.substring(3);
            }
            return abs;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizePublicUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String u = username.trim();
        if (u.startsWith("+")) {
            return null;
        }
        if (u.startsWith("@")) {
            u = u.substring(1);
        }
        if (u.startsWith("https://t.me/")) {
            u = u.substring("https://t.me/".length());
            int slash = u.indexOf('/');
            if (slash >= 0) {
                u = u.substring(0, slash);
            }
        }
        if (u.startsWith("+") || u.isBlank()) {
            return null;
        }
        return u;
    }
}
