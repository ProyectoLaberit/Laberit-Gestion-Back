package com.example.demo.services;

import org.springframework.stereotype.Service;

import com.example.demo.dto.LoginRequest;

@Service
public class UsuarioService {

    public boolean validarUsuario(LoginRequest login) {
        if ("admin".equals(login.getEmail()) && "1234".equals(login.getPassword())) {
            return true;
        } else {
            return false;
        }
    }
}
