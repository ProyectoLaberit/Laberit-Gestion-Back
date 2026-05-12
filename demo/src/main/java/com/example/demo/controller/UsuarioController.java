package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.services.UsuarioService;
import com.example.demo.services.AuditService;
import com.example.demo.entity.Usuario;
import com.example.demo.security.JwtUtil;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuditService auditService;

    // ── Helpers de autenticación ──────────────────────────────────────────────

    private String getEmailAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    private boolean tieneRol(String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + rol));
    }

    private boolean esAdmin() {
        return tieneRol("ADMINISTRADOR") || tieneRol("SUPERADMINISTRADOR");
    }
    private boolean esSuperAdmin() {
        return tieneRol("SUPERADMINISTRADOR");
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ApiResponse verificar(@RequestBody LoginRequest login) {
        Usuario usuario = usuarioService.validarUsuario(login);

        if (usuario != null) {
            String token = jwtUtil.generarToken(usuario);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("id", usuario.getId());
            data.put("nombre", usuario.getNombre());
            data.put("email", usuario.getEmail());
            data.put("foto", usuario.getFoto());
            if (!usuario.getRoles().isEmpty()) {
                data.put("rol", usuario.getRoles().get(0).getNombre());
            }

            return new ApiResponse("Login exitoso", true, data);
        } else {
            return new ApiResponse("Credenciales inválidas", false, null);
        }
    }

    @PostMapping("/forgot-password")
    public ApiResponse forgotPassword(@RequestBody UsuarioDTO dto) {
        try {
            usuarioService.solicitarRecuperacionPassword(dto.getEmail());
            return new ApiResponse(
                    "Si el email existe en el sistema, recibirás un enlace para restablecer la contraseña.",
                    true, null);
        } catch (Exception e) {
            return new ApiResponse("No se pudo procesar la solicitud: " + e.getMessage(), false, null);
        }
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(@RequestBody UsuarioDTO dto) {
        try {
            usuarioService.resetearPassword(dto.getResetToken(), dto.getPasswordNueva());
            return new ApiResponse("Contraseña restablecida correctamente.", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error inesperado al restablecer la contraseña: " + e.getMessage(), false, null);
        }
    }

    /**
     * Listar todos los usuarios: solo SuperAdministrador.
     */
    @GetMapping
    public ApiResponse listarUsuarios() {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para ver la lista de usuarios.", false, null);
        }
        try {
            List<UsuarioDTO> usuarios = usuarioService.listarTodosLosUsuarios();
            return new ApiResponse("Usuarios recuperados", true, usuarios);
        } catch (Exception e) {
            return new ApiResponse("Error al obtener usuarios: " + e.getMessage(), false, null);
        }
    }

    /**
     * Obtener un usuario por ID: SuperAdministrador o el propio usuario.
     */
    @GetMapping("/{id}")
    public ApiResponse obtenerUsuarioPorId(@PathVariable Integer id) {
        String emailAuth = getEmailAutenticado();
        UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
        if (!esSuperAdmin() && !propio.getId().equals(id)) {
            return new ApiResponse("No tienes permisos para ver este usuario.", false, null);
        }
        try {
            UsuarioDTO usuario = usuarioService.obtenerUsuarioPorId(id);
            return new ApiResponse("Usuario encontrado", true, usuario);
        } catch (Exception e) {
            return new ApiResponse("Error: " + e.getMessage(), false, null);
        }
    }


    @PostMapping
    public ApiResponse crearUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para crear usuarios. Se requiere rol ADMIN.", false, null);
        }
        try {
            UsuarioDTO usuarioCreado = usuarioService.crearUsuario(usuarioDTO);
            auditService.registrar(
                AuditService.CREACION_USUARIO,
                "Usuario creado: " + usuarioDTO.getEmail() + " con rol " + usuarioDTO.getRol(),
                null,
                usuarioCreado.getId()
            );
            return new ApiResponse("Usuario creado con éxito", true, usuarioCreado);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error inesperado al crear el usuario: " + e.getMessage(), false, null);
        }
    }

    /**
     * Eliminar usuario: solo ADMIN (garantizado por SecurityConfig).
     * ADMIN no puede eliminarse a sí mismo.
     */
    @DeleteMapping("/{id}")
    public ApiResponse eliminarUsuario(@PathVariable Integer id) {
        try {
            String emailAuth = getEmailAutenticado();
            UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
            if (propio.getId().equals(id)) {
                return new ApiResponse("No puedes eliminarte a ti mismo.", false, null);
            }
            usuarioService.eliminarUsuario(id);
            auditService.registrar(
                AuditService.BORRADO_USUARIO,
                "Usuario ID " + id + " eliminado.",
                null,
                id
            );
            return new ApiResponse("Usuario eliminado correctamente", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error al intentar eliminar el usuario: " + e.getMessage(), false, null);
        }
    }

    /**
     * Cambiar contraseña:
     * - USER solo puede cambiar la suya propia.
     * - MANAGER puede cambiar la de usuarios con rol USER.
     * - ADMIN puede cambiar la de cualquiera.
     */
    @PutMapping("/{id}/password")
    public ApiResponse cambiarContrasena(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            if (dto.getPasswordVieja() == null || dto.getPasswordNueva() == null) {
                return new ApiResponse("Faltan datos: Debes enviar la contraseña vieja y la nueva.", false, null);
            }

            String emailAuth = getEmailAutenticado();
            if (!puedeEditarUsuario(id, emailAuth)) {
                return new ApiResponse("No tienes permisos para modificar este usuario.", false, null);
            }

            usuarioService.cambiarContrasena(id, dto.getPasswordVieja(), dto.getPasswordNueva());
            return new ApiResponse("Contraseña actualizada correctamente.", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error inesperado al cambiar la contraseña: " + e.getMessage(), false, null);
        }
    }

    /**
     * Cambiar foto:
     * Mismas reglas que cambiar contraseña.
     */
    @PutMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    public ApiResponse cambiarFoto(
            @PathVariable Integer id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {
        try {
            String emailAuth = getEmailAutenticado();
            if (!puedeEditarUsuario(id, emailAuth)) {
                return new ApiResponse("No tienes permisos para modificar este usuario.", false, null);
            }

            usuarioService.cambiarFoto(id, archivo);
            return new ApiResponse("Proceso de actualización de foto completado.", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error inesperado al guardar la foto: " + e.getMessage(), false, null);
        }
    }

    /**
     * Cambiar rol: solo ADMIN.
     */
    @PutMapping("/{id}/rol")
    public ApiResponse cambiarRol(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para cambiar roles. Se requiere rol ADMIN.", false, null);
        }
        try {
            if (dto.getRol() == null || dto.getRol().trim().isEmpty()) {
                return new ApiResponse("No se solicitó cambio de rol.", true, null);
            }

            String emailAuth = getEmailAutenticado();
            UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
            if (propio.getId().equals(id)) {
                return new ApiResponse("No puedes cambiar tu propio rol.", false, null);
            }

            usuarioService.cambiarRol(id, dto.getRol());
            auditService.registrar(
                AuditService.CAMBIO_ROL,
                "Rol del usuario ID " + id + " cambiado a: " + dto.getRol(),
                null,
                id
            );
            return new ApiResponse("Rol actualizado correctamente.", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error inesperado al cambiar el rol: " + e.getMessage(), false, null);
        }
    }

    /**
     * Actualizar nombre/email/foto del perfil:
     * Mismas reglas que cambiar contraseña.
     */
    @PutMapping("/{id}")
    public ApiResponse actualizarUsuario(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            String emailAuth = getEmailAutenticado();
            if (!puedeEditarUsuario(id, emailAuth)) {
                return new ApiResponse("No tienes permisos para modificar este usuario.", false, null);
            }

            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, dto);
            return new ApiResponse("Usuario actualizado correctamente", true, actualizado);
        } catch (Exception e) {
            return new ApiResponse(e.getMessage(), false, null);
        }
    }

    // ── Lógica de permisos ────────────────────────────────────────────────────

    /**
     * Determina si el usuario autenticado puede editar al usuario con 'id'.
     * - ADMIN: puede editar a cualquiera.
     * - MANAGER: solo puede editar a usuarios con rol USER.
     * - USER: solo puede editarse a sí mismo.
     */
    private boolean puedeEditarUsuario(Integer idObjetivo, String emailAuth) {
        if (esAdmin()) return true;

        UsuarioDTO objetivo = usuarioService.obtenerUsuarioPorId(idObjetivo);
        if (objetivo == null) return false;

        // Empleado solo puede editarse a sí mismo
        UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
        return propio != null && propio.getId().equals(idObjetivo);
    }
}
