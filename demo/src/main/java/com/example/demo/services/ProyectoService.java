package com.example.demo.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.ProyectoDTO;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ProyectoRepository;

@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private GitLabService gitLabService;

    @Autowired
    private ClockifyService clockifyService;

    // Inyectamos el servicio de Clockify

    public List<ProyectoDTO> obtenerTodosLosProyectos(Boolean activo, LocalDate desde, LocalDate hasta) {
        List<Proyecto> proyectosDB = proyectoRepository.findByFiltrosOpcionales(activo, desde, hasta);

        return proyectosDB.stream().map(p -> new ProyectoDTO(
            p.getId(),
            p.getNombre(),
            p.getDescripcion(),
            p.getFechaInicio(),
            p.getFechaFin(),
            p.isActivo(),
            p.getGitlabId(),
            p.getClockifyId(),
            true,
            p.getExcels()
        )).collect(Collectors.toList());
    }

    public ProyectoDTO crearProyecto(ProyectoDTO dto) {
        Proyecto nuevoProyecto = new Proyecto();

        nuevoProyecto.setNombre(dto.getNombre());
        nuevoProyecto.setDescripcion(dto.getDescripcion());
        nuevoProyecto.setFechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now());
        nuevoProyecto.setFechaFin(dto.getFechaFin());
        nuevoProyecto.setActivo(dto.isActivo() != null ? dto.isActivo() : true);
        nuevoProyecto.setGitlabId(dto.getGitlabId());
        nuevoProyecto.setClockifyId(dto.getClockifyId());
        nuevoProyecto.setExcels(dto.getExcels());

        Proyecto guardado = proyectoRepository.save(nuevoProyecto);

        return new ProyectoDTO(
            guardado.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getFechaInicio(),
            guardado.getFechaFin(),
            guardado.isActivo(),
            guardado.getGitlabId(),
            guardado.getClockifyId(),
            true,
            guardado.getExcels()
        );
    }

    public void eliminarProyecto(Long id) {
        if (!proyectoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: El proyecto con ID " + id + " no existe.");
        }
        proyectoRepository.deleteById(id);
    }

    public ProyectoDTO actualizarProyecto(Long id, ProyectoDTO proyectoDTO) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        proyecto.setNombre(proyectoDTO.getNombre());
        proyecto.setDescripcion(proyectoDTO.getDescripcion());
        proyecto.setFechaInicio(proyectoDTO.getFechaInicio());
        proyecto.setFechaFin(proyectoDTO.getFechaFin());
        proyecto.setActivo(proyectoDTO.isActivo());
        proyecto.setExcels(proyectoDTO.getExcels());

        Proyecto actualizado = proyectoRepository.save(proyecto);

        return new ProyectoDTO(
            actualizado.getId(),
            actualizado.getNombre(),
            actualizado.getDescripcion(),
            actualizado.getFechaInicio(),
            actualizado.getFechaFin(),
            actualizado.isActivo(),
            actualizado.getGitlabId(),
            actualizado.getClockifyId(),
            true,
            actualizado.getExcels()
        );
    }

    public List<ProyectoDTO> obtenerProyectosGitLabNoRegistrados() {
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getGitlabId())
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<Map<String, Object>> proyectosGitLab = gitLabService.obtenerProyectosDeGitLab();

        return proyectosGitLab.stream()
                .filter(git -> !idsYaGuardados.contains(git.get("id").toString()))
                .map(git -> new ProyectoDTO(
                        null,
                        git.get("name").toString(),
                        git.get("description") != null ? git.get("description").toString() : "Sin descripción",
                        null,
                        null,
                        true,
                        git.get("id").toString(),
                        null,
                        false,
                        null
                    ))
                .collect(Collectors.toList());
    }

    /**
     * NUEVO MÉTODO: Trae proyectos de Clockify que no están en nuestra base de
     * datos local.
     */
     public List<ProyectoDTO> obtenerProyectosClockifyNoRegistrados() {
        // 1. Obtenemos los IDs de Clockify que ya están guardados en nuestra DB
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getClockifyId())
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // 2. Llamamos al servicio de Clockify para traer todos sus proyectos
        List<Map<String, Object>> proyectosClockify = clockifyService.obtenerProyectosDeClockify();

        // Filtramos los que NO están en nuestra lista de IDs guardados y mapeamos a DTO
        return proyectosClockify.stream()
                .filter(c -> !idsYaGuardados.contains(c.get("id").toString()))
                .map(c -> new ProyectoDTO(
                        null, // No tiene ID de DB aún
                        c.get("name").toString(),
                        "Importado desde Clockify", // Clockify API no siempre devuelve descripción en este endpoint
                        null,
                        null,
                        true,
                        null,
                        c.get("id").toString(), // El ID original de Clockify
                        false, // Marcamos que NO está en DB
                        null
                ))
                .collect(Collectors.toList());
    }
}