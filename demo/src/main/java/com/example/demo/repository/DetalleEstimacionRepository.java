package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
List<DetalleEstimacion> findByIdExcel(Integer idExcel);

    List<DetalleEstimacion> findByIdTareaProyecto(Long idTareaProyecto);
    
    DetalleEstimacion findFirstByIdExcelAndIdTareaProyecto(Integer idExcel, Long idTareaProyecto);
}