package com.trustvault.service;

import com.trustvault.model.AppUser;
import com.trustvault.model.DocumentRecord;
import com.trustvault.model.Role;
import com.trustvault.repo.DocumentRepository;
import com.trustvault.repo.GrantRepository;
import com.trustvault.repo.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Central place for the document access decision. Every file route uses this service. */
@Service
public class DocumentAccessService {
    private final DocumentRepository documents;
    private final GrantRepository grants;
    private final UserRepository users;
    private final AuditService audit;

    public DocumentAccessService(
            DocumentRepository documents,
            GrantRepository grants,
            UserRepository users,
            AuditService audit) {
        this.documents = documents;
        this.grants = grants;
        this.users = users;
        this.audit = audit;
    }

    public DocumentRecord requireRead(UUID documentId, String username) {
        return requireGrant(documentId, username, false);
    }

    public DocumentRecord requireEdit(UUID documentId, String username) {
        return requireGrant(documentId, username, true);
    }

    public DocumentRecord requireOwner(UUID documentId, String username) {
        DocumentRecord document = findDocument(documentId);
        if (!document.getOwner().equals(username)) {
            deny(documentId, username, "OWNER_REQUIRED");
        }
        return document;
    }

    private DocumentRecord requireGrant(UUID documentId, String username, boolean editRequired) {
        DocumentRecord document = findDocument(documentId);
        if (document.getOwner().equals(username)) {
            return document;
        }

        var grant = grants.findByDocumentIdAndUsername(documentId, username);
        Role role = users.findByUsername(username)
                .map(AppUser::getRole)
                .orElse(Role.VIEWER);
        boolean hasRead = grant.isPresent() && grant.get().canRead();
        boolean hasEdit = grant.isPresent() && grant.get().canEdit();
        boolean viewerTryingToEdit = role == Role.VIEWER && editRequired;

        if (hasRead && (!editRequired || (hasEdit && !viewerTryingToEdit))) {
            return document;
        }

        deny(documentId, username, editRequired ? "EDIT_DENIED" : "READ_DENIED");
        throw new IllegalStateException("Access denial must stop request processing");
    }

    private DocumentRecord findDocument(UUID documentId) {
        return documents.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    private void deny(UUID documentId, String username, String reason) {
        audit.record(username, "ACCESS_DENIED", documentId, reason);
        throw new AccessDeniedException("You do not have access to this document");
    }
}
