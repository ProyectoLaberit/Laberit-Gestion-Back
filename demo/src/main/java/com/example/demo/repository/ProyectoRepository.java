package com.example.demo.repository;

// IMPORT: Traemos tu clase Proyecto para que el repositorio sepa qué tabla debe manejar
import com.example.demo.entity.Proyecto;

import java.time.LocalDate;
import java.util.List;

// IMPORT: Esta es la herramienta de Spring que contiene todos los métodos (findAll, save, etc.)
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
// IMPORT: Etiqueta necesaria para que Spring reconozca esta interfaz como un componente de datos
import org.springframework.stereotype.Repository;

@Repository
/* * @Repository: Le indica a Spring que esta interfaz es la encargada de comunicarse 
 * con la base de datos. Spring la detectará automáticamente y la dejará lista para usar.
 */
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    @Query("SELECT p FROM Proyecto p WHERE " +
           "(:activo IS NULL OR p.activo = :activo) AND " +
           "(:desde IS NULL OR p.fechaInicio >= :desde)" +
           "(:hasta IS NULL OR p.fechaInicio <= :hasta)")
           
    List<Proyecto> findByFiltrosOpcionales(
        @Param("activo") Boolean activo, 
        @Param("desde") LocalDate desde,
        @Param("hasta") LocalDate hasta); // Método para obtener solo los proyectos activos
}