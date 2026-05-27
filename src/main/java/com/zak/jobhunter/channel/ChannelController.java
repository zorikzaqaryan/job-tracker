package com.zak.jobhunter.channel;

import com.zak.jobhunter.channel.dto.ChannelResponse;
import com.zak.jobhunter.channel.dto.CreateChannelRequest;
import com.zak.jobhunter.channel.dto.UpdateChannelRequest;
import com.zak.jobhunter.common.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@RequiredArgsConstructor
@Tag(name = "Channels", description = "Job source channel management")
public class ChannelController {

    private final ChannelService service;

    @GetMapping
    @Operation(summary = "List all channels", description = "Optionally filter by sourceType and/or enabled flag")
    public List<ChannelResponse> list(
            @RequestParam(required = false) SourceType sourceType,
            @RequestParam(required = false) Boolean enabled) {
        return service.findAll(sourceType, enabled);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get channel by ID")
    @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ChannelResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new channel")
    public ChannelResponse create(@Valid @RequestBody CreateChannelRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing channel")
    public ChannelResponse update(@PathVariable Long id, @Valid @RequestBody UpdateChannelRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/enable")
    @Operation(summary = "Enable a channel")
    public ChannelResponse enable(@PathVariable Long id) {
        return service.setEnabled(id, true);
    }

    @PatchMapping("/{id}/disable")
    @Operation(summary = "Disable a channel")
    public ChannelResponse disable(@PathVariable Long id) {
        return service.setEnabled(id, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a channel")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
