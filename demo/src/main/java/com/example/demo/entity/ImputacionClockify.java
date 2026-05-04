package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.math.BigDecimal;

@Entity
@Table(name = "imputacion_clockify")
public class ImputacionClockify {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imputacion_clockify")
    private Long idImputacionClockify;

    @Column(name = "id_clockify_original")
    private String idClockifyOriginal;

    @Column(name = "nombre_tarea", columnDefinition = "TEXT")
    private String nombreTarea;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private BigDecimal horaInicio;

    @Column(name = "hora_fin")
    private BigDecimal horaFin;

    @Column(name = "horas_totales")
    private BigDecimal horasTotales;

    @Column(name = "estado", length = 45)
    private String estado;

    @Column(name = "id_detalle_estimacion")
    private Long idDetalleEstimacion;

    public ImputacionClockify() {}

    // GETTERS Y SETTERS
    public Long getIdImputacionClockify() { return idImputacionClockify; }
    public void setIdImputacionClockify(Long idImputacionClockify) { this.idImputacionClockify = idImputacionClockify; }

    public String getIdClockifyOriginal() { return idClockifyOriginal; }
    public void setIdClockifyOriginal(String idClockifyOriginal) { this.idClockifyOriginal = idClockifyOriginal; }

    public String getNombreTarea() { return nombreTarea; }
    public void setNombreTarea(String nombreTarea) { this.nombreTarea = nombreTarea; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public BigDecimal getHoraInicio() { return horaInicio; }
    public void setHoraInicio(BigDecimal horaInicio) { this.horaInicio = horaInicio; }

    public BigDecimal getHoraFin() { return horaFin; }
    public void setHoraFin(BigDecimal horaFin) { this.horaFin = horaFin; }

    public BigDecimal getHorasTotales() { return horasTotales; }
    public void setHorasTotales(BigDecimal horasTotales) { this.horasTotales = horasTotales; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getIdDetalleEstimacion() { return idDetalleEstimacion; }
    public void setIdDetalleEstimacion(Long idDetalleEstimacion) { this.idDetalleEstimacion = idDetalleEstimacion; }
}