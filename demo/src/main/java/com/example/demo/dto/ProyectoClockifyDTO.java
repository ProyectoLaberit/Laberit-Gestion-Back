package com.example.demo.dto;
/**
 *  DTO (Data Transfer Object) para representar un proyecto de Clockify en las respuestas de la API.
*/
/**
 * Clase que contiene informacion de un proyecto de clockify
 */
public class ProyectoClockifyDTO {
    private String id;
    private String nombre;


    public ProyectoClockifyDTO(){
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }


    public String getNombre() {
        return nombre;
    }


    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
    
}
