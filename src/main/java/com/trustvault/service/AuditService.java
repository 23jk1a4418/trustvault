package com.trustvault.service;

import com.trustvault.model.AuditEvent;
import com.trustvault.repo.AuditRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/** Writes a linked event chain. This serializes writers within one application process. */
@Service
public class AuditService {
    private final AuditRepository events;

    public AuditService(AuditRepository events) {
        this.events = events;
    }

    public synchronized void record(String actor, String action, UUID documentId, String details) {
        AuditEvent previousEvent = events.findTopByOrderByOccurredAtDesc().orElse(null);
        String previousHash = previousEvent == null
                ? "GENESIS"
                : previousEvent.getEventHash();
        Instant occurredAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (previousEvent != null && !occurredAt.isAfter(previousEvent.getOccurredAt())) {
            occurredAt = previousEvent.getOccurredAt().plus(1, ChronoUnit.MICROS);
        }
        String content = occurredAt + "|" + actor + "|" + action + "|"
                + documentId + "|" + details + "|" + previousHash;

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            String eventHash = HexFormat.of().formatHex(digest);
            events.save(new AuditEvent(
                    occurredAt, actor, action, documentId, details, previousHash, eventHash));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not record audit event", exception);
        }
    }
}
