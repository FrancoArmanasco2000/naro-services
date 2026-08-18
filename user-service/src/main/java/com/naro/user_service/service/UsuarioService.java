package com.naro.user_service.service;

import com.naro.user_service.dto.ActualizarParcialUsuarioRequest;
import com.naro.user_service.dto.ActualizarUsuarioRequest;
import com.naro.user_service.dto.CrearUsuarioRequest;
import com.naro.user_service.entity.Usuario;

public interface UsuarioService {

    Usuario obtenerPorId(Long id);

    Usuario crear(CrearUsuarioRequest request);

    Usuario actualizarParcial(Long id, ActualizarParcialUsuarioRequest request);

    Usuario actualizarCompleto(Long id, ActualizarUsuarioRequest request);

}
