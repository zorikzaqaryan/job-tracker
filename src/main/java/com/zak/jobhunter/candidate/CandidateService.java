package com.zak.jobhunter.candidate;

import com.zak.jobhunter.candidate.dto.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidateService {

    private final CandidateProfileRepository    profileRepository;
    private final CandidateSkillRepository      skillRepository;
    private final CandidateExperienceRepository experienceRepository;

    public List<CandidateProfileResponse> findAll() {
        return profileRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CandidateProfileResponse findById(Long id) {
        return toResponse(getOrThrow(id));
    }

    public CandidateProfile findMostRecent() {
        return profileRepository.findMostRecent().orElse(null);
    }

    @Transactional
    public CandidateProfileResponse create(CandidateProfileRequest req) {
        CandidateProfile profile = CandidateProfile.builder()
                .name(req.name())
                .surname(req.surname())
                .email(req.email())
                .phoneNumber(req.phoneNumber())
                .location(req.location())
                .linkedinUrl(req.linkedinUrl())
                .githubUrl(req.githubUrl())
                .portfolioUrl(req.portfolioUrl())
                .currentTitle(req.currentTitle())
                .yearsOfExperience(req.yearsOfExperience())
                .summary(req.summary())
                .preferredJobTitles(req.preferredJobTitles())
                .preferredLocations(req.preferredLocations())
                .preferredWorkModes(req.preferredWorkModes())
                .avoidRules(req.avoidRules())
                .build();

        CandidateProfile saved = profileRepository.save(profile);
        applySkills(saved, req.skills());
        applyExperiences(saved, req.experiences());
        return toResponse(profileRepository.save(saved));
    }

    @Transactional
    public CandidateProfileResponse update(Long id, CandidateProfileRequest req) {
        CandidateProfile profile = getOrThrow(id);
        profile.setName(req.name());
        profile.setSurname(req.surname());
        profile.setEmail(req.email());
        profile.setPhoneNumber(req.phoneNumber());
        profile.setLocation(req.location());
        profile.setLinkedinUrl(req.linkedinUrl());
        profile.setGithubUrl(req.githubUrl());
        profile.setPortfolioUrl(req.portfolioUrl());
        profile.setCurrentTitle(req.currentTitle());
        profile.setYearsOfExperience(req.yearsOfExperience());
        profile.setSummary(req.summary());
        profile.setPreferredJobTitles(req.preferredJobTitles());
        profile.setPreferredLocations(req.preferredLocations());
        profile.setPreferredWorkModes(req.preferredWorkModes());
        profile.setAvoidRules(req.avoidRules());

        profile.getSkills().clear();
        profile.getExperiences().clear();
        applySkills(profile, req.skills());
        applyExperiences(profile, req.experiences());
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public CandidateSkillDto addSkill(Long profileId, CandidateSkillDto dto) {
        CandidateProfile profile = getOrThrow(profileId);
        CandidateSkill skill = CandidateSkill.builder()
                .candidate(profile)
                .skillName(dto.skillName())
                .level(dto.level())
                .yearsOfExperience(dto.yearsOfExperience())
                .build();
        return toSkillDto(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(Long skillId) {
        skillRepository.deleteById(skillId);
    }

    public List<CandidateSkillDto> findSkills(Long profileId) {
        return skillRepository.findByCandidateIdOrderBySkillName(profileId)
                .stream().map(this::toSkillDto).toList();
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private void applySkills(CandidateProfile profile, List<CandidateSkillDto> dtos) {
        if (dtos == null) return;
        dtos.forEach(dto -> {
            CandidateSkill s = CandidateSkill.builder()
                    .candidate(profile)
                    .skillName(dto.skillName())
                    .level(dto.level())
                    .yearsOfExperience(dto.yearsOfExperience())
                    .build();
            profile.getSkills().add(s);
        });
    }

    private void applyExperiences(CandidateProfile profile, List<CandidateExperienceDto> dtos) {
        if (dtos == null) return;
        dtos.forEach(dto -> {
            CandidateExperience e = CandidateExperience.builder()
                    .candidate(profile)
                    .company(dto.company())
                    .title(dto.title())
                    .startDate(dto.startDate())
                    .endDate(dto.endDate())
                    .description(dto.description())
                    .technologies(dto.technologies())
                    .build();
            profile.getExperiences().add(e);
        });
    }

    private CandidateProfile getOrThrow(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found: " + id));
    }

    private CandidateProfileResponse toResponse(CandidateProfile p) {
        List<CandidateSkillDto> skills = p.getSkills().stream().map(this::toSkillDto).toList();
        List<CandidateExperienceDto> exps = p.getExperiences().stream().map(this::toExpDto).toList();
        return new CandidateProfileResponse(
                p.getId(), p.getName(), p.getSurname(), p.getCurrentTitle(),
                p.getLocation(), p.getYearsOfExperience(), p.getSummary(),
                p.getPreferredJobTitles(), p.getPreferredLocations(), p.getPreferredWorkModes(),
                p.getAvoidRules(), p.getLinkedinUrl(), p.getGithubUrl(), p.getPortfolioUrl(),
                skills, exps, p.getCreatedAt(), p.getUpdatedAt());
    }

    private CandidateSkillDto toSkillDto(CandidateSkill s) {
        return new CandidateSkillDto(s.getId(), s.getSkillName(), s.getLevel(), s.getYearsOfExperience());
    }

    private CandidateExperienceDto toExpDto(CandidateExperience e) {
        return new CandidateExperienceDto(e.getId(), e.getCompany(), e.getTitle(),
                e.getStartDate(), e.getEndDate(), e.getDescription(), e.getTechnologies());
    }
}
