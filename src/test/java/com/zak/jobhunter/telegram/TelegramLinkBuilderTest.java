package com.zak.jobhunter.telegram;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramLinkBuilderTest {

    @Test
    void buildMessageUrl_publicChannel() {
        assertThat(TelegramLinkBuilder.buildMessageUrl("job_channel", "-100123", "42"))
                .isEqualTo("https://t.me/job_channel/42");
    }

    @Test
    void buildMessageUrl_privateSupergroup() {
        assertThat(TelegramLinkBuilder.buildMessageUrl(null, "-1003813699756", "99"))
                .isEqualTo("https://t.me/c/3813699756/99");
    }

    @Test
    void buildMessageUrl_convertsTdLibMessageId() {
        assertThat(TelegramLinkBuilder.buildMessageUrl(null, "2036676380", "78581334016"))
                .isEqualTo("https://t.me/c/2036676380/74941");
    }

    @Test
    void toPublicMessageId_shiftsLargeTdLibIds() {
        assertThat(TelegramLinkBuilder.toPublicMessageId("78581334016")).isEqualTo("74941");
        assertThat(TelegramLinkBuilder.toPublicMessageId("42")).isEqualTo("42");
    }

    @Test
    void buildChannelUrl_prefersExistingInviteLink() {
        assertThat(TelegramLinkBuilder.buildChannelUrl(null, null, "https://t.me/+inviteHash"))
                .isEqualTo("https://t.me/+inviteHash");
    }

    @Test
    void buildChannelUrl_fromUsername() {
        assertThat(TelegramLinkBuilder.buildChannelUrl("@jobs", null, null))
                .isEqualTo("https://t.me/jobs");
    }

    @Test
    void toPrivateChatPath_stripsMinus100Prefix() {
        assertThat(TelegramLinkBuilder.toPrivateChatPath("-1003813699756"))
                .isEqualTo("3813699756");
    }

    @Test
    void toPrivateChatPath_numericIdWithout100Prefix() {
        assertThat(TelegramLinkBuilder.toPrivateChatPath("-3981546775"))
                .isEqualTo("3981546775");
    }
}
