package com.specflux.prd.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for PrdDocument entity. */
public interface PrdDocumentRepository extends JpaRepository<PrdDocument, Long> {

  List<PrdDocument> findByPrdIdOrderByOrderIndexAscIdAsc(Long prdId);

  Optional<PrdDocument> findByPrdIdAndFilePath(Long prdId, String filePath);

  Optional<PrdDocument> findByIdAndPrdId(Long id, Long prdId);

  int countByPrdId(Long prdId);
}
