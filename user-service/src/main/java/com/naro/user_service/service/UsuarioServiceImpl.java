package com.naro.user_service.service;

import com.naro.user_service.dto.ActualizarParcialUsuarioRequest;
import com.naro.user_service.dto.ActualizarUsuarioRequest;
import com.naro.user_service.dto.CrearUsuarioRequest;
import com.naro.user_service.entity.Usuario;
import com.naro.user_service.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    @Override
    @Transactional
    public Usuario crear(CrearUsuarioRequest request) {
        if (usuarioRepository.existsById(request.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya tiene un perfil creado");
        }

        Usuario usuario = Usuario.builder()
            .id(request.getId())
            .nombre(request.getNombre())
            .apellido(request.getApellido())
            .dni(request.getDni())
            .fechaNacimiento(request.getFechaNacimiento())
            .genero(request.getGenero())
            .email(request.getEmail())
            .telefono(request.getTelefono())
            .ciudad(request.getCiudad())
            .provincia(request.getProvincia())
            .pais(request.getPais())
            .miembroDesde(LocalDate.now())
            .build();

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizarParcial(Long id, ActualizarParcialUsuarioRequest request) {
        Usuario usuario = obtenerPorId(id);

        if (request.getNombre() != null) usuario.setNombre(request.getNombre());
        if (request.getApellido() != null) usuario.setApellido(request.getApellido());
        if (request.getDni() != null) usuario.setDni(request.getDni());
        if (request.getFechaNacimiento() != null) usuario.setFechaNacimiento(request.getFechaNacimiento());
        if (request.getGenero() != null) usuario.setGenero(request.getGenero());
        if (request.getEmail() != null) usuario.setEmail(request.getEmail());
        if (request.getTelefono() != null) usuario.setTelefono(request.getTelefono());
        if (request.getCiudad() != null) usuario.setCiudad(request.getCiudad());
        if (request.getProvincia() != null) usuario.setProvincia(request.getProvincia());
        if (request.getPais() != null) usuario.setPais(request.getPais());
        // miembroDesde: nunca se toca

        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario actualizarCompleto(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = obtenerPorId(id);

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());
        usuario.setDni(request.getDni());
        usuario.setFechaNacimiento(request.getFechaNacimiento());
        usuario.setGenero(request.getGenero());
        usuario.setEmail(request.getEmail());
        usuario.setTelefono(request.getTelefono());
        usuario.setCiudad(request.getCiudad());
        usuario.setProvincia(request.getProvincia());
        usuario.setPais(request.getPais());
        // miembroDesde: nunca se toca, tampoco en el reemplazo completo

        return usuarioRepository.save(usuario);
    }

}
