package com.example.demo.dto;
/**
 *  DTO (Data Transfer Object) para representar una subfase en las respuestas de la API.
*/
/**
 * Clase que contiene la informacion de las subfases
 */
public class SubFaseDTO {
    private Integer id;
    private String nombre;

    public SubFaseDTO(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
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
}