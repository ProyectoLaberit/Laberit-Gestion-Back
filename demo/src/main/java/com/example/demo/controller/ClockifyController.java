package com.example.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ClockifyTareaDTO;
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.services.ClockifyService;


@RestController
@RequestMapping("/api/clockify")
@CrossOrigin(origins = "*")

public class ClockifyController {

    @Autowired
    private ClockifyService clockifyService;

    /**
     * Metodo que recoge el id de un proyecto y el nombre de una subfase y devuelve una lista con las tareas pertenecientes a esa subfase
     * @param id id del proyecto de nuestra base de datos
     * @param subfase nombre de la subfase en clockify
     * @return ApiResponse json que contiene una lista de las tareas de esa subfase
    */
    @GetMapping("/{id}/{subfase}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerTareas(@PathVariable Long id, @PathVariable String subfase) {

        List<ClockifyTareaDTO> todasLasTareas = clockifyService.obtenerTareasPorSubfase(id, subfase);

        System.out.println(todasLasTareas);


        return new ApiResponse("a", true, todasLasTareas);
    }
    
    /**
     * Metodo que devuelve los proyectos de clockify que no tienen ningun proyecto asociado en la base de datos
     * @return ApiResponse json que contiene los proyectos de clockify sin ningun proyecto asociado en la base de datos
     */
    @GetMapping("/externos")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerProyectosClockifyNoBD() {

        List<ProyectoClockifyDTO> proyectosClockify = clockifyService.obtenerProyectosNuevosDTO();

        return new ApiResponse(null, true, proyectosClockify);
        
    }

    /**
     * Metodo que devuelve las imputaciones de clockify mal escritas de un proyecto
     * @param id id del proyecto en la base de datos
     * @return ApiResponse json que contiene las imputaciones del proyecto mal escritas, se comprueban por las tareas en la base de datos, el id de gitlab y las subfases e nla base de datos
     */
    @GetMapping("/fallos/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse getFallidos(@PathVariable Long id) {

        List<ClockifyTareaDTO> tareasFallidas = clockifyService.obtenerEntradasInvalidas(id);
        return new ApiResponse("tareasFaliidas", true, tareasFallidas);
    }
    

    /**
     * Metodo que sincroniza los proyectos de clockify y sube los proyectos de clockify que no estan en la base de datos a la base de datos
     * @param idProyecto id del proyecto del que se suben las imputaciones de clockify
     * @return ApiResopnse con un boolean a true si no hubo fallos en la sincronizacion y false si hubo problemas
     */
    @PostMapping("/sincronizar/{idProyecto}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ApiResponse sincronizarConClockify(@PathVariable Long idProyecto) {
        try {
            int cantidad = clockifyService.sincronizarImputaciones(idProyecto);
            return new ApiResponse("Sincronización completada. Se importaron " + cantidad + " imputaciones nuevas.", true, cantidad);
        } catch (Exception e) {
            return new ApiResponse("Error al sincronizar: " + e.getMessage(), false, null);
        }
    }
}
