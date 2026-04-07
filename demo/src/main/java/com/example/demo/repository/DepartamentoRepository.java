package com.example.demo.repository;

import java.util.Optional;

import org.apache.poi.sl.draw.geom.GuideIf.Op;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Departamento;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {
    // Aquí puedes agregar métodos personalizados si es necesario
    
    Optional<Departamento> findByNombre(String nombre);
    
}


