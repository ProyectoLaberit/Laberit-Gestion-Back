package com.example.demo.repository;

import com.example.demo.entity.TareaProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
