package com.trustvault.repo;
import com.trustvault.model.AuditEvent; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface AuditRepository extends JpaRepository<AuditEvent,UUID>{List<AuditEvent> findAllByOrderByOccurredAtDesc(); Optional<AuditEvent> findTopByOrderByOccurredAtDesc();}
