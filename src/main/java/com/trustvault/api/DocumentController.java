package com.trustvault.api;

import com.trustvault.model.AppUser;
import com.trustvault.model.AuditEvent;
import com.trustvault.model.DocumentGrant;
import com.trustvault.model.DocumentRecord;
import com.trustvault.model.Role;
import com.trustvault.repo.AuditRepository;
import com.trustvault.repo.DocumentRepository;
import com.trustvault.repo.GrantRepository;
import com.trustvault.repo.UserRepository;
import com.trustvault.service.AuditService;
import com.trustvault.service.CurrentUser;
import com.trustvault.service.DocumentAccessService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class DocumentController {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final DocumentRepository documents;
    private final GrantRepository grants;
    private final UserRepository users;
    private final AuditRepository audits;
    private final DocumentAccessService access;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final Path storage;

    public DocumentController(
            DocumentRepository documents,
            GrantRepository grants,
            UserRepository users,
            AuditRepository audits,
            DocumentAccessService access,
            CurrentUser currentUser,
            AuditService audit,
            @Value("${app.storage-dir}") String storageDir) {
        this.documents = documents;
        this.grants = grants;
        this.users = users;
        this.audits = audits;
        this.access = access;
        this.currentUser = currentUser;
        this.audit = audit;
        this.storage = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        AppUser user = currentUser.get(authentication);
        return Map.of("username", user.getUsername(), "role", user.getRole().name());
    }

    @GetMapping("/documents")
    public List<Map<String, Object>> listDocuments(
            Authentication authentication,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        AppUser user = currentUser.get(authentication);
        Collection<DocumentRecord> accessible = user.getRole() == Role.ADMIN
                ? documents.findAll()
                : documents.findAll().stream()
                    .filter(document -> canList(document, user.getUsername()))
                    .toList();

        return accessible.stream()
                .filter(document -> name == null || containsIgnoreCase(document.getName(), name))
                .filter(document -> category == null || containsIgnoreCase(document.getCategory(), category))
                .filter(document -> owner == null || containsIgnoreCase(document.getOwner(), owner))
                .filter(document -> from == null || !document.getCreatedAt()
                        .atZone(ZoneOffset.UTC).toLocalDate().isBefore(from))
                .filter(document -> to == null || !document.getCreatedAt()
                        .atZone(ZoneOffset.UTC).toLocalDate().isAfter(to))
                .map(this::documentView)
                .toList();
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "General") @NotBlank String category,
            Authentication authentication) throws Exception {
        AppUser user = currentUser.get(authentication);
        if (user.getRole() == Role.VIEWER) {
            throw new AccessDeniedException("Viewers cannot upload documents");
        }
        validateUpload(file);

        Files.createDirectories(storage);
        UUID id = UUID.randomUUID();
        String storageKey = id + ".blob";
        Files.write(storage.resolve(storageKey), file.getBytes(), StandardOpenOption.CREATE_NEW);

        String displayName = Path.of(Objects.requireNonNullElse(file.getOriginalFilename(), "document"))
                .getFileName().toString();
        DocumentRecord document = new DocumentRecord(
                displayName, category, user.getUsername(), storageKey,
                file.getContentType(), file.getSize());
        document = documents.save(document);
        audit.record(user.getUsername(), "UPLOAD", document.getId(), "name=" + document.getName());
        return documentView(document);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<ByteArrayResource> download(
            @PathVariable UUID id,
            Authentication authentication) throws Exception {
        DocumentRecord document = access.requireRead(id, authentication.getName());
        byte[] content = Files.readAllBytes(storage.resolve(document.getStorageKey()).normalize());
        audit.record(authentication.getName(), "DOWNLOAD", id, "size=" + content.length);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + safeFilename(document.getName()) + "\"")
                .body(new ByteArrayResource(content));
    }

    @PutMapping("/documents/{id}")
    public Map<String, Object> rename(
            @PathVariable UUID id,
            @RequestBody RenameRequest request,
            Authentication authentication) {
        DocumentRecord document = access.requireEdit(id, authentication.getName());
        if (request.name() == null || request.name().isBlank() || request.name().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required (max 200 characters)");
        }

        document.setName(request.name().trim());
        document = documents.save(document);
        audit.record(authentication.getName(), "UPDATE", id, "rename=" + document.getName());
        return documentView(document);
    }

    @DeleteMapping("/documents/{id}")
    public void delete(@PathVariable UUID id, Authentication authentication) throws Exception {
        DocumentRecord document = access.requireOwner(id, authentication.getName());
        Files.deleteIfExists(storage.resolve(document.getStorageKey()).normalize());
        grants.deleteByDocumentId(id);
        documents.delete(document);
        audit.record(authentication.getName(), "DELETE", id, "document deleted");
    }

    @PutMapping("/documents/{id}/permissions")
    public Map<String, String> grant(
            @PathVariable UUID id,
            @RequestBody GrantRequest request,
            Authentication authentication) {
        access.requireOwner(id, authentication.getName());
        if (request.username() == null || request.username().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose another named user");
        }
        if (users.findByUsername(request.username()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        boolean canEdit = request.edit() && request.read();
        grants.save(new DocumentGrant(id, request.username(), request.read(), canEdit));
        audit.record(authentication.getName(), "GRANT", id,
                "user=" + request.username() + ",read=" + request.read() + ",edit=" + canEdit);
        return Map.of("status", "granted");
    }

    @DeleteMapping("/documents/{id}/permissions/{username}")
    public void revoke(
            @PathVariable UUID id,
            @PathVariable String username,
            Authentication authentication) {
        access.requireOwner(id, authentication.getName());
        grants.deleteByDocumentIdAndUsername(id, username);
        audit.record(authentication.getName(), "REVOKE", id, "user=" + username);
    }

    @GetMapping("/documents/{id}/permissions")
    public List<Map<String, Object>> permissions(@PathVariable UUID id, Authentication authentication) {
        access.requireOwner(id, authentication.getName());
        return grants.findByDocumentId(id).stream()
                .map(grant -> Map.<String, Object>of(
                        "username", grant.getUsername(),
                        "read", grant.canRead(),
                        "edit", grant.canEdit()))
                .toList();
    }

    @GetMapping("/admin/audit")
    public List<AuditEvent> adminAudit(Authentication authentication) {
        requireAdministrator(authentication);
        return audits.findAllByOrderByOccurredAtDesc();
    }

    @GetMapping("/admin/documents")
    public List<Map<String, Object>> adminDocuments(Authentication authentication) {
        requireAdministrator(authentication);
        return documents.findAll().stream().map(this::documentView).toList();
    }

    private boolean canList(DocumentRecord document, String username) {
        return document.getOwner().equals(username)
                || grants.findByDocumentIdAndUsername(document.getId(), username)
                    .map(DocumentGrant::canRead).orElse(false);
    }

    private void validateUpload(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be between 1 byte and 10 MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File type is not allowed");
        }
    }

    private void requireAdministrator(Authentication authentication) {
        if (currentUser.get(authentication).getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Administrator role required");
        }
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value.toLowerCase().contains(search.toLowerCase());
    }

    private String safeFilename(String filename) {
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    private Map<String, Object> documentView(DocumentRecord document) {
        return Map.of(
                "id", document.getId(),
                "name", document.getName(),
                "category", document.getCategory(),
                "owner", document.getOwner(),
                "contentType", document.getContentType(),
                "size", document.getSize(),
                "createdAt", document.getCreatedAt());
    }

    public record RenameRequest(String name) { }
    public record GrantRequest(String username, boolean read, boolean edit) { }
}
