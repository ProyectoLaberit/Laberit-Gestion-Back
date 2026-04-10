package com.example.demo.repository;

import com.example.demo.entity.ApiConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ApiConfigRepository extends JpaRepository<ApiConfig, Integer> {
    // Buscaremos por el nombre (ej: "GitLab Maestro")
    Optional<ApiConfig> findByNombre(String nombre);
}