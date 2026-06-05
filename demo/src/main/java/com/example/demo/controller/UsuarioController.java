package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.services.UsuarioService;
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

    private String getEmailAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    private boolean tieneRol(String rol) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return false;
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

    /**
     * Metodo para iniciar sesion en la aplicacion
     * 
     * @param login objeto tipo LoginRequest con los datos de login
     * @return Apiresponse con un boolean a true si se verifica el inicio de sesion
     *         y un json con los datos del usuario y el token de autenticacion o un
     *         boolean false si no se pasa la verificacion
     */
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

    /**
     * metodo para recuperacion de contraseña
     * 
     * @param dto Objeto tipo usuarioDTO que contiene la informacion necesaria para
     *            el cambio de contraseña
     * @return Apiresponse con boolean true si eiste un ususario con ese correo y el
     *         mensaje de que vas a recibir un correo electronico para el cambio de
     *         contraseña o false si no existe el usuario con ese correo o oçhubo
     *         algun fallo en la recuperacion
     */
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

    /**
     * Metodo para cambiar la contraseña de un usuario
     * 
     * @param dto objeto tipo UsuarioDTO que contiene la informacoon del usuario a
     *            cambiar la contraseña y la contraseña nueva
     * @return ApiResponse con boolean true si el cambio se realiza correctamente o
     *         false si no
     */
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
     * Metodo que devuelve todos los usuarios existentes en la base de datos, solo
     * para admin o superadmin
     * 
     * @return ApiResponse json que contiene la lista de usuarios y su informacion
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
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

    /**
     * Metodo para obtener la informacion de un usuario (solo admin o superadmin) o
     * del propio usuario actual
     * 
     * @param id id del usuario a consultar la informacion
     * @return ApiResponse json que contiene la informacion del usuario consultado
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
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

    /**
     * Metodo para crear un usuario
     * 
     * @param usuarioDTO objeto tipo usuarioDTO con la informacion del nuevo usuario
     *                   a crear
     * @return ApiResponse con boleano true si el usuario se creo correctamente y un
     *         json con la informacion del nuevo usuario o false si no se creao
     *         correctamente
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ApiResponse crearUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para crear usuarios. Se requiere rol ADMIN.", false, null);
        }
        try {
            UsuarioDTO usuarioCreado = usuarioService.crearUsuario(usuarioDTO);

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
    /**
     * Metodo para eliminar un usuario
     * 
     * @param id id del usuario a eliminar
     * @return ApiResponse con boleano true si el usuario se elimina correctamente o
     *         false si no
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMINISTRADOR')")
    public ApiResponse eliminarUsuario(@PathVariable Integer id) {
        try {
            String emailAuth = getEmailAutenticado();
            UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
            if (propio.getId().equals(id)) {
                return new ApiResponse("No puedes eliminarte a ti mismo.", false, null);
            }
            usuarioService.eliminarUsuario(id);

            return new ApiResponse("Usuario eliminado correctamente", true, null);
        } catch (RuntimeException e) {
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            return new ApiResponse("Error al intentar eliminar el usuario: " + e.getMessage(), false, null);
        }
    }

    /**
     * Endpoint para cambiar la contraseña de un usuario aplicando reglas de control
     * de acceso por rol.
     * Permite a un usuario cambiar su propia clave, a un MANAGER modificar las de
     * usuarios básicos y a un ADMIN cambiar cualquiera.
     *
     * @param id  Identificador único del usuario al que se le modificará la
     *            contraseña.
     * @param dto Objeto de transferencia que contiene las contraseñas vieja y
     *            nueva.
     * @return ApiResponse con el resultado de la operación.
     */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ApiResponse cambiarContrasena(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            // Valida que se hayan proporcionado obligatoriamente ambas contraseñas
            if (dto.getPasswordVieja() == null || dto.getPasswordNueva() == null) {
                return new ApiResponse("Faltan datos: Debes enviar la contraseña vieja y la nueva.", false, null);
            }

            // Obtiene el email del usuario que ha iniciado sesión actualmente
            String emailAuth = getEmailAutenticado();

            // Comprueba las restricciones y permisos de edición según el rol
            if (!puedeEditarUsuario(id, emailAuth)) {
                return new ApiResponse("No tienes permisos para modificar este usuario.", false, null);
            }

            // Invoca al servicio para validar la clave actual y persistir el cambio
            usuarioService.cambiarContrasena(id, dto.getPasswordVieja(), dto.getPasswordNueva());
            return new ApiResponse("Contraseña actualizada correctamente.", true, null);
        } catch (RuntimeException e) {
            // Captura errores controlados de lógica de negocio (ej. la contraseña vieja no
            // coincide)
            return new ApiResponse(e.getMessage(), false, null);
        } catch (Exception e) {
            // Captura cualquier otro fallo técnico o imprevisto del sistema
            return new ApiResponse("Error inesperado al cambiar la contraseña: " + e.getMessage(), false, null);
        }
    }

    /**
     * metodo para cambiar el icono del perfil del usuario
     * Admin puede cambiar todos los iconos
     * Manager solo los usuarios con rol User
     * Usuarios solo la suya propia
     * 
     * @param id      id del usuario del que se quiere cambiar el icono
     * @param archivo nuevo icono
     * @return ApiResponse con boleano true si el icono se cambia correctamente o
     *         false si no
     */
    @PutMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
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
    /**
     * Metodo para cambiar el rol de un usuario (solo administradores)
     * 
     * @param id  id del usuario a cambiar
     * @param dto objeto tipo usuarioDTO con el nuevo rol
     * @return ApiResponse con boleano true si el rol se cambia correctamente o
     *         false si no
     */
    @PutMapping("/{id}/rol")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
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
    /**
     * metodo para cambiar el nombre correo o foto de perfil
     * Admin puede cambiar todos los usuarios
     * Manager solo los usuarios con rol User
     * Usuarios solo a si mismos
     * 
     * @param id  id del usuario a cambiar
     * @param dto objeto tipo usuarioDTO con la nueva informacion del usuario
     * @return ApiResponse con boleano true si el usuario se actualiza correctamente
     *         y un json con la informacion nueva del usuario o false si no
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
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

    /**
     * Determina si el usuario autenticado puede editar al usuario con 'id'.
     * - ADMIN: puede editar a cualquiera.
     * - MANAGER: solo puede editar a usuarios con rol USER.
     * - USER: solo puede editarse a sí mismo.
     */
    /**
     * metodo que determina si el usuario actual puede editar al usuario que se
     * consulta
     * - ADMIN: puede editar a cualquiera.
     * - MANAGER: solo puede editar a usuarios con rol USER.
     * - USER: solo puede editarse a sí mismo.
     * 
     * @param idObjetivo id del usuario a cambiar
     * @param emailAuth  mail del usuario
     * @return boolean true si puede editar o false si no
     */
    private boolean puedeEditarUsuario(Integer idObjetivo, String emailAuth) {
        if (esAdmin())
            return true;

        UsuarioDTO objetivo = usuarioService.obtenerUsuarioPorId(idObjetivo);
        if (objetivo == null)
            return false;

        // Empleado solo puede editarse a sí mismo
        UsuarioDTO propio = usuarioService.obtenerUsuarioPorEmail(emailAuth);
        return propio != null && propio.getId().equals(idObjetivo);
    }
}
