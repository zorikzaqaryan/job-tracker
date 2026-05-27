package com.zak.jobhunter.filter;

import com.zak.jobhunter.filter.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filter-rules")
@RequiredArgsConstructor
@Tag(name = "Filter Rules", description = "Keyword filter rule management")
public class FilterRuleController {

    private final FilterRuleService service;

    @GetMapping
    @Operation(summary = "List all filter rules")
    public List<FilterRuleResponse> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get filter rule by ID")
    public FilterRuleResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new filter rule")
    public FilterRuleResponse create(@Valid @RequestBody CreateFilterRuleRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a filter rule")
    public FilterRuleResponse update(@PathVariable Long id, @Valid @RequestBody UpdateFilterRuleRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a filter rule")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
