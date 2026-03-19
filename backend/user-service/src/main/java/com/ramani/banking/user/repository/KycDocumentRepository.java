package com.ramani.banking.user.repository;

import com.ramani.banking.user.entity.KycDocument;
import com.ramani.banking.user.entity.KycStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {
    List<KycDocument> findByUserId(UUID userId);
    List<KycDocument> findByStatus(KycStatus status);
    boolean existsByUserIdAndStatus(UUID userId, KycStatus status);
}
