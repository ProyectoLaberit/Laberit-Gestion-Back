package com.example.demo.repository;

import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.entity.TareaProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface TareaProyectoRepository extends JpaRepository<TareaProyecto, Long> {
    
    Optional<TareaProyecto> findByIdProyectoAndIdFaseAndIdDepartamentoAndTarea(
            Long idProyecto, Integer idFase, Integer idDepartamento, String tarea
    );

    // método para sacar todas las tareas de un proyecto
    List<TareaProyecto> findByIdProyecto(Long idProyecto);

    List<TareaProyecto> findByTarea(String tarea);

    List<TareaProyecto> findByIdProyectoAndIdFaseAndTarea(Long idProyecto, Integer idFase, String tarea);

    ///Concultas para el Excel Analítico
    

    @Query("SELECT new com.example.demo.dto.excel.FilaComparativaTareaDTO(" +
           "g.issueId, f.nombre, t.nombre, d.nombre, " +
           "COALESCE(e.tiempoMin, 0.0), COALESCE(e.tiempoMax, 0.0), " +
           "COALESCE(SUM(c.horasTrabajadas), 0.0), " +
           "(COALESCE(SUM(c.horasTrabajadas), 0.0) - COALESCE(e.tiempoMax, 0.0)), " +
           "CASE WHEN COALESCE(e.tiempoMax, 0.0) > 0 THEN ((COALESCE(SUM(c.horasTrabajadas), 0.0) - e.tiempoMax) / e.tiempoMax) * 100 ELSE 0.0 END, " +
           "COALESCE(g.estado, 'Sin issue')) " +
           "FROM TareaProyecto t " +
           "JOIN t.fase f " +
           "JOIN t.departamento d " +
           "LEFT JOIN DetalleEstimacion e ON e.tareaProyecto.id = t.id AND e.excel.vigente = true " +
           "LEFT JOIN ImputacionClockify c ON c.tareaProyecto.id = t.id AND c.valida = true " +
           "LEFT JOIN GitLabTarea g ON g.tareaProyecto.id = t.id " +
           "WHERE t.proyecto.id = :idProyecto " +
           "GROUP BY t.id, g.issueId, f.nombre, d.nombre, e.tiempoMin, e.tiempoMax, g.estado")
    List<FilaComparativaDTO> obtenerComparativaTareas(@Param("idProyecto") Long idProyecto);
}
