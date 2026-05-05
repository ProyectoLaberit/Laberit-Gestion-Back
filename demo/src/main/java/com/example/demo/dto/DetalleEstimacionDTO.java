package com.example.demo.dto;



/**
 * Clase enviada y/o recibida del front para mostrar o recibir datos de las estimaciones de los excel
 */
public class DetalleEstimacionDTO {
    
    private Long id;
    private Integer idExcel;
    
    // IDs originales por si el front necesita hacer updates
    private Integer idDepartamento;
    private Integer idFasePadre;
    
    private Integer idSubFase;
    // Textos legibles para pintar la tabla directamente
    private String nombreDepartamento;
    private String nombreFase;     // Padre (ej: Análisis, Desarrollo)
    private String nombreSubfase;  // Hijo (ej: Investigación, Frontend)
    
    private String tarea;
    private Double tiempoMin;
    private Double tiempoMax;
    private Double tiempoReal;
    private String numeroGitlab;
    private Boolean completada;

    // Constructor vacío
    public DetalleEstimacionDTO() {}

    // Constructor completo para facilitar el mapeo
    public DetalleEstimacionDTO(Long id, Integer idExcel, Integer idDepartamento, Integer idFase,
                                String nombreDepartamento, String nombreFase, String nombreSubfase,
                                String tarea, Double tiempoMin, Double tiempoMax) {
        this.id = id;
        this.idExcel = idExcel;
        this.idDepartamento = idDepartamento;
        this.idSubFase = idFase;
        this.nombreDepartamento = nombreDepartamento;
        this.nombreFase = nombreFase;
        this.nombreSubfase = nombreSubfase;
        this.tarea = tarea;
        this.tiempoMin = tiempoMin;
        this.tiempoMax = tiempoMax;
        this.completada = false;
    }

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getIdExcel() { return idExcel; }
    public void setIdExcel(Integer idExcel) { this.idExcel = idExcel; }
    
    public Integer getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Integer idDepartamento) { this.idDepartamento = idDepartamento; }

    public Integer getIdFasePadre() { return idFasePadre; }
    public void setIdFasePadre(Integer idFasePadre) { this.idFasePadre = idFasePadre; }
    
    public Integer getIdSubFase() { return idSubFase; }
    public void setIdSubFase(Integer idSubFase) { this.idSubFase = idSubFase; }
    
    public String getNombreDepartamento() { return nombreDepartamento; }
    public void setNombreDepartamento(String nombreDepartamento) { this.nombreDepartamento = nombreDepartamento; }
    
    public String getNombreFase() { return nombreFase; }
    public void setNombreFase(String nombreFase) { this.nombreFase = nombreFase; }
    
    public String getNombreSubfase() { return nombreSubfase; }
    public void setNombreSubfase(String nombreSubfase) { this.nombreSubfase = nombreSubfase; }
    
    public String getTarea() { return tarea; }
    public void setTarea(String tarea) { this.tarea = tarea; }
    
    public Double getTiempoMin() { return tiempoMin; }
    public void setTiempoMin(Double tiempoMin) { this.tiempoMin = tiempoMin; }
    
    public Double getTiempoMax() { return tiempoMax; }
    public void setTiempoMax(Double tiempoMax) { this.tiempoMax = tiempoMax; }

    public Double getTiempoReal() { return tiempoReal; }
    public void setTiempoReal(Double tiempoReal) { this.tiempoReal = tiempoReal; }

    public String getNumeroGitlab() { return numeroGitlab; }
    public void setNumeroGitlab(String numeroGitlab) { this.numeroGitlab = numeroGitlab; }

    public Boolean getCompletada() { return completada; }
    public void setCompletada(Boolean completada) { this.completada = completada; }
}
