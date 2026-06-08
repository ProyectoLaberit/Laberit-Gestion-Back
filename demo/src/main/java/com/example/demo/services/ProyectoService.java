package com.example.demo.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.annotation.Auditable;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ProyectoRepository;
import com.example.demo.repository.TareaProyectoRepository;

@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    /**
     * Recupera una lista de todos los proyectos filtrada por su estado de actividad
     * y un rango de fechas.
     */
    public List<ProyectoDTO> obtenerTodosLosProyectos(Boolean activo, Boolean completado, LocalDate desde, LocalDate hasta) {
        List<Proyecto> proyectosDB = proyectoRepository.findByFiltrosOpcionales(activo, completado, desde, hasta);

        return proyectosDB.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un nuevo proyecto en la base de datos con los datos proporcionados.
     */
    @Auditable(
        accion = "CREAR_PROYECTO",
        tabla = "proyecto",
        entidad = Proyecto.class,
        descripcion = "Se creo un nuevo proyecto llamado '#{#dto.nombre}'"
    )
    @Transactional
    public ProyectoDTO crearProyecto(ProyectoDTO dto) {
        Proyecto nuevoProyecto = new Proyecto();

        nuevoProyecto.setNombre(dto.getNombre());
        nuevoProyecto.setDescripcion(dto.getDescripcion());
        nuevoProyecto.setClockifyId(dto.getClockifyId());
        nuevoProyecto.setGitlabId(dto.getGitlabId());
        nuevoProyecto.setFechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now());
        nuevoProyecto.setActivo(dto.isActivo() != null ? dto.isActivo() : true);
        nuevoProyecto.setExcels(dto.getExcels());
        aplicarEstadoCompletado(nuevoProyecto, dto.getCompletado());

        Proyecto guardado = proyectoRepository.saveAndFlush(nuevoProyecto);
        return convertirADTO(guardado);
    }

    /**
     * Elimina permanentemente un proyecto del sistema.
     */
    @Auditable(
        accion = "BORRAR_PROYECTO",
        tabla = "proyecto",
        entidad = Proyecto.class,
        descripcion = "Se elimino del sistema el proyecto '#{#resultado.nombre}'"
    )
    public ProyectoDTO eliminarProyecto(Long id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se puede eliminar: El proyecto con ID " + id + " no existe."));

        ProyectoDTO dto = convertirADTO(proyecto);
        proyectoRepository.delete(proyecto);
        return dto;
    }

    /**
     * Actualiza los datos de un proyecto existente.
     */
    @Auditable(
        accion = "ACTUALIZAR_PROYECTO",
        tabla = "proyecto",
        entidad = Proyecto.class,
        descripcion = "Se actualizaron los datos del proyecto '#{#proyectoDTO.nombre}' (ID: #{#id})"
    )
    @Transactional
    public ProyectoDTO actualizarProyecto(Long id, ProyectoDTO proyectoDTO) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        if (tieneTexto(proyectoDTO.getNombre())) {
            proyecto.setNombre(proyectoDTO.getNombre());
        }
        if (tieneTexto(proyectoDTO.getDescripcion())) {
            proyecto.setDescripcion(proyectoDTO.getDescripcion());
        }
        if (tieneTexto(proyectoDTO.getClockifyId())) {
            proyecto.setClockifyId(proyectoDTO.getClockifyId());
        }
        if (tieneTexto(proyectoDTO.getGitlabId())) {
            proyecto.setGitlabId(proyectoDTO.getGitlabId());
        }
        if (proyectoDTO.getFechaInicio() != null) {
            proyecto.setFechaInicio(proyectoDTO.getFechaInicio());
        }
        if (proyectoDTO.isActivo() != null) {
            proyecto.setActivo(proyectoDTO.isActivo());
        }
        if (proyectoDTO.getExcels() != null) {
            proyecto.setExcels(proyectoDTO.getExcels());
        }
        if (proyectoDTO.getCompletado() != null) {
            aplicarEstadoCompletado(proyecto, proyectoDTO.getCompletado());
        }

        Proyecto actualizado = proyectoRepository.saveAndFlush(proyecto);
        return convertirADTO(actualizado);
    }

    /**
     * Actualiza solo el estado de finalizacion de un proyecto.
     */
    @Transactional
    public ProyectoDTO actualizarCompletado(Long id, Boolean completado) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        aplicarEstadoCompletado(proyecto, completado);
        Proyecto actualizado = proyectoRepository.saveAndFlush(proyecto);
        return convertirADTO(actualizado);
    }

    private void aplicarEstadoCompletado(Proyecto proyecto, Boolean completado) {
        boolean completadoNormalizado = Boolean.TRUE.equals(completado);
        proyecto.setCompletado(completadoNormalizado);

        if (completadoNormalizado) {
            if (proyecto.getFechaFin() == null) {
                proyecto.setFechaFin(LocalDate.now());
            }
            if (proyecto.getId() != null) {
                tareaProyectoRepository.actualizarCompletadaPorProyecto(proyecto.getId(), true);
            }
            return;
        }

        proyecto.setFechaFin(null);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private ProyectoDTO convertirADTO(Proyecto proyecto) {
        return new ProyectoDTO(
            proyecto.getId(),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getFechaInicio(),
            proyecto.getFechaFin(),
            proyecto.isActivo(),
            proyecto.getExcels(),
            proyecto.getCompletado()
        );
    }
}
