package com.example.demo.dto.excel;

public class CabeceraDTO {
    private Long horasMinimas;
    private Long horasMaximas;
    private Long horasReales;
    private Long desviacion;
    private Integer porcentajesValidasGitlab;
    private Integer imputacionesInvalidadas;


    public CabeceraDTO() {
    }

    public CabeceraDTO(Long horasMinimas, Long horasMaximas, Long horasReales, Long desviacion, Integer porcentajesValidasGitlab, Integer imputacionesInvalidadas) {
        this.horasMinimas = horasMinimas;
        this.horasMaximas = horasMaximas;
        this.horasReales = horasReales;
        this.desviacion = desviacion;
        this.porcentajesValidasGitlab = porcentajesValidasGitlab;
        this.imputacionesInvalidadas = imputacionesInvalidadas;
    }

    public Long getHorasMinimas() {
        return horasMinimas;
    }

    public Long getHorasMaximas() {
        return horasMaximas;
    }

    public Long getHorasReales() {
        return horasReales;
    }

    public Long getDesviacion() {
        return desviacion;
    }

    public Integer getPorcentajesValidasGitlab() {
        return porcentajesValidasGitlab;
    }

    public Integer getImputacionesInvalidadas() {
        return imputacionesInvalidadas;
    }

    public void setHorasMinimas(Long horasMinimas) {
        this.horasMinimas = horasMinimas;
    }

    public void setHorasMaximas(Long horasMaximas) {
        this.horasMaximas = horasMaximas;
    }

    public void setHorasReales(Long horasReales) {
        this.horasReales = horasReales;
    }

    public void setDesviacion(Long desviacion) {
        this.desviacion = desviacion;
    }

    public void setPorcentajesValidasGitlab(Integer porcentajesValidasGitlab) {
        this.porcentajesValidasGitlab = porcentajesValidasGitlab;
    }

    public void setImputacionesInvalidadas(Integer imputacionesInvalidadas) {
        this.imputacionesInvalidadas = imputacionesInvalidadas;
    }

    
}
