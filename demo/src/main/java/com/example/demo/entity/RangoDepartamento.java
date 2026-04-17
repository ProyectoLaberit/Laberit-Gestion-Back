package com.example.demo.entity;

/**
 * Clase auxiliar para mapear la estructura matricial del Excel.
 * Define el rango de columnas (mín/máx) que pertenecen a un Departamento específico.
 */
public class RangoDepartamento {
    private int colMin;
    private int colMax;
    private String nombreExcel;
    private Integer idBD;

    public RangoDepartamento() {
    }

    public RangoDepartamento(int colMin, int colMax, String nombreExcel) {
        this.colMin = colMin;
        this.colMax = colMax;
        this.nombreExcel = nombreExcel;
    }

    // Getters y Setters necesarios para el acceso desde el Service
    public int getColMin() { return colMin; }
    public void setColMin(int colMin) { this.colMin = colMin; }

    public int getColMax() { return colMax; }
    public void setColMax(int colMax) { this.colMax = colMax; }

    public String getNombreExcel() { return nombreExcel; }
    public void setNombreExcel(String nombreExcel) { this.nombreExcel = nombreExcel; }

    public Integer getIdBD() { return idBD; }
    public void setIdBD(Integer idBD) { this.idBD = idBD; }
}