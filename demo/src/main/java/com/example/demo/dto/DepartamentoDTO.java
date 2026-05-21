package com.example.demo.dto;

import com.example.demo.entity.Departamento;

/**
 *  DTO (Data Transfer Object) para representar un departamento en las respuestas de la API.
*/
/**
 * clase que sire para enviar y recibir informacion de los departamentos a front
 * Contiene un campo departamento padre para evitar un bucle infinito al serializar la entidad
 */

public class DepartamentoDTO {
    private Integer id;
    private String nombre;
    private Integer idPadre;
    private String nombrePadre;

    public DepartamentoDTO() {
    }

    // Constructor mágico para convertir la Entidad en DTO fácilmente
    public DepartamentoDTO(Departamento d) {
        this.id = d.getId();
        this.nombre = d.getNombre();
        if (d.getPadre() != null) {
            this.idPadre = d.getPadre().getId();
            this.nombrePadre = d.getPadre().getNombre();
        }
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

    public Integer getIdPadre() {
        return idPadre;
    }

    public void setIdPadre(Integer idPadre) {
        this.idPadre = idPadre;
    }

    public String getNombrePadre() {
        return nombrePadre;
    }

    public void setNombrePadre(String nombrePadre) {
        this.nombrePadre = nombrePadre;
    }
}
