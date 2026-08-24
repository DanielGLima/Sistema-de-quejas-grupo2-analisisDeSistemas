package com.sistemadequejas.service;

import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> findById(Integer id) {
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> buscarPorCorreo(String correo) {
        return usuarioRepository.findByCorreo(correo);
    }

    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }

    // CU-01: crea la cuenta cifrando la contrasena antes de guardar.
    public Usuario registrar(Usuario usuario, String passwordPlano) {
        if (usuarioRepository.existsByCorreo(usuario.getCorreo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo electronico ya se encuentra registrado");
        }
        usuario.setContrasenaHash(passwordEncoder.encode(passwordPlano));
        return usuarioRepository.save(usuario);
    }

    // CU-00: valida credenciales sin revelar cual campo fallo (FA05).
    public Usuario autenticar(String correo, String passwordPlano) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electronico o contrasena incorrectos"));

        if (!passwordEncoder.matches(passwordPlano, usuario.getContrasenaHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo electronico o contrasena incorrectos");
        }
        return usuario;
    }

    // CU-03: actualiza datos y, opcionalmente, la contrasena (exige la actual).
    public Usuario actualizarPerfil(Usuario usuario, String passwordActualPlano, String passwordNuevaPlano) {
        if (passwordNuevaPlano != null && !passwordNuevaPlano.isBlank()) {
            if (passwordActualPlano == null || !passwordEncoder.matches(passwordActualPlano, usuario.getContrasenaHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrasena actual incorrecta");
            }
            usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        }
        return usuarioRepository.save(usuario);
    }

    // CU-02: aplica la nueva contrasena tras validar el codigo de recuperacion.
    public void restablecerPassword(Usuario usuario, String passwordNuevaPlano) {
        usuario.setContrasenaHash(passwordEncoder.encode(passwordNuevaPlano));
        usuarioRepository.save(usuario);
    }
}
