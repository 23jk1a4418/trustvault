package com.trustvault.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="audit_events")
public class AuditEvent {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false) private Instant occurredAt=Instant.now();
    @Column(nullable=false) private String actor;
    @Column(nullable=false) private String action;
    private UUID documentId;
    @Column(length=1000) private String details;
    @Column(nullable=false,length=64) private String previousHash;
    @Column(nullable=false,length=64) private String eventHash;
    protected AuditEvent(){}
    public AuditEvent(Instant occurredAt,String actor,String action,UUID documentId,String details,String previousHash,String eventHash){this.occurredAt=occurredAt;this.actor=actor;this.action=action;this.documentId=documentId;this.details=details;this.previousHash=previousHash;this.eventHash=eventHash;}
    public Instant getOccurredAt(){return occurredAt;} public String getActor(){return actor;} public String getAction(){return action;} public UUID getDocumentId(){return documentId;} public String getDetails(){return details;} public String getPreviousHash(){return previousHash;} public String getEventHash(){return eventHash;}
}
