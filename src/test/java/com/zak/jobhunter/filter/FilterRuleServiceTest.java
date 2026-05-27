package com.zak.jobhunter.filter;

import com.zak.jobhunter.filter.dto.CreateFilterRuleRequest;
import com.zak.jobhunter.filter.dto.FilterRuleResponse;
import com.zak.jobhunter.filter.dto.UpdateFilterRuleRequest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilterRuleServiceTest {

    @Mock private FilterRuleRepository repository;

    private TextNormalizer     normalizer;
    private FilterRuleService  service;

    @BeforeEach
    void setUp() {
        normalizer = new TextNormalizer();
        service    = new FilterRuleService(repository, normalizer);
    }

    @Test
    void findAll_returnsAll() {
        when(repository.findAll()).thenReturn(List.of(sample(1L, "Remote"), sample(2L, "Java")));
        assertThat(service.findAll()).hasSize(2);
    }

    @Test
    void findById_returnsRule() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample(1L, "Remote")));
        FilterRuleResponse response = service.findById(1L);
        assertThat(response.keyword()).isEqualTo("Remote");
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void create_normalizesKeywordAndPersists() {
        CreateFilterRuleRequest req = new CreateFilterRuleRequest(
                "Full Remote", MatchType.PHRASE, RuleField.ANY, RuleType.POSITIVE, 6, true);

        FilterRule saved = sample(10L, "Full Remote");
        saved.setNormalizedKeyword("full remote");
        when(repository.save(any())).thenReturn(saved);

        FilterRuleResponse response = service.create(req);
        assertThat(response.keyword()).isEqualTo("Full Remote");

        ArgumentCaptor<FilterRule> captor = ArgumentCaptor.forClass(FilterRule.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getNormalizedKeyword()).isEqualTo("full remote");
        assertThat(captor.getValue().getMatchType()).isEqualTo(MatchType.PHRASE);
        assertThat(captor.getValue().getWeight()).isEqualTo(6);
    }

    @Test
    void update_modifiesRule() {
        FilterRule existing = sample(1L, "Remote");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateFilterRuleRequest req = new UpdateFilterRuleRequest(
                "Fully Remote", MatchType.PHRASE, RuleField.ANY, RuleType.POSITIVE, 7, true);

        FilterRuleResponse response = service.update(1L, req);
        assertThat(response.keyword()).isEqualTo("Fully Remote");
        assertThat(response.weight()).isEqualTo(7);
    }

    @Test
    void delete_callsRepositoryDelete() {
        when(repository.existsById(1L)).thenReturn(true);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(repository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private FilterRule sample(Long id, String keyword) {
        FilterRule rule = new FilterRule();
        rule.setId(id);
        rule.setKeyword(keyword);
        rule.setNormalizedKeyword(normalizer.normalizeKeyword(keyword));
        rule.setMatchType(MatchType.WHOLE_WORD);
        rule.setField(RuleField.ANY);
        rule.setRuleType(RuleType.POSITIVE);
        rule.setWeight(5);
        rule.setEnabled(true);
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());
        return rule;
    }
}
