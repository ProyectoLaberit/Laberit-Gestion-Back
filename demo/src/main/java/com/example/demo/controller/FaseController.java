package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
     * Devuelve todas las fases raíz (sin fase padre) para poblar el select del paso 2.
     */
    @GetMapping("/todas")
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
     * Crea una nueva fase raíz (sin fase padre).
     */
    @PostMapping
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
     * Crea una nueva subfase asignada a una fase padre.
     */
    @PostMapping("/subfase")
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
     * Devuelve las fases/subfases del Excel VIGENTE de un proyecto.
     */
    @GetMapping("/{idProyecto}")
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
     * Devuelve las fases/subfases de un Excel CONCRETO (por su idExcel).
     * Usado para el historial: al seleccionar un excel del desplegable se cargan sus fases.
     */
    @GetMapping("/por-excel/{idExcel}")
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
     * Devuelve el historial de excels subidos para un proyecto, ordenados del más reciente al más antiguo.
     */
    @GetMapping("/historial/{idProyecto}")
    public ApiResponse obtenerHistorialExcels(@PathVariable Long idProyecto) {
        try {
            List<HistorialExcelDTO> historial = excelService.obtenerHistorialExcels(idProyecto);
            return new ApiResponse("Historial de excels recuperado", true, historial);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar historial: " + e.getMessage(), false, null);
        }
    }
}
