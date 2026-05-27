package com.zak.jobhunter.filter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilterRuleRepository extends JpaRepository<FilterRule, Long> {

    List<FilterRule> findByEnabledTrue();

    List<FilterRule> findByRuleTypeAndEnabledTrue(RuleType ruleType);
}
