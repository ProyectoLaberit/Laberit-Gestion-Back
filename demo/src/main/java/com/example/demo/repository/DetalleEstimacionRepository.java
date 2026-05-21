package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Proyecto;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
List<DetalleEstimacion> findByIdExcel(Integer idExcel);

    List<DetalleEstimacion> findByIdTareaProyecto(Long idTareaProyecto);
    
    DetalleEstimacion findFirstByIdExcelAndIdTareaProyecto(Integer idExcel, Long idTareaProyecto);
}