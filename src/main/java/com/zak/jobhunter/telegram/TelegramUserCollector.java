package com.zak.jobhunter.telegram;

/**
 * Abstraction over a Telegram user-account collector.
 *
 * <p>A user account (not a bot) is required to read messages from channels
 * where only the account is subscribed.  The primary implementation uses
 * TDLib via JNI ({@link TdLibTelegramClient}).
 *
 * <p>A no-op stub implementation is provided for local development when TDLib
 * native libraries are not installed.
 */
public interface TelegramUserCollector {

    /** Authenticate and start listening for new channel messages. */
    void start();

    /** Gracefully shut down the collector and release TDLib resources. */
    void stop();

    /** Returns {@code true} if the collector is currently running. */
    boolean isRunning();
}
