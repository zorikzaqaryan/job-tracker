package com.zak.jobhunter.channel;

import com.zak.jobhunter.channel.dto.ChannelResponse;
import com.zak.jobhunter.channel.dto.CreateChannelRequest;
import com.zak.jobhunter.channel.dto.UpdateChannelRequest;
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
class ChannelServiceTest {

    @Mock private JobSourceRepository repository;

    private ChannelService service;

    @BeforeEach
    void setUp() {
        service = new ChannelService(repository);
    }

    @Test
    void findAll_returnsAllChannels() {
        when(repository.findAll()).thenReturn(List.of(sample(1L, "Channel A"), sample(2L, "Channel B")));
        List<ChannelResponse> result = service.findAll(null, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void findAll_filtersBySourceTypeAndEnabled() {
        when(repository.findBySourceTypeAndEnabledTrue(SourceType.TELEGRAM))
                .thenReturn(List.of(sample(1L, "Telegram Channel")));
        List<ChannelResponse> result = service.findAll(SourceType.TELEGRAM, true);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).sourceType()).isEqualTo(SourceType.TELEGRAM);
    }

    @Test
    void findById_returnsChannel() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample(1L, "Test Channel")));
        ChannelResponse response = service.findById(1L);
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Test Channel");
    }

    @Test
    void findById_throwsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_persistsAndReturnsChannel() {
        CreateChannelRequest req = new CreateChannelRequest(
                SourceType.TELEGRAM, "New Channel", "new_channel", "-100999", null, true);

        JobSource saved = sample(5L, "New Channel");
        when(repository.save(any())).thenReturn(saved);

        ChannelResponse response = service.create(req);
        assertThat(response.id()).isEqualTo(5L);

        ArgumentCaptor<JobSource> captor = ArgumentCaptor.forClass(JobSource.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Channel");
        assertThat(captor.getValue().getSourceType()).isEqualTo(SourceType.TELEGRAM);
    }

    @Test
    void update_modifiesChannel() {
        JobSource existing = sample(1L, "Old Name");
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateChannelRequest req = new UpdateChannelRequest(
                SourceType.TELEGRAM, "New Name", "new_user", "-100new", null, true);

        ChannelResponse response = service.update(1L, req);
        assertThat(response.name()).isEqualTo("New Name");
    }

    @Test
    void setEnabled_disablesChannel() {
        JobSource existing = sample(1L, "Active Channel");
        existing.setEnabled(true);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChannelResponse response = service.setEnabled(1L, false);
        assertThat(response.enabled()).isFalse();
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

    private JobSource sample(Long id, String name) {
        return JobSource.builder()
                .id(id)
                .sourceType(SourceType.TELEGRAM)
                .name(name)
                .telegramUsername("test_channel")
                .telegramChannelId("-100123")
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
