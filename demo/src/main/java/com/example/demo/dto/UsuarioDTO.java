package com.example.demo.dto;

public class UsuarioDTO {

    private Integer id;
    private String nombre;
    private String email;
    private Boolean excels;
    
    // Para el Endpoint 2 (Crear Usuario)
    private String password; 
    
    // Para el Endpoint 4 (Cambiar Contraseña)
    private String passwordVieja;
    private String passwordNueva;

    // Para el Endpoint 5 (Cambiar Foto) 
    private String foto;

    // Para el Endpoint 6 (Cambiar Rol) 
    private String rol; 

    public UsuarioDTO() {
    }

    // --- GETTERS Y SETTERS ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getExcels() { return excels; }
    public void setExcels(Boolean excels) { this.excels = excels; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPasswordVieja() { return passwordVieja; }
    public void setPasswordVieja(String passwordVieja) { this.passwordVieja = passwordVieja; }

    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}