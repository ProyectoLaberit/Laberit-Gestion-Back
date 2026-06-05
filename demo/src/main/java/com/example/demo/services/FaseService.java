package com.example.demo.services;

import com.example.demo.repository.TareaProyectoRepository;
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

    private final TareaProyectoRepository tareaProyectoRepository;

    @Autowired
    private FaseRepository faseRepository;

    @Autowired
    private ExcelService excelService;

    FaseService(TareaProyectoRepository tareaProyectoRepository) {
        this.tareaProyectoRepository = tareaProyectoRepository;
    }

    /**
     * Devuelve la jerarquía completa de Fases y sus Subfases para el Excel VIGENTE
     * del proyecto.
     */
    public List<FaseDTO> obtenerJerarquiaFasesPorProyecto(Long idProyecto) {
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
        if (excel == null) {
            return new ArrayList<>();
        }
        return obtenerJerarquiaPorIdExcel(excel.getIdExcel());
    }

    /**
     * Devuelve la jerarquía de Fases y Subfases para un Excel concreto (por
     * idExcel).
     * Reutilizado tanto por el vigente como por el historial.
     */
    public List<FaseDTO> obtenerJerarquiaPorIdExcel(Integer idExcel) {
        List<Fase> subfasesActivas = faseRepository.findSubfasesConTareasPorExcel(idExcel);
        if (subfasesActivas.isEmpty()) {
            return new ArrayList<>();
        }

        List<Fase> todasLasFases = faseRepository.findAll();

        Map<Integer, List<SubFaseDTO>> agrupadasPorPadre = subfasesActivas.stream()
                .collect(Collectors.groupingBy(
                        Fase::getFasePadre,
                        Collectors.mapping(f -> new SubFaseDTO(f.getId(), f.getNombre()), Collectors.toList())));

        List<FaseDTO> jerarquiaFinal = new ArrayList<>();

        for (Map.Entry<Integer, List<SubFaseDTO>> entry : agrupadasPorPadre.entrySet()) {
            Integer idPadre = entry.getKey();

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

    public boolean faseCompleta(Long idProyecto, int idFase) {
        boolean completadas = tareaProyectoRepository.estanTodasCompletadasPorProyectoYFase(idProyecto, idFase);
        return completadas;
    }

    public int[] numeroCompletadas(Long idProyecto, int idFase) {
        int[] result = { tareaProyectoRepository.countByIdProyectoAndIdFaseAndCompletadaTrue(idProyecto, idFase),
                tareaProyectoRepository.countByIdProyectoAndIdFase(idProyecto, idFase) };
        return result;
    }
}
