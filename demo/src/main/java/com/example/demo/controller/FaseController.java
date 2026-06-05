package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.services.ExcelService;
import com.example.demo.services.FaseService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.demo.entity.Fase;
import com.example.demo.repository.FaseRepository;


@RestController
@RequestMapping("/api/fases")
@CrossOrigin(origins = "*")
public class FaseController {

    @Autowired
    private FaseService faseService;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private FaseRepository faseRepository;

   
    /**
     * Metodo que devuelve todas las fases existentes
     * @return ApiResponse json que contiene una lista de todas las fases existentes
     */
    @GetMapping("/todas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerTodasLasFases() {
        try {
            List<Fase> fases = faseRepository.findAll().stream()
                    .filter(f -> f.getFasePadre() == null)
                    .collect(java.util.stream.Collectors.toList());
            return new ApiResponse("Fases recuperadas", true, fases);
        } catch (Exception e) {
            return new ApiResponse("Error: " + e.getMessage(), false, null);
        }
    }
    
    /**
     * Metodo que devuelve las fases y subfases de un excel en concreto
     * @param idExcel id del excel a consultar
     * @return ApiResponse json que contiene las fases y subfases del excel
     */
    @GetMapping("/jerarquia/todas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerJerarquiaCompleta() {
        try {
            return new ApiResponse("Fases y subfases recuperadas", true, faseService.obtenerJerarquiaCompleta());
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar fases y subfases: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo que crea una nueva fase 
     * @param dto Objeto tipo FaseDTO que contiene la informacion de la fase a crear
     * @return ApiResponse con un boolean a true si la creacion a tenido exito o false si no
    */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ApiResponse crearFase(@RequestBody FaseDTO dto) {
        

        try {
            if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                return new ApiResponse("El nombre de la fase es obligatorio.", false, null);
            }
            Fase nueva = new Fase();
            nueva.setNombre(dto.getNombre().trim());
            nueva.setFasePadre(null);
            Fase guardada = faseRepository.save(nueva);
            FaseDTO respuesta = new FaseDTO(guardada.getId(), guardada.getNombre());
            return new ApiResponse("Fase creada correctamente.", true, respuesta);
        } catch (Exception e) {
            return new ApiResponse("Error al crear la fase: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo para crear una nueva subfase para una fase padre
     * @param dto Objeto FaseDTO con la informacion de la nueva subfase a crear (incluye el id de la fase padre)
     * @return ApiResponse con un boolean a true si la creacion a tenido exito o false si no 
     */
    @PostMapping("/subfase")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ApiResponse crearSubfase(@RequestBody FaseDTO dto) {
        

        try {
            if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
                return new ApiResponse("El nombre de la subfase es obligatorio.", false, null);
            }
            if (dto.getFasePadre() == null) {
                return new ApiResponse("Debes seleccionar una fase padre.", false, null);
            }
            if (!faseRepository.existsById(dto.getFasePadre())) {
                return new ApiResponse("La fase padre seleccionada no existe.", false, null);
            }
            Fase nueva = new Fase();
            nueva.setNombre(dto.getNombre().trim());
            nueva.setFasePadre(dto.getFasePadre());
            Fase guardada = faseRepository.save(nueva);
            FaseDTO respuesta = new FaseDTO(guardada.getId(), guardada.getNombre());
            return new ApiResponse("Subfase creada correctamente.", true, respuesta);
        } catch (Exception e) {
            return new ApiResponse("Error al crear la subfase: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo que devuelve las fases y subfases del excel vigente de un proyecto
     * @param idProyecto id del proyecto a consultar
     * @return ApiResponse json que contiene las fases y subfases correspondientes
     */
    @GetMapping("/{idProyecto}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerJerarquiaFases(@PathVariable Long idProyecto) {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaFasesPorProyecto(idProyecto);

            if (jerarquia.isEmpty()) {
                return new ApiResponse("El proyecto no tiene tareas o excel activo", true, jerarquia);
            }

            return new ApiResponse("Jerarquía de fases activa recuperada", true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo que devuelve las fases y subfases de un excel en concreto
     * @param idExcel id del excel a consultar
     * @return ApiResponse json que contiene las fases y subfases del excel
     */
    @GetMapping("/por-excel/{idExcel}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerJerarquiaPorExcel(@PathVariable Integer idExcel) {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaPorIdExcel(idExcel);

            if (jerarquia.isEmpty()) {
                return new ApiResponse("Este excel no tiene tareas registradas", true, jerarquia);
            }

            return new ApiResponse("Jerarquía recuperada para excel " + idExcel, true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo que devuelve el historial de excels de un proyecto concreto
     * @param idProyecto id del proyecto a consultar
     * @return ApiResponse json que contiene los excel que tienen asociados al proyecto en la base de datos
     */
    @GetMapping("/historial/{idProyecto}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse obtenerHistorialExcels(@PathVariable Long idProyecto) {
        try {
            List<HistorialExcelDTO> historial = excelService.obtenerHistorialExcels(idProyecto);
            return new ApiResponse("Historial de excels recuperado", true, historial);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar historial: " + e.getMessage(), false, null);
        }
    }

    /**
     * Metodo para comprobar el estado de finalización de una subfase dentro de un proyecto específico.
     * @param idProyecto id del proyecto a evaluar.
     * @param idSubfase id de la subfase a consultar.
     * @return ApiResponse con un booleano (true si está completa, false si no) y el conteo de tareas completadas vs totales.
     */
    @GetMapping("/completa/{idProyecto}/{idSubfase}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse subfasesCompletadas(@PathVariable Long idProyecto, @PathVariable int idSubfase) {
        boolean completada = faseService.faseCompleta(idProyecto, idSubfase);
        int[] numeros = faseService.numeroCompletadas(idProyecto, idSubfase);

        if(completada){
                return new ApiResponse("Subfase completada", true, numeros);
            }else{
                return new ApiResponse("Subfase incompleta", false, numeros);
            }
    }
    
}
