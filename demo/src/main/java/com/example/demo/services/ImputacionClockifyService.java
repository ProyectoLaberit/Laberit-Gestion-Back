package com.example.demo.services;

import com.example.demo.entity.ImputacionClockify;
import com.example.demo.repository.ImputacionClockifyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImputacionClockifyService {

    @Autowired
    private ImputacionClockifyRepository repository;

    /**
     * Calcula y devuelve la suma total de horas trabajadas que están marcadas como válidas 
     * para una tarea (detalle de estimación) específica. Si el resultado es nulo, devuelve 0.0.
     */
    public Double obtenerSumaHorasValidas(Long idDetalleEstimacion) {
        Double suma = repository.sumarHorasValidas(idDetalleEstimacion);
        if (suma != null) {
            return suma;
        } else {
            return 0.0;
        }
    }
    
    /**
     * Recupera una lista de todas las imputaciones que tienen el estado de válidas 
     * asociadas a una tarea en concreto.
     */
    public List<ImputacionClockify> obtenerImputacionesValidas(Long idDetalleEstimacion) {
        return repository.findByIdDetalleEstimacionAndValidaTrue(idDetalleEstimacion);
    }

    /**
     * Recupera las imputaciones de un proyecto que están marcadas como no válidas (huérfanas), 
     * es decir, que no están asignadas a ninguna tarea.
     */
    public List<ImputacionClockify> obtenerHuerfanas(Long idProyecto) {
        return repository.findByIdProyectoAndValidaFalse(idProyecto);
    }

    /**
     * Asigna manualmente una imputación huérfana a una tarea específica, 
     * cambiando su estado a válido (true).
     */
    public ImputacionClockify vincularImputacionManual(Long idImputacionClockify, Long idDetalleEstimacion) {
        ImputacionClockify imputacion = repository.findById(idImputacionClockify).orElse(null);
        
        if (imputacion != null && !imputacion.getValida()) {
            imputacion.setValida(true);
            imputacion.setIdDetalleEstimacion(idDetalleEstimacion);
            return repository.save(imputacion);
        }
        
        return null; 
    }

    /**
     * Comprueba si ya existe un registro de imputación en la base de datos 
     * utilizando su identificador original proveniente de Clockify.
     */
    public boolean existeImputacion(String idClockifyOriginal) {
        return repository.existsByIdClockifyOriginal(idClockifyOriginal);
    }
    
    /**
     * Guarda una lista completa de nuevas imputaciones en la base de datos de una sola vez.
     */
    public List<ImputacionClockify> guardarImputaciones(List<ImputacionClockify> nuevasImputaciones) {
        return repository.saveAll(nuevasImputaciones);
    }

    /**
     * Busca y devuelve un registro de imputación específico basándose en su 
     * identificador original de Clockify.
     */
    public ImputacionClockify obtenerPorIdClockify(String idClockifyOriginal) {
        return repository.findByIdClockifyOriginal(idClockifyOriginal);
    }

    /**
     * Guarda los cambios realizados en un objeto de imputación ya existente 
     * en la base de datos.
     */
    public ImputacionClockify actualizarImputacion(ImputacionClockify imputacion) {
        return repository.save(imputacion);
    }

    /**
     * Filtra y devuelve las imputaciones que coinciden con un proyecto, 
     * una tarea y un departamento específicos.
     */
    public List<ImputacionClockify> obtenerPorDepartamentoYDetalle(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento) {
        return repository.findByIdProyectoAndIdDetalleEstimacionAndIdDepartamento(idProyecto, idDetalleEstimacion, idDepartamento);
    }

    /**
     * Cuenta el número total de imputaciones que están validadas para una tarea 
     * y un departamento concretos dentro de un proyecto.
     */
    public Integer contarValidasPorDepartamento(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento) {
        return repository.countByIdProyectoAndIdDetalleEstimacionAndIdDepartamentoAndValidaTrue(idProyecto, idDetalleEstimacion, idDepartamento);
    }

    /**
     * Cuenta el número total de imputaciones inválidas (huérfanas) para una tarea 
     * y un departamento específicos en un proyecto.
     */
    public Integer contarInvalidasPorDepartamento(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento) {
        return repository.countByIdProyectoAndIdDetalleEstimacionAndIdDepartamentoAndValidaFalse(idProyecto, idDetalleEstimacion, idDepartamento);
    }

    /**
     * Alterna el estado de validación de una imputación. 
     * Si pasa a ser válida, se le asigna el ID de la tarea indicada. 
     * Si pasa a ser inválida, se limpia su ID de tarea dejándola huérfana.
     */
    public ImputacionClockify alternarEstadoValidacion(Long idImputacion, Long idDetalleEstimacion) {
        ImputacionClockify imputacion = repository.findById(idImputacion).orElse(null);
        if (imputacion != null) {
            boolean nuevoEstado = !imputacion.getValida();
            imputacion.setValida(nuevoEstado);
            
            if (nuevoEstado) {
                // Si la activamos (true), le ponemos el ID de la tarea que nos llega
                imputacion.setIdDetalleEstimacion(idDetalleEstimacion);
            } else {
                // Si la desactivamos (false), limpiamos el ID para que sea una tarea huérfana
                imputacion.setIdDetalleEstimacion(null);
            }
            
            return repository.save(imputacion);
        }
        return null;
    }

    /**
     * Modifica el texto descriptivo de una imputación 
     * específica y guarda los cambios.
     */
    public ImputacionClockify editarTareaExtraida(Long idImputacion, String nuevaTarea) {
        ImputacionClockify imputacion = repository.findById(idImputacion).orElse(null);
        
        if (imputacion != null) {
            imputacion.setTareaExtraida(nuevaTarea);
            return repository.save(imputacion);
        }
        return null;
    }

    /**
     * Elimina permanentemente un registro de imputación de la base de datos 
     * si este existe.
     */
    public boolean borrarImputacion(Long idImputacion) {
        if (repository.existsById(idImputacion)) {
            repository.deleteById(idImputacion);
            return true;
        }
        return false;
    }
}