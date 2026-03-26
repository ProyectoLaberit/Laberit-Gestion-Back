package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
    // Este método buscará automáticamente todas las tareas 
    // que tengan el mismo id_proyecto
    List<DetalleEstimacion> findByIdProyecto(Long idProyecto);
}