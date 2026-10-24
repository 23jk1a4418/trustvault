package com.trustvault.config;

import com.trustvault.model.AppUser;
import com.trustvault.model.DocumentGrant;
import com.trustvault.model.DocumentRecord;
import com.trustvault.model.Role;
import com.trustvault.repo.DocumentRepository;
import com.trustvault.repo.GrantRepository;
import com.trustvault.repo.UserRepository;
import com.trustvault.service.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seedDemoData(
            UserRepository users,
            DocumentRepository documents,
            GrantRepository grants,
            PasswordEncoder passwordEncoder,
            AuditService audit,
            @Value("${app.seed:true}") boolean enabled,
            @Value("${app.storage-dir:./uploads}") String storageDir) {
        return args -> {
            if (!enabled || users.count() > 0) {
                return;
            }

            users.save(new AppUser("admin", passwordEncoder.encode("Admin123!"), Role.ADMIN));
            users.save(new AppUser("alice", passwordEncoder.encode("Owner123!"), Role.OWNER));
            users.save(new AppUser("bob", passwordEncoder.encode("Viewer123!"), Role.VIEWER));

            Path storage = Paths.get(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(storage);
            String contents = "TrustVault sample document\n"
                    + "Private by default. Alice granted Bob read access to this demo file.\n";
            String storageKey = "trustvault-welcome-demo.blob";
            Files.writeString(storage.resolve(storageKey), contents,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            DocumentRecord sample = documents.save(new DocumentRecord(
                    "Welcome to TrustVault.txt",
                    "Getting started",
                    "alice",
                    storageKey,
                    "text/plain",
                    contents.getBytes(StandardCharsets.UTF_8).length));
            grants.save(new DocumentGrant(sample.getId(), "bob", true, false));

            audit.record("alice", "UPLOAD", sample.getId(), "seed demonstration document");
            audit.record("alice", "GRANT", sample.getId(), "user=bob,read=true,edit=false");
        };
    }
}
