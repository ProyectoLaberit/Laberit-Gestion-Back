package com.example.demo.services;

import com.example.demo.dto.DepartamentoDTO;
import com.example.demo.entity.Departamento;
import com.example.demo.repository.DepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartamentoService {

    @Autowired
    private DepartamentoRepository deptoRepo;

    // 1. LISTAR
    public List<DepartamentoDTO> listarTodos() {
        return deptoRepo.findAll().stream()
                .map(DepartamentoDTO::new)
                .collect(Collectors.toList());
    }

    // 2. CREAR
    public void crear(DepartamentoDTO dto) {
        Departamento depto = new Departamento();
        depto.setNombre(dto.getNombre());

        if (dto.getIdPadre() != null) {
            Departamento padre = deptoRepo.findById(dto.getIdPadre())
                    .orElseThrow(() -> new RuntimeException("El departamento padre no existe"));
            depto.setPadre(padre);
        }

        deptoRepo.save(depto);
    }

    // 3. ACTUALIZAR
    public void actualizar(Integer id, DepartamentoDTO dto) {
        Departamento depto = deptoRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Departamento no encontrado con ID: " + id));

        depto.setNombre(dto.getNombre());

        // Validación de jerarquía: No puede ser su propio padre
        if (dto.getIdPadre() != null) {
            if (id.equals(dto.getIdPadre())) {
                throw new RuntimeException("Un departamento no puede ser su propio padre");
            }
            Departamento padre = deptoRepo.findById(dto.getIdPadre())
                    .orElseThrow(() -> new RuntimeException("El departamento padre no existe"));
            depto.setPadre(padre);
        } else {
            depto.setPadre(null);
        }

        deptoRepo.save(depto);
    }

    // 4. ELIMINAR
    public void eliminar(Integer id) {
        if (!deptoRepo.existsById(id)) {
            throw new RuntimeException("El departamento no existe");
        }
        deptoRepo.deleteById(id);
    }
}