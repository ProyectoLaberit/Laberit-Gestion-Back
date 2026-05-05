package com.example.demo.repository;

import com.example.demo.entity.Departamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/* Repositorio para la entidad Departamento.
    * Extiende JpaRepository para proporcionar métodos CRUD básicos sin necesidad de implementarlos manualmente.
    * La anotación @Repository indica que esta interfaz es un componente de acceso a datos, lo que permite a Spring manejar las excepciones de la base de datos de manera adecuada.
*/
@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {
}