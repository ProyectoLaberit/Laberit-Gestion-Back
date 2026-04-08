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

    public List<ProyectoDTO> obtenerTodosLosProyectos(Boolean activo, LocalDate fecha) {
        List<Proyecto> proyectosDB = proyectoRepository.findByFiltrosOpcionales(activo,fecha);

        return proyectosDB.stream().map(p -> new ProyectoDTO(
            p.getId(),
            p.getNombre(),
            p.getDescripcion(),
            p.getFechaInicio(),
            p.isActivo(),
            p.getGitlabId(),
            p.getClockifyId(),
            true
        )).collect(Collectors.toList());
    }

    public ProyectoDTO actualizarProyecto(Long id, ProyectoDTO proyectoDTO) {
        // Buscar el proyecto en la base de datos por su ID
        Proyecto proyecto = proyectoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
        
        // Actualizar los datos del proyecto con la información nueva
        proyecto.setNombre(proyectoDTO.getNombre());
        proyecto.setDescripcion(proyectoDTO.getDescripcion());
        proyecto.setFechaInicio(proyectoDTO.getFechaInicio());
        proyecto.setActivo(proyectoDTO.isActivo());
        
        // Guardar los cambios en la base de datos
        Proyecto actualizado = proyectoRepository.save(proyecto);
        
        // Devolver un DTO con el proyecto ya actualizado
        return new ProyectoDTO(
            actualizado.getId(),
            actualizado.getNombre(),
            actualizado.getDescripcion(),
            actualizado.getFechaInicio(),
            actualizado.isActivo(),
            actualizado.getGitlabId(),
            actualizado.getClockifyId(),
            true
        );
    }

    public List<ProyectoDTO> obtenerProyectosGitLabNoRegistrados() {
        // Sacamos de nuestra DB los IDs de GitLab que ya conocemos
        // stream: pasa proyecto por proyecto hasta que acaben
        // map: de cada proyecto que pase se queda con la id
        // filter: Si la id del proyecto es null no pasa
        // collect: mete todos los proyectos que pasaron el filtro en la nueva lista
        List<String> idsYaGuardados = proyectoRepository.findAll()
            .stream()
            .map(p -> p.getGitlabId())
            .filter(id -> id != null)
            .collect(Collectors.toList());

        // Le pedimos a GitLab TODOS los proyectos
        List<Map<String, Object>> proyectosGitLab = gitLabService.obtenerProyectosDeGitLab();

        // filtra que proyectos de GitLab NO están en la lista de IDs guardados"
        return proyectosGitLab.stream()
            .filter(git -> !idsYaGuardados.contains(git.get("id").toString()))
            .map(git -> new ProyectoDTO(
                null,
                git.get("name").toString(),
                git.get("description") != null ? git.get("description").toString() : "Sin descripción",
                null, // GitLab no nos da la fecha de inicio igual que nuestra DB
                true,
                git.get("id").toString(), // El ID de GitLab
                null,
                false // Indicamos que NO está en la base de datos
            ))
            .collect(Collectors.toList());
    }

}