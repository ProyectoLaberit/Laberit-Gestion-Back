package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
   /**
     * Recupera todos los detalles asociados a un ID de Excel.
     * Es la base para mostrar la tabla en el Frontend.
     */
    List<DetalleEstimacion> findByIdExcel(Integer idExcel);

    /**
     * Busca un detalle específico. Útil para la búsqueda puntual que comentamos.
     */
    DetalleEstimacion findFirstByIdExcelAndIdFaseAndTareaIgnoreCase(Integer idExcel, Integer idFase, String tarea);
}