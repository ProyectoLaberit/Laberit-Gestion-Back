package com.example.demo.dto;

import java.util.List;

public class FaseDTO {
    private Integer id;
    private String nombre;
    private Integer fasePadre;
    private List<SubFaseDTO> subfases;

    public FaseDTO(Integer id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public FaseDTO() {}

    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getFasePadre() { return fasePadre; }
    public void setFasePadre(Integer fasePadre) { this.fasePadre = fasePadre; }
    public List<SubFaseDTO> getSubfases() { return subfases; }
    public void setSubfases(List<SubFaseDTO> subfases) { this.subfases = subfases; }
}
