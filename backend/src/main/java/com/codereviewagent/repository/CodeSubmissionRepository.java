package com.codereviewagent.repository;

import com.codereviewagent.entity.CodeSubmission;
import com.codereviewagent.entity.enums.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, UUID> {
    List<CodeSubmission> findByProjectId(UUID projectId);
    List<CodeSubmission> findByAuthorId(UUID authorId);
    List<CodeSubmission> findByStatus(SubmissionStatus status);

    @Query("SELECT DISTINCT c.language FROM CodeSubmission c WHERE c.author.id = :authorId AND c.language IS NOT NULL")
    List<String> findDistinctLanguagesByAuthorId(@Param("authorId") UUID authorId);

    @Query("SELECT DISTINCT c.language FROM CodeSubmission c WHERE c.language IS NOT NULL")
    List<String> findAllDistinctLanguages();
}
