package com.zak.jobhunter.channel;

import com.zak.jobhunter.channel.dto.ChannelResponse;
import com.zak.jobhunter.channel.dto.CreateChannelRequest;
import com.zak.jobhunter.channel.dto.UpdateChannelRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelService {

    private final JobSourceRepository repository;

    public List<ChannelResponse> findAll(SourceType sourceType, Boolean enabled) {
        List<JobSource> sources;
        if (sourceType != null && Boolean.TRUE.equals(enabled)) {
            sources = repository.findBySourceTypeAndEnabledTrue(sourceType);
        } else if (sourceType != null) {
            sources = repository.findBySourceType(sourceType);
        } else if (Boolean.TRUE.equals(enabled)) {
            sources = repository.findByEnabledTrue();
        } else {
            sources = repository.findAll();
        }
        return sources.stream().map(this::toResponse).toList();
    }

    public ChannelResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ChannelResponse create(CreateChannelRequest req) {
        JobSource source = JobSource.builder()
                .sourceType(req.sourceType())
                .name(req.name())
                .telegramUsername(req.telegramUsername())
                .telegramChannelId(req.telegramChannelId())
                .url(req.url())
                .enabled(req.enabled() != null ? req.enabled() : true)
                .build();
        return toResponse(repository.save(source));
    }

    @Transactional
    public ChannelResponse update(Long id, UpdateChannelRequest req) {
        JobSource source = getOrThrow(id);
        source.setSourceType(req.sourceType());
        source.setName(req.name());
        source.setTelegramUsername(req.telegramUsername());
        source.setTelegramChannelId(req.telegramChannelId());
        source.setUrl(req.url());
        if (req.enabled() != null) {
            source.setEnabled(req.enabled());
        }
        return toResponse(repository.save(source));
    }

    @Transactional
    public ChannelResponse setEnabled(Long id, boolean enabled) {
        JobSource source = getOrThrow(id);
        source.setEnabled(enabled);
        return toResponse(repository.save(source));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Channel not found: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional
    public void updateLastMessageId(Long sourceId, String externalMessageId) {
        repository.findById(sourceId).ifPresent(s -> {
            s.setLastExternalMessageId(externalMessageId);
            repository.save(s);
        });
    }

    private JobSource getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Channel not found: " + id));
    }

    private ChannelResponse toResponse(JobSource s) {
        return new ChannelResponse(
                s.getId(), s.getSourceType(), s.getName(),
                s.getTelegramUsername(), s.getTelegramChannelId(),
                s.getUrl(), s.isEnabled(), s.getLastExternalMessageId(),
                s.getCreatedAt(), s.getUpdatedAt());
    }
}
