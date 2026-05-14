package com.example.demo.services;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.entity.Rol;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.RolRepository;
import com.example.demo.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import com.example.demo.annotation.Auditable;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${brevo.sender.email:${SENDER_EMAIL:}}")
    private String senderEmailConfigurado;

    @Value("${brevo.sender.name:${SENDER_NAME:Labertir}}")
    private String senderNameConfigurado;

    @Value("${app.reset-password-url}")
    private String resetPasswordUrl;

    private static final int PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final String CARPETA_FOTOS = "uploads/fotos_perfil/";

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public Usuario validarUsuario(LoginRequest login) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(login.getEmail());
        if (usuarioOpt.isEmpty()) {
            return null;
        }

        Usuario usuarioDB = usuarioOpt.get();
        if (passwordEncoder.matches(login.getPassword(), usuarioDB.getPassword())) {
            return usuarioDB;
        }

        return null;
    }

    public List<UsuarioDTO> listarTodosLosUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(this::mapearADTO)
                .collect(java.util.stream.Collectors.toList());
    }

    public UsuarioDTO obtenerUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        return mapearADTO(usuario);
    }

    public UsuarioDTO obtenerUsuarioPorId(Integer id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElse(null);

        if (usuario == null) return null;
        return mapearADTO(usuario);
    }

    private UsuarioDTO mapearADTO(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setExcels(usuario.getExcels());
        dto.setFoto(usuario.getFoto());

        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            Rol rol = usuario.getRoles().iterator().next();
            dto.setRol(rol.getNombre());
        }

        return dto;
    }
   @Auditable(
        accion = "CREAR_USUARIO", 
        tabla = "usuario", 
        entidad = Usuario.class,
        descripcion = "Se creó el usuario '#{#dto.nombre}' con email '#{#dto.email}'"
    )
    public UsuarioDTO crearUsuario(UsuarioDTO dto) {
        if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Error: Ya existe un usuario registrado con este email.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(dto.getNombre());
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setFoto("default1.png");
        nuevoUsuario.setExcels(dto.getExcels() != null ? dto.getExcels() : false);

        // Determinar rol a asignar
        Rol rolAsignado;
        if (dto.getRol() != null && !dto.getRol().trim().isEmpty()) {
            try {
                // Transformamos el String ("1", "2" o "3") a un número entero
                int idRol = Integer.parseInt(dto.getRol().trim());
                
                // Buscamos directamente por ese ID
                rolAsignado = rolRepository.findById(idRol)
                        .orElseThrow(() -> new RuntimeException("Error: El rol especificado (" + idRol + ") no existe en la base de datos."));
                        
            } catch (NumberFormatException e) {
                throw new RuntimeException("Error: El rol enviado no es un número válido.");
            }
        } else {
            // Si viene null o vacío, asignamos empleado
            rolAsignado = rolRepository.findById(3)
                    .orElseThrow(() -> new RuntimeException("Error: El rol Empleado no existe en la base de datos."));
        }

        nuevoUsuario.getRoles().add(rolAsignado);
        nuevoUsuario.setPassword(passwordEncoder.encode(dto.getPassword()));

        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        UsuarioDTO respuesta = new UsuarioDTO();
        respuesta.setId(guardado.getId());
        respuesta.setNombre(guardado.getNombre());
        respuesta.setEmail(guardado.getEmail());
        respuesta.setExcels(guardado.getExcels());
        return respuesta;
    }
  @Auditable(
    accion = "BORRAR_USUARIO", 
    tabla = "usuario", 
    entidad = Usuario.class,
    descripcion = "Se eliminó al usuario '#{#antiguo.nombre}' (Email: #{#antiguo.email})"
)
    public void eliminarUsuario(Integer id) {
        if (!usuarioRepository.existsById(id)) {
            throw new RuntimeException("Error: No se puede eliminar. El usuario con ID " + id + " no existe.");
        }

        usuarioRepository.deleteById(id);
    }
    @Auditable(
        accion = "CAMBIAR_CONTRASENA", 
        tabla = "usuario", 
        entidad = Usuario.class,
        descripcion = "Se actualizó la contraseña del usuario con ID: #{#id}"
    )
    public void cambiarContrasena(Integer id, String passwordVieja, String passwordNueva) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        if (!passwordEncoder.matches(passwordVieja, usuario.getPassword())) {
            throw new RuntimeException("Error: La contrasena actual es incorrecta.");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void solicitarRecuperacionPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email.trim());
        if (usuarioOpt.isEmpty()) {
            return;
        }

        Usuario usuario = usuarioOpt.get();
        String tokenPlano = generarResetToken();
        usuario.setPasswordResetTokenHash(hashToken(tokenPlano));
        usuario.setPasswordResetExpiresAt(LocalDateTime.now().plusHours(1));
        usuarioRepository.save(usuario);

        try {
            enviarCorreoRecuperacion(usuario, tokenPlano);
        } catch (RuntimeException e) {
            limpiarDatosRecuperacion(usuario);
            usuarioRepository.save(usuario);
            throw e;
        }
    }

    @Transactional
    public void resetearPassword(String tokenPlano, String passwordNueva) {
        if (tokenPlano == null || tokenPlano.trim().isEmpty()) {
            throw new RuntimeException("El token de recuperacion es obligatorio.");
        }

        if (passwordNueva == null || passwordNueva.trim().isEmpty()) {
            throw new RuntimeException("La nueva contrasena es obligatoria.");
        }

        String tokenHash = hashToken(tokenPlano.trim());
        Usuario usuario = usuarioRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new RuntimeException("El token es invalido o ya no esta disponible."));

        if (usuario.getPasswordResetExpiresAt() == null
                || usuario.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            limpiarDatosRecuperacion(usuario);
            usuarioRepository.save(usuario);
            throw new RuntimeException("El token ha expirado. Solicita uno nuevo.");
        }

        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        limpiarDatosRecuperacion(usuario);
        usuarioRepository.save(usuario);
    }
    @Auditable(
        accion = "CAMBIAR_FOTO", 
        tabla = "usuario", 
        entidad = Usuario.class,
        descripcion = "Se actualizó la foto de perfil del usuario con ID: #{#id}"
    )
    public void cambiarFoto(Integer id, MultipartFile archivo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        if (archivo == null || archivo.isEmpty()) {
            return;
        }

        try {
            Path directorio = Paths.get(CARPETA_FOTOS);
            if (!Files.exists(directorio)) {
                Files.createDirectories(directorio);
            }

            String nombreOriginal = archivo.getOriginalFilename();
            String nombreFinal = System.currentTimeMillis() + nombreOriginal;

            Path rutaArchivo = directorio.resolve(nombreFinal);
            Files.copy(archivo.getInputStream(), rutaArchivo, StandardCopyOption.REPLACE_EXISTING);

            usuario.setFoto(nombreFinal);
            usuarioRepository.save(usuario);
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar la foto: " + e.getMessage());
        }
    }
    @Auditable(
        accion = "CAMBIAR_ROL", 
        tabla = "usuario", 
        entidad = Usuario.class,
        descripcion = "Se cambió el rol del usuario con ID #{#idUsuario} al identificador de rol '#{#rolNuevo}'"
    )
    public void cambiarRol(Integer idUsuario, String rolNuevo) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Error: Usuario no encontrado."));

        Integer idRol;
        try {
            idRol = Integer.parseInt(rolNuevo);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Error: El rol enviado no es un numero valido.");
        }

        Rol nuevoRolObj = rolRepository.findById(idRol)
                .orElseThrow(() -> new RuntimeException("Error: El rol especificado no existe en el sistema."));

        usuario.getRoles().clear();
        usuario.getRoles().add(nuevoRolObj);
        usuarioRepository.save(usuario);
    }
   @Auditable(
        accion = "ACTUALIZAR_USUARIO", 
        tabla = "usuario", 
        entidad = Usuario.class,
        descripcion = "Se actualizó el perfil del usuario '#{#resultado.nombre}' (ID: #{#id})"
    )
    public UsuarioDTO actualizarUsuario(Integer id, UsuarioDTO dto) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            usuario.setNombre(dto.getNombre());
        }

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(usuario.getEmail())) {
            if (usuarioRepository.findByEmail(dto.getEmail()).isPresent()) {
                throw new RuntimeException("El email ya esta en uso");
            }
            usuario.setEmail(dto.getEmail());
        }

        if (dto.getFoto() != null && !dto.getFoto().trim().isEmpty()) {
            usuario.setFoto(dto.getFoto());
        }

        Usuario actualizado = usuarioRepository.save(usuario);

        UsuarioDTO response = new UsuarioDTO();
        response.setId(actualizado.getId());
        response.setNombre(actualizado.getNombre());
        response.setEmail(actualizado.getEmail());
        response.setFoto(actualizado.getFoto());
        return response;
    }

    private String generarResetToken() {
        byte[] bytes = new byte[PASSWORD_RESET_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String tokenPlano) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(tokenPlano.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No se pudo generar el hash del token.", e);
        }
    }

    private void limpiarDatosRecuperacion(Usuario usuario) {
        usuario.setPasswordResetTokenHash(null);
        usuario.setPasswordResetExpiresAt(null);
    }

    private void enviarCorreoRecuperacion(Usuario usuario, String tokenPlano) {
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            throw new RuntimeException("Falta configurar el SMTP en las variables de entorno.");
        }

        String separador = resetPasswordUrl.contains("?") ? "&" : "?";
        String enlaceReset = resetPasswordUrl + separador + "token="
                + URLEncoder.encode(tokenPlano, StandardCharsets.UTF_8);

        String senderEmail = (senderEmailConfigurado != null && !senderEmailConfigurado.isBlank())
                ? senderEmailConfigurado
                : smtpUsername;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setTo(usuario.getEmail());
            helper.setSubject("Recuperacion de contrasena");
            helper.setFrom(senderEmail, senderNameConfigurado);
            helper.setText(construirHtmlRecuperacion(usuario.getNombre(), enlaceReset), true);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("No se pudo enviar el correo de recuperacion. " + obtenerMensajeRaiz(e), e);
        }
    }

    private String construirHtmlRecuperacion(String nombre, String enlaceReset) {
        String saludo = (nombre != null && !nombre.isBlank()) ? nombre : "usuario";
        return "<html><body style=\"font-family:Arial,sans-serif;color:#1f2937;\">"
                + "<h2>Recuperacion de contrasena</h2>"
                + "<p>Hola " + saludo + ",</p>"
                + "<p>Hemos recibido una solicitud para restablecer tu contrasena.</p>"
                + "<p><a href=\"" + enlaceReset + "\" "
                + "style=\"display:inline-block;padding:12px 20px;background:#111827;color:#ffffff;text-decoration:none;border-radius:6px;\">"
                + "Restablecer contrasena</a></p>"
                + "<p>Si el boton no funciona, copia y pega este enlace en tu navegador:</p>"
                + "<p>" + enlaceReset + "</p>"
                + "<p>Este enlace caduca en 1 hora.</p>"
                + "</body></html>";
    }

    private String obtenerMensajeRaiz(Throwable error) {
        Throwable actual = error;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage() != null ? actual.getMessage() : error.getMessage();
    }
}
