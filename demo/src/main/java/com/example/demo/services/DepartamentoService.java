package com.example.demo.services;

import com.example.demo.annotation.Auditable;
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

    /**
     * Método para listar todos los departamentos.
     * Este método recupera las entidades Departamento de la base de datos y las transforma en DTOs.
     * La transformación a DTO es crucial para evitar el bucle infinito que ocurre al serializar entidades con relaciones recursivas (departamento -> padre -> departamento).
     * Al usar DTOs, controlamos exactamente qué información se expone en la API, evitando problemas de serialización y mejorando la seguridad.
     *
     * @return Lista de DepartamentoDTO con información del departamento y su padre (si existe).
     */
    public List<DepartamentoDTO> listarTodos() {
        return deptoRepo.findAll().stream()
                .map(DepartamentoDTO::new)
                .collect(Collectors.toList());
    }

    // 2. CREAR

    /**
     * Método para crear un nuevo departamento.
     * Recibe un DepartamentoDTO que contiene el nombre del departamento y opcionalmente el ID del departamento padre.
     * El método valida que el departamento padre exista (si se proporciona) y luego guarda el nuevo departamento en la base de datos.
     *
     * @param dto Objeto de transferencia de datos que contiene la información del nuevo departamento.
     */
    @Auditable(
        accion = "CREAR_DEPARTAMENTO", 
        tabla = "departamento", 
        entidad = Departamento.class,
        descripcion = "Se creó el departamento '#{#dto.nombre}'"
    )
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

        /**
        * Método para actualizar un departamento existente.
        * Recibe el ID del departamento a actualizar y un DepartamentoDTO con la nueva información.
        * El método valida que el departamento exista, actualiza su nombre y su relación con el departamento padre (si se proporciona).
        * También incluye una validación para evitar que un departamento se convierta en su propio padre, lo que causaría un bucle infinito.
        *
        * @param id  ID del departamento a actualizar.
        * @param dto Objeto de transferencia de datos que contiene la nueva información del departamento.
        */
       @Auditable(
        accion = "ACTUALIZAR_DEPARTAMENTO", 
        tabla = "departamento", 
        entidad = Departamento.class,
        descripcion = "Se actualizó el departamento '#{#dto.nombre}' (ID: #{#id})"
    )
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

        /**
        * Método para eliminar un departamento.
        * Recibe el ID del departamento a eliminar y valida que exista antes de eliminarlo.
        * Es importante manejar la eliminación con cuidado, ya que si un departamento tiene sub-departamentos dependientes, la eliminación podría fallar debido a restricciones de integridad referencial en la base de datos.
        *
        * @param id ID del departamento a eliminar.
        */
       @Auditable(
        accion = "BORRAR_DEPARTAMENTO", 
        tabla = "departamento", 
        entidad = Departamento.class,
        descripcion = "Se eliminó el departamento con ID: #{#id}"
    )
    public void eliminar(Integer id) {
        if (!deptoRepo.existsById(id)) {
            throw new RuntimeException("El departamento no existe");
        }
        deptoRepo.deleteById(id);
    }
}