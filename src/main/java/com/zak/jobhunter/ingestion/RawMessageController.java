package com.zak.jobhunter.ingestion;

import com.zak.jobhunter.ingestion.dto.RawMessageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/raw-messages")
@RequiredArgsConstructor
@Tag(name = "Raw Messages", description = "Ingest raw job messages from any source")
public class RawMessageController {

    private final RawMessageService service;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Ingest a raw message",
            description = "Stores the message and publishes it to RabbitMQ for async processing. " +
                    "Duplicates are silently ignored.")
    public ResponseEntity<Map<String, Object>> ingest(@Valid @RequestBody RawMessageDto dto) {
        RawMessage saved = service.ingest(dto);
        return ResponseEntity.accepted()
                .body(Map.of("id", saved.getId(), "contentHash", saved.getContentHash()));
    }
}
