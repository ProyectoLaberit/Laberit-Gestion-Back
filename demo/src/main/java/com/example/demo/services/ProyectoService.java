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
            true
        )).collect(Collectors.toList());
    }

    public ProyectoDTO crearProyecto(ProyectoDTO dto) {
        // Creamos una Entity vacía (una fila nueva para la base de datos)
        Proyecto nuevoProyecto = new Proyecto();
        
        // Rellenamos los datos con lo que viene del Front-end
        nuevoProyecto.setNombre(dto.getNombre());
        nuevoProyecto.setDescripcion(dto.getDescripcion());
        
        // Si el front-end no manda fecha de inicio, ponemos la de hoy por defecto
        nuevoProyecto.setFechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now());
        
        // La fecha fin puede ser null, la metemos tal cual
        nuevoProyecto.setFechaFin(dto.getFechaFin());
        
        // Un proyecto nuevo siempre debería nacer "activo" por defecto si no dicen lo contrario
        nuevoProyecto.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        
        nuevoProyecto.setGitlabId(dto.getGitlabId());
        nuevoProyecto.setClockifyId(dto.getClockifyId());

        // 3. Guardamos en la base de datos (Esto genera el ID automáticamente)
        Proyecto guardado = proyectoRepository.save(nuevoProyecto);

        // 4. Devolvemos el proyecto guardado convertido en DTO para confirmar al Front-end
        return new ProyectoDTO(
            guardado.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getFechaInicio(),
            guardado.getFechaFin(),
            guardado.getActivo(),
            guardado.getGitlabId(),
            guardado.getClockifyId(),
            true // Ya está en base de datos
        );
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
            actualizado.getFechaFin(),
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
                null,
                true,
                git.get("id").toString(), // El ID de GitLab
                null,
                false // Indicamos que NO está en la base de datos
            ))
            .collect(Collectors.toList());
    }

}