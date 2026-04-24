package com.example.demo.repository;

import com.example.demo.entity.Proyecto;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // Necesario para findByClockifyId

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

        /**
         * Busca un proyecto por el ID único que nos da Clockify.
         * Es vital para la sincronización: si lo encuentra, actualizamos;
         * si no, creamos uno nuevo.
         */
        Optional<Proyecto> findByClockifyId(String clockifyId);
        
@Query("SELECT p FROM Proyecto p WHERE " +
            "(:activo IS NULL OR p.activo = :activo) AND " +
            "(cast(:desde as date) IS NULL OR p.fechaInicio >= :desde) AND " +
            "(cast(:hasta as date) IS NULL OR p.fechaInicio <= :hasta)")
    List<Proyecto> findByFiltrosOpcionales(
            @Param("activo") Boolean activo,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);            
}