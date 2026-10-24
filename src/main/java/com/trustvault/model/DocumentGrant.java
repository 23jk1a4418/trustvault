package com.trustvault.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="document_grants", uniqueConstraints=@UniqueConstraint(columnNames={"documentId","username"}))
public class DocumentGrant {
    @Id private UUID id=UUID.randomUUID();
    @Column(nullable=false) private UUID documentId;
    @Column(nullable=false) private String username;
    @Column(nullable=false) private boolean canRead;
    @Column(nullable=false) private boolean canEdit;
    protected DocumentGrant(){}
    public DocumentGrant(UUID documentId,String username,boolean canRead,boolean canEdit){this.documentId=documentId;this.username=username;this.canRead=canRead;this.canEdit=canEdit;}
    public UUID getDocumentId(){return documentId;} public String getUsername(){return username;} public boolean canRead(){return canRead;} public boolean canEdit(){return canEdit;}
}
