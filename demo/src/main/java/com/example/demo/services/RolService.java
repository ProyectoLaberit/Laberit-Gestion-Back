package com.example.demo.services;

import com.example.demo.dto.PermisoDTO;
import com.example.demo.dto.RolDTO;
import com.example.demo.entity.Rol;
import com.example.demo.repository.RolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RolService {

    @Autowired
    private RolRepository rolRepository;

    public List<RolDTO> obtenerTodosLosRoles() {
        List<Rol> rolesDB = rolRepository.findAll();

        return rolesDB.stream().map(rol -> {
            List<PermisoDTO> permisosDTO = rol.getPermisos().stream()
                    .map(p -> new PermisoDTO(p.getId(), p.getNombre()))
                    .collect(Collectors.toList());

            return new RolDTO(rol.getId(), rol.getNombre(), permisosDTO);
        }).collect(Collectors.toList());
    }
}