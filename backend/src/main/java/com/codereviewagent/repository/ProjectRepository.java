package com.codereviewagent.repository;

import com.codereviewagent.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOwnerId(UUID ownerId);

    @Query("SELECT COUNT(p) FROM Project p WHERE p.owner.id = :userId")
    long countActiveProjectsByUserId(@Param("userId") UUID userId);
}
