package com.zak.jobhunter.channel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSourceRepository extends JpaRepository<JobSource, Long> {

    List<JobSource> findByEnabledTrue();

    List<JobSource> findBySourceTypeAndEnabledTrue(SourceType sourceType);

    List<JobSource> findBySourceType(SourceType sourceType);

    boolean existsBySourceTypeAndTelegramChannelId(SourceType sourceType, String telegramChannelId);
}
