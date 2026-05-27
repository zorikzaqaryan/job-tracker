package com.zak.jobhunter.candidate;

import com.zak.jobhunter.candidate.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidate")
@RequiredArgsConstructor
@Tag(name = "Candidate Profile", description = "Manage the candidate profile used for AI job qualification")
public class CandidateController {

    private final CandidateService candidateService;

    @GetMapping("/profiles")
    @Operation(summary = "List all candidate profiles")
    public List<CandidateProfileResponse> listProfiles() {
        return candidateService.findAll();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get the most recent candidate profile")
    public CandidateProfileResponse getProfile() {
        List<CandidateProfileResponse> all = candidateService.findAll();
        if (all.isEmpty()) throw new jakarta.persistence.EntityNotFoundException("No candidate profile found");
        return all.get(0);
    }

    @PostMapping("/profile")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new candidate profile")
    public CandidateProfileResponse createProfile(@RequestBody CandidateProfileRequest request) {
        return candidateService.create(request);
    }

    @PutMapping("/profile/{id}")
    @Operation(summary = "Update a candidate profile")
    public CandidateProfileResponse updateProfile(@PathVariable Long id,
                                                   @RequestBody CandidateProfileRequest request) {
        return candidateService.update(id, request);
    }

    @GetMapping("/profile/{id}/skills")
    @Operation(summary = "List skills for a candidate profile")
    public List<CandidateSkillDto> listSkills(@PathVariable Long id) {
        return candidateService.findSkills(id);
    }

    @PostMapping("/profile/{id}/skills")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a skill to a candidate profile")
    public CandidateSkillDto addSkill(@PathVariable Long id,
                                       @Valid @RequestBody CandidateSkillDto dto) {
        return candidateService.addSkill(id, dto);
    }

    @DeleteMapping("/skills/{skillId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a candidate skill")
    public void deleteSkill(@PathVariable Long skillId) {
        candidateService.deleteSkill(skillId);
    }
}
