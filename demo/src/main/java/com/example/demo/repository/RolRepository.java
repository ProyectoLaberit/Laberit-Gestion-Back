package com.example.demo.repository;

import com.example.demo.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    // De momento no necesitamos métodos extra aquí, con los de por defecto nos vale para buscar por ID
}