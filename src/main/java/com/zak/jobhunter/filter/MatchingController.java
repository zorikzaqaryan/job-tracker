package com.zak.jobhunter.filter;

import com.zak.jobhunter.filter.dto.MatchTestRequest;
import com.zak.jobhunter.filter.dto.MatchTestResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
@Tag(name = "Matching", description = "Test the matching engine against current filter rules")
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/test")
    @Operation(summary = "Dry-run matching",
            description = "Evaluates the current filter rules against a hypothetical job payload. " +
                    "Nothing is persisted.")
    public MatchTestResponse test(@RequestBody MatchTestRequest req) {
        MatchingService.MatchResult result = matchingService.evaluate(
                req.title(), req.description(), req.location());
        int threshold = matchingService.getThreshold();
        return new MatchTestResponse(
                result.isMatched(threshold),
                result.score(),
                threshold,
                result.matchedRules());
    }
}
