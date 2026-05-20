package com.example.demo.repository;

import com.example.demo.entity.GitLabTarea;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GitLabTareaRepository extends JpaRepository<GitLabTarea, Long> { // <-- Cambiado a Long

    /**
     * Busca una tarea vinculada utilizando el ID único global que viene de GitLab.
     * Esto nos servirá para saber si la issue ya está registrada en Neon y
     * actualizarla.
     */
    Optional<GitLabTarea> findByIssueId(String issueId);
}