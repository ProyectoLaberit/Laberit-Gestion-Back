package com.example.demo.repository;

import com.example.demo.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    // Este método es vital para el Login y para evitar correos duplicados
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByPasswordResetTokenHash(String passwordResetTokenHash);


}