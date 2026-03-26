package com.example.demo.entity;

import jakarta.persistence.Column;
// Importa las herramientas de persistencia (JPA) para mapear Java con la Base de Datos
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity 
// @Entity: Le dice a Spring que esta clase es una "Entidad". 
// Sin esto, Java no sabría que esta clase debe guardarse en una base de datos.

@Table(name = "proyecto") 
// @Table: Indica el nombre real de la tabla en tu MySQL. 
// Se usa porque a veces la clase se llama "Proyecto" pero la tabla se llama "proyectos" (en plural).

public class Proyecto {

    @Id 
    // @Id: Marca esta variable como la "Llave Primaria" (Primary Key). 
    // Es el identificador único que no se puede repetir.
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    // @GeneratedValue: Indica que el ID es "autoincremental". 
    // Tú no lo escribes, la base de datos le suma 1 cada vez que creas un proyecto.
    @Column(name = "id_proyecto")
    private Long id;

    private String nombre;
    private String descripcion;
    @Column(name = "fecha_inicio")
    private String fechaInicio;
    private boolean activo;
    
    // IDs internos para las APIs
    @Column(name = "gitlab_proyecto_id")
    // Mapeamos tu variable gitlabId a la columna real de la imagen
    private String gitlabId;

    @Column(name = "clockify_proyecto_id")
    // Mapeamos tu variable clockifyId a la columna real de la imagen
    private String clockifyId;

    // --- CONSTRUCTOR VACÍO ---
    // Es obligatorio para que Spring Boot pueda crear el objeto al leer la base de datos
    public Proyecto() {
    }

    // --- GETTERS Y SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getGitlabId() {
        return gitlabId;
    }

    public void setGitlabId(String gitlabId) {
        this.gitlabId = gitlabId;
    }

    public String getClockifyId() {
        return clockifyId;
    }

    public void setClockifyId(String clockifyId) {
        this.clockifyId = clockifyId;
    }
    
}
