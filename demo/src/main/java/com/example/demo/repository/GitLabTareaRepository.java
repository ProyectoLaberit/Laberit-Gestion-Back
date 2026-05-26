package com.example.demo.repository;

import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.TareaProyecto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface GitLabTareaRepository extends JpaRepository<GitLabTarea, Long> { // <-- Cambiado a Long

    /**
     * Busca una tarea vinculada utilizando el ID único global que viene de GitLab.
     * Esto nos servirá para saber si la issue ya está registrada en Neon y
     * actualizarla.
     */
    Optional<GitLabTarea> findByIssueId(String issueId);

    List<GitLabTarea> findByTareaProyecto_IdTareaProyectoIn(List<Long> idsTareaProyecto);

    List<GitLabTarea> findByValidaAndTareaProyecto_IdProyecto(Boolean valida, Long idProyecto);

    @Query("SELECT g.numeroGitLab FROM GitLabTarea g WHERE g.tareaProyecto.idTareaProyecto = :idTareaProyecto")
    Long findNumeroGitLabByTareaProyectoId(@Param("idTareaProyecto") Long idTareaProyecto);

    
}
