package com.example.demo.services;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.repository.ImputacionClockifyRepository;
import com.example.demo.repository.TareaProyectoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImputacionClockifyService {

    @Autowired
    private ImputacionClockifyRepository repository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    @Autowired
    private com.example.demo.repository.GitLabTareaRepository gitLabTareaRepository;

    /**
     * Calcula y devuelve la suma total de horas trabajadas que están marcadas como
     * válidas
     * para una tarea (detalle de estimación) específica. Si el resultado es nulo,
     * devuelve 0.0.
     */
    public Double obtenerSumaHorasValidas(Long idTareaProyecto) {
        Double suma = repository.sumarHorasValidas(idTareaProyecto);
        if (suma != null) {
            return suma;
        } else {
            return 0.0;
        }
    }

    /**
     * Asigna manualmente una imputación huérfana a una tarea específica,
     * cambiando su estado a válido (true).
     * Vincula manualmente una imputación y "enseña" al Excel el número de GitLab
     * para que las próximas se validen solas.
     */
    @Auditable(accion = "VINCULAR_IMPUTACION", tabla = "imputacion_clockify", entidad = ImputacionClockify.class, descripcion = "Se vinculó manualmente la imputación con ID '#{#idImputacionClockify}' a la tarea con ID '#{#idTareaProyecto}'")
    public ImputacionClockify vincularImputacionManual(Long idImputacionClockify, Long idTareaProyecto) {
        // 1. Buscamos la imputación en la base de datos
        ImputacionClockify imputacion = repository.findById(idImputacionClockify).orElse(null);

        if (imputacion != null && !imputacion.getValida()) {
            // 2. Marcamos como válida y enlazamos a la tarea
            imputacion.setValida(true);
            imputacion.setIdTareaProyecto(idTareaProyecto);
            repository.save(imputacion);

            return imputacion;
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
     * Guarda una lista completa de nuevas imputaciones en la base de datos de una
     * sola vez.
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
    @Auditable(accion = "ACTUALIZAR_IMPUTACION", tabla = "imputacion_clockify", entidad = ImputacionClockify.class, descripcion = "Se actualizaron los datos de la imputación (Clockify ID original: '#{#imputacion.idClockifyOriginal}')")
    public ImputacionClockify actualizarImputacion(ImputacionClockify imputacion) {
        return repository.save(imputacion);
    }

    /**
     * Filtra y devuelve las imputaciones que coinciden con un proyecto,
     * una tarea y un departamento específicos.
     */
    public List<ImputacionClockify> obtenerPorDepartamentoYTarea(Long idProyecto, Long idTareaProyecto,
            Integer idDepartamento, String subfase) {
        
        Long numeroGitlab = null;
        if (idTareaProyecto != null) {
            numeroGitlab = gitLabTareaRepository.findNumeroGitlabByTareaProyectoId(idTareaProyecto);
        }

        if (numeroGitlab != null) {
            return repository.findByNumeroGitlabAndIdProyecto(numeroGitlab, idProyecto);
        }
        // Si no tiene numero de GitLab asociado, no devolvemos nada para evitar 
        // cruzar imputaciones de otras tareas del mismo departamento y subfase.
        return java.util.Collections.emptyList();
    }

    /**
     * Cuenta el número total de imputaciones que están validadas para una tarea
     * y un departamento concretos dentro de un proyecto.
     */
    public Integer contarValidasPorDepartamento(Long idProyecto, Long idTareaProyecto, Integer idDepartamento) {
        Integer idDepartamentoReal = resolverDepartamentoReal(idProyecto, idTareaProyecto, idDepartamento);
        return repository.countByIdProyectoAndIdTareaProyectoAndIdDepartamentoAndValidaTrue(idProyecto, idTareaProyecto,
                idDepartamentoReal);
    }

    /**
     * Cuenta el número total de imputaciones inválidas (huérfanas) para una tarea
     * y un departamento específicos en un proyecto.
     */
    public Integer contarInvalidasPorDepartamento(Long idProyecto, Long idTareaProyecto, Integer idDepartamento) {
        Integer idDepartamentoReal = resolverDepartamentoReal(idProyecto, idTareaProyecto, idDepartamento);
        return repository.countByIdProyectoAndIdTareaProyectoAndIdDepartamentoAndValidaFalse(idProyecto,
                idTareaProyecto, idDepartamentoReal);
    }

    /**
     * Alterna el estado de validación de una imputación.
     * Si pasa a ser válida, se le asigna el ID de la tarea indicada.
     * Si pasa a ser inválida, se limpia su ID de tarea dejándola huérfana.
     */
    @Auditable(accion = "ALTERNAR_ESTADO_IMPUTACION", tabla = "imputacion_clockify", entidad = ImputacionClockify.class, descripcion = "Se cambió el estado de la imputación con ID '#{#idImputacion}' (Nueva tarea asociada: #{#idTareaProyecto})")
    public ImputacionClockify alternarEstadoValidacion(Long idImputacion, Long idTareaProyecto) {
        ImputacionClockify imputacion = repository.findById(idImputacion).orElse(null);
        if (imputacion != null) {
            boolean nuevoEstado = !imputacion.getValida();
            imputacion.setValida(nuevoEstado);

            if (nuevoEstado) {
                // Si la activamos (true), le ponemos el ID de la tarea que nos llega
                imputacion.setIdTareaProyecto(idTareaProyecto);
            } else {
                // Si la desactivamos (false), limpiamos el ID para que sea una tarea huérfana
                imputacion.setIdTareaProyecto(null);
            }

            return repository.save(imputacion);
        }
        return null;
    }

    /**
     * Modifica el texto descriptivo de una imputación
     * específica y guarda los cambios.
     */
    @Auditable(accion = "EDITAR_TAREA_IMPUTACION", tabla = "imputacion_clockify", entidad = ImputacionClockify.class, descripcion = "Se editó la descripción extraída a '#{#nuevaTarea}' en la imputación '#{#idImputacion}'")
    public ImputacionClockify editarTareaExtraida(Long idImputacion, String nuevaTarea) {
        ImputacionClockify imputacion = repository.findById(idImputacion).orElse(null);

        if (imputacion == null)
            return null;

        // 1. Actualizamos los textos (la parte visual)
        if (nuevaTarea != null && !nuevaTarea.trim().isEmpty()) {
            imputacion.setTareaExtraida(nuevaTarea.trim());
        }
        return repository.save(imputacion);
    }

    /**
     * Elimina permanentemente un registro de imputación de la base de datos
     * si este existe.
     */
    @Auditable(accion = "BORRAR_IMPUTACION", tabla = "imputacion_clockify", entidad = ImputacionClockify.class, descripcion = "Se borró permanentemente la imputación con ID: #{#idImputacion}")
    public boolean borrarImputacion(Long idImputacion) {
        if (repository.existsById(idImputacion)) {
            repository.deleteById(idImputacion);
            return true;
        }
        return false;
    }

    /**
     * Filtra las imputaciones por fechas asegurándose de que el rango sea válido.
     */
    public List<ImputacionClockify> filtrarPorFechas(Long idProyecto, Long idTareaProyecto, Integer idDepartamento,
            String subfase, java.time.LocalDate desde, java.time.LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            return java.util.Collections.emptyList();
        }

        Long numeroGitlab = null;
        if (idTareaProyecto != null) {
            numeroGitlab = gitLabTareaRepository.findNumeroGitlabByTareaProyectoId(idTareaProyecto);
        }

        if (numeroGitlab != null) {
            return repository.findByNumeroGitlabAndIdProyectoAndFechaBetween(numeroGitlab, idProyecto, desde, hasta);
        }
        // Igual que arriba, si no hay GitLab ID, devolvemos una lista vacía 
        // para que no se filtren imputaciones ajenas a esta tarea.
        return java.util.Collections.emptyList();
    }

    /**
     * Obtiene el departamento real desde tarea_proyecto para evitar consultas
     * vacias
     * cuando el front envia un identificador heredado o incorrecto.
     */
    private Integer resolverDepartamentoReal(Long idProyecto, Long idTareaProyecto, Integer idDepartamentoFallback) {
        if (idTareaProyecto == null) {
            return idDepartamentoFallback;
        }

        return tareaProyectoRepository.findById(idTareaProyecto)
                .filter(tarea -> idProyecto == null || tarea.getIdProyecto().equals(idProyecto))
                .map(tarea -> tarea.getIdDepartamento())
                .orElse(idDepartamentoFallback);
    }

    // Limpia tildes y espacios
    private String normalizar(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return "";
        }
        String limpio = texto.trim().toLowerCase();
        String normalizado = java.text.Normalizer.normalize(limpio, java.text.Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
