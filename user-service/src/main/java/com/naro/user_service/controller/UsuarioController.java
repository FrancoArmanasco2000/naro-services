package com.naro.user_service.controller;

import com.naro.user_service.dto.ActualizarParcialUsuarioRequest;
import com.naro.user_service.dto.ActualizarUsuarioRequest;
import com.naro.user_service.dto.CrearUsuarioRequest;
import com.naro.user_service.dto.UsuarioResponse;
import com.naro.user_service.entity.Usuario;
import com.naro.user_service.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Value("${application.security.internal-secret}")
    private String internalSecret;

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> obtenerPorId(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") String userId
    ) {
        verificarPropietario(id, userId);
        return ResponseEntity.ok(toResponse(usuarioService.obtenerPorId(id)));
    }

    // Solo lo puede llamar auth-service (server-to-server) al registrar, no
    // un usuario final -> no hay X-User-Id que chequear, sino un secreto
    // compartido entre los dos servicios.
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(
        @RequestHeader("X-Internal-Secret") String secret,
        @Valid @RequestBody CrearUsuarioRequest request
    ) {
        verificarLlamadaInterna(secret);
        return ResponseEntity.ok(toResponse(usuarioService.crear(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarParcial(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody ActualizarParcialUsuarioRequest request
    ) {
        verificarPropietario(id, userId);
        return ResponseEntity.ok(toResponse(usuarioService.actualizarParcial(id, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizarCompleto(
        @PathVariable Long id,
        @RequestHeader("X-User-Id") String userId,
        @Valid @RequestBody ActualizarUsuarioRequest request
    ) {
        verificarPropietario(id, userId);
        return ResponseEntity.ok(toResponse(usuarioService.actualizarCompleto(id, request)));
    }

    private void verificarPropietario(Long id, String userId) {
        if (!id.toString().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tenés permiso para acceder a este perfil");
        }
    }

    private void verificarLlamadaInterna(String secret) {
        boolean valido = MessageDigest.isEqual(
            secret.getBytes(StandardCharsets.UTF_8),
            internalSecret.getBytes(StandardCharsets.UTF_8)
        );
        if (!valido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
            .id(usuario.getId())
            .nombre(usuario.getNombre())
            .apellido(usuario.getApellido())
            .dni(usuario.getDni())
            .fechaNacimiento(usuario.getFechaNacimiento())
            .genero(usuario.getGenero())
            .email(usuario.getEmail())
            .telefono(usuario.getTelefono())
            .ciudad(usuario.getCiudad())
            .provincia(usuario.getProvincia())
            .pais(usuario.getPais())
            .miembroDesde(usuario.getMiembroDesde())
            .build();
    }

}
