package com.example.demo.dto;

/**
 *  DTO (Data Transfer Object) para representar un usuario en las respuestas de la API.
*/
/**
 * Clase que contiene la informacion de un usuario
 */
public class UsuarioDTO {

    private Integer id;
    private String nombre;
    private String email;
    private String password;
    private String passwordVieja;
    private String passwordNueva;
    private String resetToken;
    private String foto;
    private String rol;

    public UsuarioDTO() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordVieja() {
        return passwordVieja;
    }

    public void setPasswordVieja(String passwordVieja) {
        this.passwordVieja = passwordVieja;
    }

    public String getPasswordNueva() {
        return passwordNueva;
    }

    public void setPasswordNueva(String passwordNueva) {
        this.passwordNueva = passwordNueva;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}