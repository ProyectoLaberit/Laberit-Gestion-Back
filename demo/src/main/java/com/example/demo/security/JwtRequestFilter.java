package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;

    // Inyectamos la misma llave secreta que usamos para crear el token
    public JwtRequestFilter(@Value("${SECRET_KEY_STRING}") String secretKeyString) {
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Buscamos el tokken en la cabecera de la petición (Authorization)
        final String authorizationHeader = request.getHeader("Authorization");

        // Comprobamos si lo trae y si tiene el formato correcto (empieza por "Bearer ")
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7); // Le quitamos el prefijo "Bearer "

            try {
                // Desciframos el token con nuestra llave
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // Sacamos los datos que guardamos al hacer login
                String email = claims.getSubject();
                List<String> roles = claims.get("roles", List.class);

                // Si hay un email y el usuario aún no está validado en este hilo de ejecución
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // Convertimos los roles al formato que entiende Spring (con el prefijo ROLE_)
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (roles != null) {
                        authorities = roles.stream()
                                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()))
                                .collect(Collectors.toList());
                    }

                    // Creamos la tarjeta de identificación oficial para Spring Security
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email, null, authorities);
                    
                    // Le decimos a Spring: "Este usuario es válido y tiene estos roles, déjale pasar"
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Si el token es inventado, ha caducado o está mal firmado, cae aquí.
                // No hacemos nada, simplemente no le damos la validación y Spring le bloqueará luego.
                System.out.println("Token inválido o caducado: " + e.getMessage());
            }
        }

        // Pase lo que pase, dejamos que la petición continúe su camino hacia el SecurityConfig
        chain.doFilter(request, response);
    }
}