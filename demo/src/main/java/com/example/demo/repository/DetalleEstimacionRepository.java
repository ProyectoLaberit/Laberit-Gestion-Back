package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
    // Este método buscará automáticamente todas las tareas 
    // que tengan el mismo id_proyecto
    List<DetalleEstimacion> findByIdExcel(Integer idExcel);
/*
    // Este método buscará automáticamente la primera tarea que coincida con los tres criterios
*/
    DetalleEstimacion findFirstByIdExcelAndIdFaseAndTareaIgnoreCase(Integer idExcel, Integer idFase, String tarea);
}