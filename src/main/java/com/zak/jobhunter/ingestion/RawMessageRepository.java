package com.zak.jobhunter.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RawMessageRepository extends JpaRepository<RawMessage, Long> {

    boolean existsBySourceTypeAndSourceChannelIdAndExternalMessageId(
            String sourceType, String sourceChannelId, String externalMessageId);

    Optional<RawMessage> findBySourceTypeAndSourceChannelIdAndExternalMessageId(
            String sourceType, String sourceChannelId, String externalMessageId);

    boolean existsByContentHash(String contentHash);

    List<RawMessage> findByProcessedFalseOrderByCreatedAtAsc();
}
