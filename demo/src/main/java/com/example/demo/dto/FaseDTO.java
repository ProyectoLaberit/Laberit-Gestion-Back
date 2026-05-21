package com.example.demo.dto;

import java.util.List;
/**
 *  DTO (Data Transfer Object) para representar una fase en las respuestas de la API.
*/
/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos de las fases 
 */
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

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Integer getFasePadre() { return fasePadre; }
    public void setFasePadre(Integer fasePadre) { this.fasePadre = fasePadre; }
    public List<SubFaseDTO> getSubfases() { return subfases; }
    public void setSubfases(List<SubFaseDTO> subfases) { this.subfases = subfases; }
}
