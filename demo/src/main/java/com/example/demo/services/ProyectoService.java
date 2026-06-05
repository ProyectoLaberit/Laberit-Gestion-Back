package com.example.demo.services;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.annotation.Auditable;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ProyectoRepository;

@Service
public class ProyectoService {

    @Autowired
    private ProyectoRepository proyectoRepository;

    /**
     * Recupera una lista de todos los proyectos filtrada por su estado de actividad y un rango de fechas.
     * * @param activo Boolean que indica si se buscan proyectos activos (true) o inactivos (false). Puede ser null.
     * @param desde Fecha de inicio del rango de búsqueda.
     * @param hasta Fecha de fin del rango de búsqueda.
     * @return Lista de objetos ProyectoDTO con la información de los proyectos encontrados.
     */
    public List<ProyectoDTO> obtenerTodosLosProyectos(Boolean activo, LocalDate desde, LocalDate hasta) {
        List<Proyecto> proyectosDB = proyectoRepository.findByFiltrosOpcionales(activo, desde, hasta);

        return proyectosDB.stream().map(p -> new ProyectoDTO(
            p.getId(),
            p.getNombre(),
            p.getDescripcion(),
            p.getFechaInicio(),
            p.getFechaFin(),
            p.isActivo(), 
            p.getExcels()
        )).collect(Collectors.toList());
    }

    /**
     * Crea un nuevo proyecto en la base de datos con los datos proporcionados en el DTO.
     * * @param dto Objeto ProyectoDTO con la información del proyecto a crear.
     * @return El ProyectoDTO recién creado y persistido.
     */
    @Auditable(
        accion = "CREAR_PROYECTO", 
        tabla = "proyecto", 
        entidad = Proyecto.class,
        descripcion = "Se creó un nuevo proyecto llamado '#{#dto.nombre}'"
    )
    public ProyectoDTO crearProyecto(ProyectoDTO dto) {
        Proyecto nuevoProyecto = new Proyecto();

        nuevoProyecto.setNombre(dto.getNombre());
        nuevoProyecto.setDescripcion(dto.getDescripcion());
        nuevoProyecto.setClockifyId(dto.getClockifyId());
        nuevoProyecto.setGitlabId(dto.getGitlabId());
        nuevoProyecto.setFechaInicio(dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now());
        nuevoProyecto.setFechaFin(dto.getFechaFin());
        nuevoProyecto.setActivo(dto.isActivo() != null ? dto.isActivo() : true);
        nuevoProyecto.setExcels(dto.getExcels());

        Proyecto guardado = proyectoRepository.save(nuevoProyecto);

        return new ProyectoDTO(
            guardado.getId(),
            guardado.getNombre(),
            guardado.getDescripcion(),
            guardado.getFechaInicio(),
            guardado.getFechaFin(),
            guardado.isActivo(),
            guardado.getExcels()
        );
    }

    /**
     * Elimina permanentemente un proyecto del sistema utilizando su identificador único.
     * * @param id Identificador único (ID) del proyecto a eliminar.
     */
    @Auditable(
        accion = "BORRAR_PROYECTO", 
        tabla = "proyecto", 
        entidad = Proyecto.class,
        descripcion = "Se eliminó del sistema el proyecto '#{#resultado.nombre}'"
    )
    public ProyectoDTO eliminarProyecto(Long id) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se puede eliminar: El proyecto con ID " + id + " no existe."));

        ProyectoDTO dto = new ProyectoDTO(
            proyecto.getId(),
            proyecto.getNombre(),
            proyecto.getDescripcion(),
            proyecto.getFechaInicio(),
            proyecto.getFechaFin(),
            proyecto.isActivo(),
            proyecto.getExcels()
        );

        proyectoRepository.delete(proyecto);
        return dto;
    }

    /**
     * Actualiza los datos de un proyecto existente con la información proporcionada.
     * * @param id Identificador único del proyecto que se va a modificar.
     * @param proyectoDTO Objeto ProyectoDTO con los nuevos datos a guardar.
     * @return El ProyectoDTO con la información actualizada.
     */
    @Auditable(
        accion = "ACTUALIZAR_PROYECTO", 
        tabla = "proyecto", 
        entidad = Proyecto.class,
        descripcion = "Se actualizaron los datos del proyecto '#{#proyectoDTO.nombre}' (ID: #{#id})"
    )
    public ProyectoDTO actualizarProyecto(Long id, ProyectoDTO proyectoDTO) {
        Proyecto proyecto = proyectoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        if(proyectoDTO.getNombre() != "" && proyectoDTO.getNombre() != null){
            proyecto.setNombre(proyectoDTO.getNombre());
        }
        if(proyectoDTO.getDescripcion() != "" && proyectoDTO.getDescripcion() != null){
            proyecto.setDescripcion(proyectoDTO.getDescripcion());
        }
        if(proyectoDTO.getClockifyId() != "" && proyectoDTO.getClockifyId() != null){
            proyecto.setClockifyId(proyectoDTO.getClockifyId());
        }
        if(proyectoDTO.getGitlabId() != "" && proyectoDTO.getGitlabId() != null){
            proyecto.setGitlabId(proyectoDTO.getGitlabId());
        }
        if(proyectoDTO.getFechaInicio() != null){
            proyecto.setFechaInicio(proyectoDTO.getFechaInicio());
        }
        if(proyectoDTO.getFechaFin() != null){
            proyecto.setFechaFin(proyectoDTO.getFechaFin());
        }
        if(proyectoDTO.isActivo() != null){
            proyecto.setActivo(proyectoDTO.isActivo());
        }
        if(proyectoDTO.getExcels() != null){
            proyecto.setExcels(proyectoDTO.getExcels());
        }
        Proyecto actualizado = proyectoRepository.save(proyecto);

        return new ProyectoDTO(
            actualizado.getId(),
            actualizado.getNombre(),
            actualizado.getDescripcion(),
            actualizado.getFechaInicio(),
            actualizado.getFechaFin(),
            actualizado.isActivo(),
            actualizado.getExcels()
        );
    }

}
