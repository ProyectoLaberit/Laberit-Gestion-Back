package com.example.demo.security;

import com.example.demo.entity.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtil {

    // La llave maestra.
    private final SecretKey secretKey;

    // Convertimos la frase en una llave criptográfica real
    public JwtUtil(@Value("${SECRET_KEY_STRING}") String secretKeyString) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    public String generarToken(Usuario usuario) {
        // Definimos cuánto durará el token (24h)
        long tiempoValidez = 1000 * 60 * 60 * 24;

        // Extraemos los roles del usuario y los convertimos a una lista de texto
        List<String> roles = usuario.getRoles().stream()
                .map(rol -> rol.getNombre())
                .collect(Collectors.toList());

        // Extraemos los permisos de todos sus roles
        List<String> permisos = usuario.getRoles().stream()
                .flatMap(rol -> rol.getPermisos().stream())
                .map(permiso -> permiso.getNombre())
                .collect(Collectors.toList());

        // Construimos y firmamos el Token
        return Jwts.builder()
                .subject(usuario.getEmail()) // El "titular" del token
                .claim("id", usuario.getId()) // Guardamos su ID
                .claim("nombre", usuario.getNombre()) // Guardamos su Nombre
                .claim("roles", roles) // Metemos la lista de roles
                .claim("permisos", permisos) // Metemos la lista de permisos
                .issuedAt(new Date()) // Fecha de creación
                .expiration(new Date(System.currentTimeMillis() + tiempoValidez)) // Fecha de caducidad
                .signWith(secretKey) // Lo sellamos con la llave maestra
                .compact(); // Lo comprimimos en el texto final
    }
}