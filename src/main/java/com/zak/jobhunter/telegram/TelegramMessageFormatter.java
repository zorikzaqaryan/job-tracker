package com.zak.jobhunter.telegram;

import com.zak.jobhunter.ai.dto.JobAiAnalysisResult;
import com.zak.jobhunter.job.JobPost;
import com.zak.jobhunter.job.JobRuleMatch;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Formats matched job posts as Telegram HTML messages.
 *
 * <p>Telegram HTML subset supports: &lt;b&gt;, &lt;i&gt;, &lt;code&gt;,
 * &lt;pre&gt;, &lt;a href&gt;. We escape user-generated text defensively.
 */
@Component
public class TelegramMessageFormatter {

    private static final int MAX_DESCRIPTION_LENGTH = 400;

    public String format(JobPost job, List<JobRuleMatch> matches) {
        StringBuilder sb = new StringBuilder();

        sb.append("🚀 <b>Job Matched</b>\n\n");

        if (job.getTitle() != null && !job.getTitle().isBlank()) {
            sb.append("<b>Title:</b> ").append(escape(job.getTitle())).append("\n");
        }
        if (job.getCompany() != null && !job.getCompany().isBlank()) {
            sb.append("<b>Company:</b> ").append(escape(job.getCompany())).append("\n");
        }
        if (job.getLocation() != null && !job.getLocation().isBlank()) {
            sb.append("<b>Location:</b> ").append(escape(job.getLocation())).append("\n");
        }

        sb.append("<b>Score:</b> ").append(job.getScore()).append("\n");

        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            String snippet = job.getDescription().length() > MAX_DESCRIPTION_LENGTH
                    ? job.getDescription().substring(0, MAX_DESCRIPTION_LENGTH) + "…"
                    : job.getDescription();
            sb.append("\n").append(escape(snippet)).append("\n");
        }

        if (!matches.isEmpty()) {
            sb.append("\n<b>Matched rules:</b>\n");
            for (JobRuleMatch m : matches) {
                String icon = m.getWeight() > 0 ? "✅" : "❌";
                String label = m.getRule() != null ? escape(m.getRule().getKeyword()) : "rule#" + m.getId();
                sb.append(icon).append(" ").append(label)
                  .append(" <i>(").append(m.getMatchedField()).append(")</i>")
                  .append(" ").append(m.getWeight() > 0 ? "+" : "").append(m.getWeight())
                  .append("\n");
            }
        }

        if (job.getRawMessage() != null && job.getRawMessage().getSourceName() != null) {
            sb.append("\n<b>Source:</b> ").append(escape(job.getRawMessage().getSourceName())).append("\n");
        }
        if (job.getUrl() != null && !job.getUrl().isBlank()) {
            sb.append("<b>Link:</b> <a href=\"").append(job.getUrl()).append("\">Open</a>\n");
        }

        return sb.toString();
    }

    /**
     * Formats an AI-qualified job notification.
     * Includes rule score, AI score, decision, matched skills, risk flags, and reason.
     */
    public String formatAiMatch(JobPost job, JobAiAnalysisResult aiResult) {
        StringBuilder sb = new StringBuilder();

        sb.append("🤖 <b>AI-Qualified Job</b>\n\n");

        if (job.getTitle() != null && !job.getTitle().isBlank()) {
            sb.append("<b>Title:</b> ").append(escape(job.getTitle())).append("\n");
        }
        if (job.getCompany() != null && !job.getCompany().isBlank()) {
            sb.append("<b>Company:</b> ").append(escape(job.getCompany())).append("\n");
        }
        if (job.getLocation() != null && !job.getLocation().isBlank()) {
            sb.append("<b>Location:</b> ").append(escape(job.getLocation())).append("\n");
        }

        sb.append("<b>Rule score:</b> ").append(job.getScore()).append("\n");
        sb.append("<b>AI score:</b> ").append(aiResult.qualificationScore()).append("/100\n");
        sb.append("<b>Decision:</b> ").append(escape(aiResult.decision())).append("\n");

        if (aiResult.matchedSkills() != null && !aiResult.matchedSkills().isEmpty()) {
            sb.append("<b>Matched skills:</b> ")
              .append(escape(String.join(", ", aiResult.matchedSkills()))).append("\n");
        }

        if (aiResult.riskFlags() != null && !aiResult.riskFlags().isEmpty()) {
            sb.append("<b>Risk flags:</b> ⚠️ ")
              .append(escape(String.join(", ", aiResult.riskFlags()))).append("\n");
        }

        if (aiResult.reason() != null && !aiResult.reason().isBlank()) {
            sb.append("\n<i>").append(escape(aiResult.reason())).append("</i>\n");
        }

        if (job.getRawMessage() != null && job.getRawMessage().getSourceName() != null) {
            sb.append("\n<b>Source:</b> ").append(escape(job.getRawMessage().getSourceName())).append("\n");
        }
        if (job.getUrl() != null && !job.getUrl().isBlank()) {
            sb.append("<b>Link:</b> <a href=\"").append(job.getUrl()).append("\">Open</a>\n");
        }

        sb.append("\n<i>Review before applying.</i>");
        return sb.toString();
    }

    /**
     * Escapes Telegram HTML special characters in user-provided text.
     */
    public static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
