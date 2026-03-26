package com.example.demo.repository;

// IMPORT: Traemos tu clase Proyecto para que el repositorio sepa qué tabla debe manejar
import com.example.demo.entity.Proyecto;

// IMPORT: Esta es la herramienta de Spring que contiene todos los métodos (findAll, save, etc.)
import org.springframework.data.jpa.repository.JpaRepository;

// IMPORT: Etiqueta necesaria para que Spring reconozca esta interfaz como un componente de datos
import org.springframework.stereotype.Repository;

@Repository
/* * @Repository: Le indica a Spring que esta interfaz es la encargada de comunicarse 
 * con la base de datos. Spring la detectará automáticamente y la dejará lista para usar.
 */
public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    
}