package com.zak.jobhunter.telegram.tdlib;

import com.zak.jobhunter.channel.dto.ChannelResponse;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.TelegramError;
import it.tdlight.jni.TdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Maps TDLib {@code chatId} values to configured {@link ChannelResponse} sources.
 * Resolves {@code @username} to numeric chat IDs via {@link TdApi.SearchPublicChat}.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "app.telegram.user.enabled", havingValue = "true")
public class TdLibChannelRegistry {

    private static final Logger log = LoggerFactory.getLogger(TdLibChannelRegistry.class);

    private final Map<Long, ChannelResponse> chatIdToChannel = new ConcurrentHashMap<>();

    public void refresh(SimpleTelegramClient client, java.util.List<ChannelResponse> sources) {
        chatIdToChannel.clear();
        for (ChannelResponse source : sources) {
            try {
                resolveAndRegister(client, source);
            } catch (Exception ex) {
                log.warn("[TDLib] Could not resolve channel '{}' (id={}): {}",
                        source.name(), source.id(), describeError(ex));
            }
        }
        log.info("[TDLib] Tracking {} channel(s) for new messages", chatIdToChannel.size());
    }

    public Optional<ChannelResponse> findByChatId(long chatId) {
        return Optional.ofNullable(chatIdToChannel.get(chatId));
    }

    public Map<Long, ChannelResponse> snapshot() {
        return Collections.unmodifiableMap(chatIdToChannel);
    }

    private void resolveAndRegister(SimpleTelegramClient client, ChannelResponse source) throws Exception {
        Long chatId = parseNumericChatId(source.telegramChannelId());
        if (chatId != null) {
            register(chatId, source);
            log.info("[TDLib] Channel '{}' → chatId={} (from DB id)", source.name(), chatId);
            return;
        }

        String inviteLink = extractInviteLink(source);
        if (inviteLink != null) {
            resolveByInviteLink(client, source, inviteLink);
            return;
        }

        String username = normalizePublicUsername(source.telegramUsername());
        if (username == null) {
            log.warn("[TDLib] Channel '{}' has no resolvable id, username, or invite link — skipped",
                    source.name());
            return;
        }

        TdApi.Chat chat = client.send(new TdApi.SearchPublicChat(username))
                .get(30, TimeUnit.SECONDS);
        register(chat.id, source);
        log.info("[TDLib] Channel '{}' (@{}) → chatId={}", source.name(), username, chat.id);
    }

    private void resolveByInviteLink(SimpleTelegramClient client, ChannelResponse source, String inviteLink)
            throws Exception {
        log.info("[TDLib] Resolving invite link for '{}' → {}", source.name(), inviteLink);

        try {
            TdApi.ChatInviteLinkInfo info = client
                    .send(new TdApi.CheckChatInviteLink(inviteLink))
                    .get(30, TimeUnit.SECONDS);

            if (info.chatId != 0) {
                register(info.chatId, source);
                log.info("[TDLib] Channel '{}' (invite) → chatId={} title='{}'",
                        source.name(), info.chatId, info.title);
                return;
            }
            log.info("[TDLib] Invite check returned chatId=0 for '{}' (not a member yet?) — trying join",
                    source.name());
        } catch (Exception ex) {
            log.info("[TDLib] checkChatInviteLink for '{}': {}", source.name(), describeError(ex));
        }

        try {
            TdApi.Chat chat = client.send(new TdApi.JoinChatByInviteLink(inviteLink))
                    .get(30, TimeUnit.SECONDS);
            register(chat.id, source);
            log.info("[TDLib] Channel '{}' (joined via invite) → chatId={}", source.name(), chat.id);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "JoinChatByInviteLink failed — join the channel in the Telegram app first, then restart. "
                            + describeError(ex), ex);
        }
    }

    private static String describeError(Throwable ex) {
        Throwable root = ex;
        if (ex instanceof ExecutionException && ex.getCause() != null) {
            root = ex.getCause();
        }
        if (root instanceof TelegramError te) {
            return "Telegram " + te.getErrorCode() + ": " + te.getErrorMessage();
        }
        if (root instanceof TimeoutException) {
            return "Timed out waiting for TDLib (30s) — often caused by blocking TDLib on its own thread";
        }
        if (root.getMessage() != null && !root.getMessage().isBlank()) {
            return root.getMessage();
        }
        return root.getClass().getSimpleName();
    }

    private static String extractInviteLink(ChannelResponse source) {
        if (source.url() != null && source.url().contains("t.me/+")) {
            return source.url().trim();
        }
        String username = source.telegramUsername();
        if (username != null && username.trim().startsWith("+")) {
            String slug = username.trim();
            return "https://t.me/" + slug;
        }
        if (username != null && username.contains("t.me/+")) {
            return username.trim();
        }
        return null;
    }

    private void register(long chatId, ChannelResponse source) {
        chatIdToChannel.put(chatId, source);
    }

    private static Long parseNumericChatId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Public @username only (not private invite slugs like +xxx). */
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
            if (u.startsWith("+")) {
                return null;
            }
        }
        return u.isBlank() ? null : u;
    }
}
