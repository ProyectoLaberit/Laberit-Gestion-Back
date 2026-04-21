package com.example.demo.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.SubFaseDTO;
import com.example.demo.entity.Excel;
import com.example.demo.entity.Fase;
import com.example.demo.repository.FaseRepository;

@Service
public class FaseService {

    @Autowired
    private FaseRepository faseRepository;

    @Autowired
    private ExcelService excelService;

    /**
     * Devuelve la jerarquía completa de Fases y sus Subfases.
     */
   public List<FaseDTO> obtenerJerarquiaFasesPorProyecto(Long idProyecto) {
        // 1. Buscamos el Excel vigente del proyecto
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        if (excel == null) {
            return new ArrayList<>();
        }

        // 2. Traemos SOLO las subfases que tienen tareas en este Excel
        List<Fase> subfasesActivas = faseRepository.findSubfasesConTareasPorExcel(excel.getIdExcel());
        if (subfasesActivas.isEmpty()) {
            return new ArrayList<>();
        }

        // 3. Traemos todas las fases de BD para poder buscar el nombre de los "Padres" rápidamente
        List<Fase> todasLasFases = faseRepository.findAll();

        // 4. Agrupamos las subfases activas por el ID de su fase_padre
        Map<Integer, List<SubFaseDTO>> agrupadasPorPadre = subfasesActivas.stream()
                .collect(Collectors.groupingBy(
                        Fase::getFasePadre,
                        Collectors.mapping(f -> new SubFaseDTO(f.getId(), f.getNombre()), Collectors.toList())
                ));

        // 5. Construimos la lista final fusionando a los padres con sus hijas supervivientes
        List<FaseDTO> jerarquiaFinal = new ArrayList<>();
        
        for (Map.Entry<Integer, List<SubFaseDTO>> entry : agrupadasPorPadre.entrySet()) {
            Integer idPadre = entry.getKey();
            
            // Buscamos al padre en la lista completa para saber su nombre (Desarrollo, Análisis...)
            todasLasFases.stream()
                    .filter(f -> f.getId().equals(idPadre))
                    .findFirst()
                    .ifPresent(padre -> {
                        FaseDTO dto = new FaseDTO(padre.getId(), padre.getNombre());
                        dto.setSubfases(entry.getValue());
                        jerarquiaFinal.add(dto);
                    });
        }

        return jerarquiaFinal;
    }
}
