package com.example.demo.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.SubFaseDTO;
import com.example.demo.entity.Fase;
import com.example.demo.repository.FaseRepository;

@Service
public class FaseService {

    @Autowired
    private FaseRepository faseRepository;

    /**
     * Devuelve la jerarquía completa de Fases y sus Subfases.
     */
    public List<FaseDTO> obtenerJerarquiaFases() {
        List<Fase> todas = faseRepository.findAll();

        return todas.stream()
            // Filtramos solo los padres
            .filter(f -> f.getFasePadre() == null)
            .map(padre -> {
                FaseDTO dto = new FaseDTO(padre.getId(), padre.getNombre());
                
                // Buscamos los hijos de este padre
                List<SubFaseDTO> hijos = todas.stream()
                    .filter(h -> h.getFasePadre() != null && h.getFasePadre().equals(padre.getId()))
                    .map(h -> new SubFaseDTO(h.getId(), h.getNombre()))
                    .collect(Collectors.toList());
                
                dto.setSubfases(hijos);
                return dto;
            })
            .collect(Collectors.toList());
    }
}