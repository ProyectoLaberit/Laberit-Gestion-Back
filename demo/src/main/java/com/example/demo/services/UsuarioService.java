package com.example.demo.services;

import org.springframework.stereotype.Service;
import com.example.demo.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    // Herramienta para encriptar las contraseñas
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Usuario validarUsuario(LoginRequest login) {
        // Buscamos al usuario en la base usando el email que nos pasa el Front-end
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(login.getEmail());

        // Si el Optional está vacío (no hay nadie con ese correo), devolvemos false
        if (usuarioOpt.isEmpty()) {
            return null;
        }

        // Si existe, sacamos el usuario de la base de datos
        Usuario usuarioDB = usuarioOpt.get();

        if (passwordEncoder.matches(login.getPassword(), usuarioDB.getPassword())) {
            return usuarioDB;
        } else {
            // Si es incorrecta, devolvemos null
            return null;
        }
    }

    public UsuarioDTO obtenerUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setExcels(usuario.getExcels());
        dto.setFoto(usuario.getFoto());

        // Extraemos el rol si lo tiene asignado
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            Rol rol = usuario.getRoles().iterator().next();
            dto.setRol(rol.getNombre()); // Ajusta getNombre() si tu entidad Rol usa otro nombre para el campo
        }

        return dto;
    }

    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        // Verificamos que no exista ya alguien con ese correo
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Ya existe un usuario registrado con este email.");
        }

        // Creamos el objeto para la base de datos
        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(dto.getNombre());
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setFoto("default1.png");
        nuevoUsuario.setExcels(dto.getExcels() != null ? dto.getExcels() : false);

        // Buscamos el rol 3 (Empleado) en la base de datos
        Rol rolPorDefecto = rolRepository.findById(3)
                .orElseThrow(() -> new RuntimeException("Error crítico: El rol 3 no existe en la base de datos."));

        // Se lo asignamos al nuevo usuario
        nuevoUsuario.getRoles().add(rolPorDefecto);

        // Encriptamos la contraseña antes de guardarla
        String contrasenaHasheada = passwordEncoder.encode(dto.getPassword());
        nuevoUsuario.setPassword(contrasenaHasheada);

        // Guardamos en la tabla
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        // 5. Devolvemos la confirmación
        UsuarioDTO respuesta = new UsuarioDTO();
        respuesta.setId(guardado.getId());
        respuesta.setNombre(guardado.getNombre());
        respuesta.setEmail(guardado.getEmail());
        respuesta.setExcels(guardado.getExcels());

        return respuesta;
    }

    public void eliminarUsuario(Integer id) {
        // Comprobamos si el usuario existe
        if (!usuarioRepository.existsById(id)) {
            // Si no existe, lanzamos un error que capturaremos en el Controller
            throw new RuntimeException("Error: No se puede eliminar. El usuario con ID " + id + " no existe.");
        }

        // Si existe, lo borramos
        // Al borrar el usuario, gracias a JPA y la configuración por defecto,
        // también se deberían borrar sus relaciones en la tabla usuario_x_rol
        // automáticamente
        usuarioRepository.deleteById(id);
    }

    public void cambiarContrasena(Integer id, String passwordVieja, String passwordNueva) {
        // Buscamos al usuario en la base de datos
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        // Comprobamos que la contraseña "vieja" que ha introducido coincide con la real
        if (!passwordEncoder.matches(passwordVieja, usuario.getPassword())) {
            throw new RuntimeException("Error: La contraseña actual es incorrecta.");
        }

        // Encriptamos la contraseña
        String nuevaHasheada = passwordEncoder.encode(passwordNueva);

        // La actualizamos y guardamos
        usuario.setPassword(nuevaHasheada);
        usuarioRepository.save(usuario);
    }

    private final String CARPETA_FOTOS = "uploads/fotos_perfil/";

    public void cambiarFoto(Integer id, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        if (archivo == null || archivo.isEmpty()) {
            return;
        }

        try {
            // Aseguramos que la carpeta exista en el programa
            Path directorio = Paths.get(CARPETA_FOTOS);
            if (!Files.exists(directorio)) {
                Files.createDirectories(directorio);
            }

            // Creamos el nombre con milisegundos para que sea único
            String nombreOriginal = archivo.getOriginalFilename();
            String nombreFinal = System.currentTimeMillis() + nombreOriginal;

            // Guardamos el archivo en el programa
            Path rutaArchivo = directorio.resolve(nombreFinal);
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            // Guardamos el nombre en la base de datos
            usuario.setFoto(nombreFinal);
            usuarioRepository.save(usuario);

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar la foto: " + e.getMessage());
        }
    }

    public void cambiarRol(Integer idUsuario, String rolNuevo) {
        // Buscamos al usuario
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        // Convertimos el texto del DTO (ej: "1") a número entero
        Integer idRol;
        try {
            idRol = Integer.parseInt(rolNuevo);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Error: El rol enviado no es un número válido.");
        }

        // Buscamos que el nuevo rol exista de verdad en la base de datos
        Rol nuevoRolObj = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Error: El rol especificado no existe en el sistema."));

        // Le quitamos los roles viejos y le ponemos el nuevo
        usuario.getRoles().clear();
        usuario.getRoles().add(nuevoRolObj);

        // Guardamos en la base
        usuarioRepository.save(usuario);
    }

    public UsuarioDTO actualizarUsuario(Integer id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            usuario.setNombre(dto.getNombre());
        }

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("El email ya está en uso");
            }
            usuario.setEmail(dto.getEmail());
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        UsuarioDTO response = new UsuarioDTO();
        response.setId(actualizado.getId());
        response.setNombre(actualizado.getNombre());
        response.setEmail(actualizado.getEmail());
        return response;
    }
}
