package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "imputacion_clockify")
public class ImputacionClockify {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imputacion_clockify")
    private Long idImputacionClockify;

    @Column(name = "id_clockify_original", unique = true)
    private String idClockifyOriginal;

    @Column(name = "id_proyecto", nullable = false)
    private Long idProyecto;

    @Column(name = "subfase_extraida")
    private String subfaseExtraida;

    @Column(name = "tarea_extraida")
    private String tareaExtraida;

    @Column(name = "descripcion_original", columnDefinition = "TEXT", nullable = false)
    private String descripcionOriginal;

    @Column(name = "id_departamento")
    private Integer idDepartamento;

    @Column(name = "horas_trabajadas", nullable = false)
    private Double horasTrabajadas;
    
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fin")
    private LocalTime horaFin;

    @Column(name = "valida", nullable = false)
    private Boolean valida = false;

    @Column(name = "id_tarea_proyecto")
    private Long idTareaProyecto;

    public ImputacionClockify() {
    }

    public Long getIdImputacionClockify() { return idImputacionClockify; }
    public void setIdImputacionClockify(Long idImputacionClockify) { this.idImputacionClockify = idImputacionClockify; }

    public String getIdClockifyOriginal() { return idClockifyOriginal; }
    public void setIdClockifyOriginal(String idClockifyOriginal) { this.idClockifyOriginal = idClockifyOriginal; }

    public Long getIdProyecto() { return idProyecto; }
    public void setIdProyecto(Long idProyecto) { this.idProyecto = idProyecto; }

    public String getSubfaseExtraida() { return subfaseExtraida; }
    public void setSubfaseExtraida(String subfaseExtraida) { this.subfaseExtraida = subfaseExtraida; }

    public String getTareaExtraida() { return tareaExtraida; }
    public void setTareaExtraida(String tareaExtraida) { this.tareaExtraida = tareaExtraida; }

    public String getDescripcionOriginal() { return descripcionOriginal; }
    public void setDescripcionOriginal(String descripcionOriginal) { this.descripcionOriginal = descripcionOriginal; }

    public Integer getIdDepartamento() { return idDepartamento; }
    public void setIdDepartamento(Integer idDepartamento) { this.idDepartamento = idDepartamento; }

    public Double getHorasTrabajadas() { return horasTrabajadas; }
    public void setHorasTrabajadas(Double horasTrabajadas) { this.horasTrabajadas = horasTrabajadas; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public Boolean getValida() { return valida; }
    public void setValida(Boolean valida) { this.valida = valida; }

    public Long getIdTareaProyecto() { return idTareaProyecto; }
    public void setIdTareaProyecto(Long idTareaProyecto) { this.idTareaProyecto = idTareaProyecto; }

}