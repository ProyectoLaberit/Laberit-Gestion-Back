package com.example.demo.dto;

public class DetalleEstimacionDTO {
    
    private Long id;
    private Integer idExcel;
    
    // IDs originales por si el front necesita hacer updates
    private Integer idDepartamento;
    private Integer idFase; 
    
    // Textos legibles para pintar la tabla directamente
    private String nombreDepartamento;
    private String nombreFase;     // Padre (ej: Análisis, Desarrollo)
    private String nombreSubfase;  // Hijo (ej: Investigación, Frontend)
    
    private String tarea;
    private Double tiempoMin; // Tiempo mínimo del excel actual
    private Double tiempoMax; // Tiempo máximo del excel actual
    private Double tiempoMinElegido; // Tiempo mínimo del excel elegido
    private Double tiempoMaxElegido; // Tiempo máximo del excel elegido
    private Double tiempoReal;
    private String numeroGitlab;

    // Constructor vacío
    public DetalleEstimacionDTO() {}

    // Constructor completo para facilitar el mapeo
    public DetalleEstimacionDTO(Long id, Integer idExcel, Integer idDepartamento, Integer idFase,
                                String nombreDepartamento, String nombreFase, String nombreSubfase,
                                String tarea, Double tiempoMin, Double tiempoMax) {
        this.id = id;
        this.idExcel = idExcel;
        this.idDepartamento = idDepartamento;
        this.idFase = idFase;
        this.nombreDepartamento = nombreDepartamento;
        this.nombreFase = nombreFase;
        this.nombreSubfase = nombreSubfase;
        this.tarea = tarea;
        this.tiempoMin = tiempoMin;
        this.tiempoMax = tiempoMax;
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
    
    public Integer getIdFase() { return idFase; }
    public void setIdFase(Integer idFase) { this.idFase = idFase; }
    
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

    public Double getTiempoMinElegido() { return tiempoMinElegido; }
    public void setTiempoMinElegido(Double tiempoMinElegido) { this.tiempoMinElegido = tiempoMinElegido; }
    
    public Double getTiempoMaxElegido() { return tiempoMaxElegido; }
    public void setTiempoMaxElegido(Double tiempoMaxElegido) { this.tiempoMaxElegido = tiempoMaxElegido; }

    public Double getTiempoReal() { return tiempoReal; }
    public void setTiempoReal(Double tiempoReal) { this.tiempoReal = tiempoReal; }

    public String getNumeroGitlab() { return numeroGitlab; }
    public void setNumeroGitlab(String numeroGitlab) { this.numeroGitlab = numeroGitlab; }
}
