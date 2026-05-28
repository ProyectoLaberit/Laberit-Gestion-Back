package com.example.demo.dto.excel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FilaAuditoriaClockifyDTO {
    private String fecha;
    private String descripcion;
    private String horas;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public FilaAuditoriaClockifyDTO() {
    }

    // Método de utilidad para formatear horas decimales a "Xh YYmin"
    public static String formatearHoras(Double horas) {
        if (horas == null)
            return "0h 0min";
        int h = (int) Math.floor(horas);
        int min = (int) Math.round((horas - h) * 60);
        return h + "h " + min + "min";
    }

    // Método de utilidad para formatear la fecha
    public static String formatearFecha(LocalDate fecha) {
        if (fecha == null)
            return "-";
        return fecha.format(FORMATO_FECHA);
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getHoras() {
        return horas;
    }

    public void setHoras(String horas) {
        this.horas = horas;
    }
}