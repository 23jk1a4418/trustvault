package com.trustvault;

import com.trustvault.model.DocumentGrant;
import com.trustvault.model.DocumentRecord;
import com.trustvault.repo.AuditRepository;
import com.trustvault.repo.DocumentRepository;
import com.trustvault.repo.GrantRepository;
import com.trustvault.service.DocumentAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trustvault-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DocumentAuthorizationTest {
    @Autowired private DocumentRepository documents;
    @Autowired private GrantRepository grants;
    @Autowired private AuditRepository audit;
    @Autowired private DocumentAccessService access;
    @Autowired private MockMvc mvc;

    @Test
    void ungrantedUserCannotReadAnotherOwnersDocument() {
        DocumentRecord document = saveDocument("private.txt");

        assertThrows(AccessDeniedException.class,
                () -> access.requireRead(document.getId(), "bob"));

        documents.delete(document);
    }

    @Test
    void grantedViewerCanReadButCannotEditEvenWithAnEditFlag() {
        DocumentRecord document = saveDocument("shared.txt");
        grants.save(new DocumentGrant(document.getId(), "bob", true, true));

        assertEquals(document.getId(), access.requireRead(document.getId(), "bob").getId());
        assertThrows(AccessDeniedException.class,
                () -> access.requireEdit(document.getId(), "bob"));

        grants.deleteByDocumentId(document.getId());
        documents.delete(document);
    }

    @Test
    void revokedGrantIsDeniedOnTheNextRequest() {
        DocumentRecord document = saveDocument("revoked.txt");
        grants.save(new DocumentGrant(document.getId(), "bob", true, false));
        assertDoesNotThrow(() -> access.requireRead(document.getId(), "bob"));

        grants.deleteByDocumentIdAndUsername(document.getId(), "bob");

        assertThrows(AccessDeniedException.class,
                () -> access.requireRead(document.getId(), "bob"));
        documents.delete(document);
    }

    @Test
    void directDownloadRejectsUninvitedViewerAndAuditsTheDenial() throws Exception {
        DocumentRecord document = saveDocument("private-route.txt");

        mvc.perform(get("/api/documents/{id}/download", document.getId())
                        .with(httpBasic("bob", "Viewer123!")))
                .andExpect(status().isForbidden());

        assertTrue(audit.findAllByOrderByOccurredAtDesc().stream()
                .anyMatch(event -> "ACCESS_DENIED".equals(event.getAction())
                        && document.getId().equals(event.getDocumentId())));
        documents.delete(document);
    }

    @Test
    void administratorCannotDownloadWithoutAnExplicitGrant() throws Exception {
        DocumentRecord document = saveDocument("admin-route.txt");

        mvc.perform(get("/api/documents/{id}/download", document.getId())
                        .with(httpBasic("admin", "Admin123!")))
                .andExpect(status().isForbidden());

        documents.delete(document);
    }

    @Test
    void auditEventsMatchTheirStoredHashChain() throws Exception {
        var events = audit.findAllByOrderByOccurredAtDesc();
        Collections.reverse(events);
        String expectedPreviousHash = "GENESIS";

        for (var event : events) {
            assertEquals(expectedPreviousHash, event.getPreviousHash());
            String content = event.getOccurredAt() + "|" + event.getActor() + "|"
                    + event.getAction() + "|" + event.getDocumentId() + "|"
                    + event.getDetails() + "|" + event.getPreviousHash();
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(content.getBytes(StandardCharsets.UTF_8));
            expectedPreviousHash = HexFormat.of().formatHex(digest);
            assertEquals(expectedPreviousHash, event.getEventHash());
        }
    }

    private DocumentRecord saveDocument(String name) {
        return documents.save(new DocumentRecord(
                name, "General", "alice", "test.blob", "text/plain", 1));
    }
}
