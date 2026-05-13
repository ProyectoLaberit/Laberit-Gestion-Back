package com.example.demo.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        p.getExcels()
    )).collect(Collectors.toList());
}
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
    @Auditable(
        accion = "BORRAR_PROYECTO", 
        tabla = "proyecto", 
        entidad = Proyecto.class,
        descripcion = "Se eliminó del sistema el proyecto '#{#resultado.nombre}'"
    )
    public void eliminarProyecto(Long id) {
        if (!proyectoRepository.existsById(id)) {
            throw new RuntimeException("No se puede eliminar: El proyecto con ID " + id + " no existe.");
        }
        proyectoRepository.deleteById(id);
    }

    @Auditable(
        accion = "ACTUALIZAR_PROYECTO", 
        tabla = "proyecto", 
        entidad = Proyecto.class,
        descripcion = "Se actualizaron los datos del proyecto '#{#proyectoDTO.nombre}' (ID: #{#id})"
    )
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
            actualizado.getExcels()
        );
    }

}