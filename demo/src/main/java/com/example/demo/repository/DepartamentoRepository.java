package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Departamento;

@Repository
public interface DepartamentoRepository extends JpaRepository<Departamento, Integer> {
  
    
}


