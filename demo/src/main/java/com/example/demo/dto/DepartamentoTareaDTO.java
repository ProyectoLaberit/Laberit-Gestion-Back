package com.example.demo.dto;
/**
 *  DTO (Data Transfer Object) para representar una tarea de la base de datos en las respuestas de la API.
*/
/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos de las tareas
 */
public class DepartamentoTareaDTO {

    private Long idTarea;
    private Integer idExcel;
    private Integer idSubFase;
    private String Nombretarea;
    private Double tiempoMin;
    private Double tiempoMax;
    private Double tiempoClockify;
    private Boolean completada;
    private String nombreTareaGit;
    private Long numeroGit;
    private Integer idDepartamento;
    private String nombreDep;

    public DepartamentoTareaDTO() {

    }

    

    public DepartamentoTareaDTO(Long idTarea, Integer idExcel, Integer idSubFase, String nombretarea, Double tiempoMin,
            Double tiempoMax, Double tiempoClockify, Boolean completada, String nombreTareaGit, Long numeroGit,
            Integer idDepartamento, String nombreDep) {
        this.idTarea = idTarea;
        this.idExcel = idExcel;
        this.idSubFase = idSubFase;
        Nombretarea = nombretarea;
        this.tiempoMin = tiempoMin;
        this.tiempoMax = tiempoMax;
        this.tiempoClockify = tiempoClockify;
        this.completada = completada;
        this.nombreTareaGit = nombreTareaGit;
        this.numeroGit = numeroGit;
        this.idDepartamento = idDepartamento;
        this.nombreDep = nombreDep;
    }



    public Long getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Long idTarea) {
        this.idTarea = idTarea;
    }

    public Integer getIdExcel() {
        return idExcel;
    }

    public void setIdExcel(Integer idExcel) {
        this.idExcel = idExcel;
    }

    public Integer getIdSubFase() {
        return idSubFase;
    }

    public void setIdSubFase(Integer idSubFase) {
        this.idSubFase = idSubFase;
    }

    public String getNombretarea() {
        return Nombretarea;
    }

    public void setNombretarea(String nombretarea) {
        Nombretarea = nombretarea;
    }

    public Double getTiempoMin() {
        return tiempoMin;
    }

    public void setTiempoMin(Double tiempoMin) {
        this.tiempoMin = tiempoMin;
    }

    public Double getTiempoMax() {
        return tiempoMax;
    }

    public void setTiempoMax(Double tiempoMax) {
        this.tiempoMax = tiempoMax;
    }

    public Double getTiempoClockify() {
        return tiempoClockify;
    }

    public void setTiempoClockify(Double tiempoClockify) {
        this.tiempoClockify = tiempoClockify;
    }

    public Boolean getCompletada() {
        return completada;
    }

    public void setCompletada(Boolean completada) {
        this.completada = completada;
    }

    public String getNombreTareaGit() {
        return nombreTareaGit;
    }

    public void setNombreTareaGit(String nombreTareaGit) {
        this.nombreTareaGit = nombreTareaGit;
    }

    public Long getNumeroGit() {
        return numeroGit;
    }

    public void setNumeroGit(Long numeroGit) {
        this.numeroGit = numeroGit;
    }

    public Integer getIdDepartamento() {
        return idDepartamento;
    }

    public void setIdDepartamento(Integer idDepartamento) {
        this.idDepartamento = idDepartamento;
    }



    public String getNombreDep() {
        return nombreDep;
    }



    public void setNombreDep(String nombreDep) {
        this.nombreDep = nombreDep;
    }
  

}
