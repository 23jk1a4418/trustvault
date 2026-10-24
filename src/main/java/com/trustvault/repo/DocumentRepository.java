package com.trustvault.repo;
import com.trustvault.model.DocumentRecord; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface DocumentRepository extends JpaRepository<DocumentRecord,UUID>{List<DocumentRecord> findByOwnerOrderByCreatedAtDesc(String owner);}
