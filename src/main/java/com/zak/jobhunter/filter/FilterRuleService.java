package com.zak.jobhunter.filter;

import com.zak.jobhunter.filter.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FilterRuleService {

    private final FilterRuleRepository repository;
    private final TextNormalizer normalizer;

    public List<FilterRuleResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public FilterRuleResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public FilterRuleResponse create(CreateFilterRuleRequest req) {
        FilterRule rule = FilterRule.builder()
                .keyword(req.keyword())
                .normalizedKeyword(normalizer.normalizeKeyword(req.keyword()))
                .matchType(req.matchType())
                .field(req.field())
                .ruleType(req.ruleType())
                .weight(req.weight())
                .enabled(req.enabled() != null ? req.enabled() : true)
                .build();
        return toResponse(repository.save(rule));
    }

    @Transactional
    public FilterRuleResponse update(Long id, UpdateFilterRuleRequest req) {
        FilterRule rule = getOrThrow(id);
        rule.setKeyword(req.keyword());
        rule.setNormalizedKeyword(normalizer.normalizeKeyword(req.keyword()));
        rule.setMatchType(req.matchType());
        rule.setField(req.field());
        rule.setRuleType(req.ruleType());
        rule.setWeight(req.weight());
        if (req.enabled() != null) {
            rule.setEnabled(req.enabled());
        }
        return toResponse(repository.save(rule));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Filter rule not found: " + id);
        }
        repository.deleteById(id);
    }

    private FilterRule getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Filter rule not found: " + id));
    }

    private FilterRuleResponse toResponse(FilterRule r) {
        return new FilterRuleResponse(r.getId(), r.getKeyword(), r.getNormalizedKeyword(),
                r.getMatchType(), r.getField(), r.getRuleType(), r.getWeight(), r.isEnabled(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
