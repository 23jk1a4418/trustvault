package com.trustvault.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="documents")
public class DocumentRecord {
    @Id private UUID id = UUID.randomUUID();
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String category;
    @Column(nullable=false) private String owner;
    @Column(nullable=false) private String storageKey;
    @Column(nullable=false) private String contentType;
    private long size;
    @Column(nullable=false) private Instant createdAt = Instant.now();
    protected DocumentRecord() {}
    public DocumentRecord(String name,String category,String owner,String storageKey,String contentType,long size){this.name=name;this.category=category;this.owner=owner;this.storageKey=storageKey;this.contentType=contentType;this.size=size;}
    public UUID getId(){return id;} public String getName(){return name;} public void setName(String name){this.name=name;} public String getCategory(){return category;} public String getOwner(){return owner;} public String getStorageKey(){return storageKey;} public String getContentType(){return contentType;} public long getSize(){return size;} public Instant getCreatedAt(){return createdAt;}
}
