package com.example.demo.repository;

import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.Proyecto;

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

    List<GitLabTarea> findByValidaAndIdProyecto_Id(Boolean valida, Long idProyecto);

    List<GitLabTarea> findByIdProyecto(Proyecto idProyecto);

    @Query("SELECT g.numeroGitLab FROM GitLabTarea g WHERE g.tareaProyecto.idTareaProyecto = :idTareaProyecto")
    Long findNumeroGitLabByTareaProyectoId(@Param("idTareaProyecto") Long idTareaProyecto);

    @Query(value = "SELECT COUNT(*) FROM tarea_gitlab g " +
            "JOIN tarea_proyecto t ON g.id_tarea_proyecto = t.id_tarea_proyecto " +
            "WHERE t.id_proyecto = :idProyecto", nativeQuery = true)
    int contarTareasVinculadasPorProyecto(@Param("idProyecto") Long idProyecto);

    List<GitLabTarea> findByValidaTrueAndIdProyecto(Proyecto idProyecto);

}
