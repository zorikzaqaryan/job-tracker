package com.zak.jobhunter.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageUrlExtractorTest {

    @Test
    void extractApplyUrl_fromApplyLine() {
        String text = """
                💼 Senior Solutions Architect
                ✅ Apply → https://remoteyeah.com/jobs/remote-senior-solutions-architect?utm_source=telegram
                """;
        assertThat(MessageUrlExtractor.extractApplyUrl(text))
                .isEqualTo("https://remoteyeah.com/jobs/remote-senior-solutions-architect?utm_source=telegram");
    }

    @Test
    void extractApplyUrl_skipsTelegramLinks() {
        String text = "See https://t.me/c/2036676380/99 and https://jobs.example.com/role";
        assertThat(MessageUrlExtractor.extractApplyUrl(text))
                .isEqualTo("https://jobs.example.com/role");
    }
}
