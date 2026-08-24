package com.sistemadequejas.service;

import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.CasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CasoService {

    @Autowired
    private CasoRepository casoRepository;

    public List<Caso> findAll() {
        return casoRepository.findAll();
    }

    public List<Caso> findByUsuario(Usuario usuario) {
        return casoRepository.findByUsuarioOrderByFechaCreacionDesc(usuario);
    }

    public Optional<Caso> findById(Integer id) {
        return casoRepository.findById(id);
    }

    public void deleteById(Integer id) {
        casoRepository.deleteById(id);
    }

    // CU-05: guarda el caso y arma su identificador visible (prefijo tipo +
    // prefijo categoria + id interno), ej. QUE-COM-123 o DEN-124.
    public Caso registrarCaso(Caso caso) {
        String marcador = "TMP" + (System.nanoTime() % 100000000L);
        caso.setIdentificadorVisible(marcador);
        Caso guardado = casoRepository.save(caso);

        StringBuilder identificador = new StringBuilder(guardado.getTipoCaso().getCodigo());
        if (guardado.getCategoriaCaso() != null) {
            identificador.append("-").append(guardado.getCategoriaCaso().getCodigo());
        }
        identificador.append("-").append(guardado.getIdCaso());

        guardado.setIdentificadorVisible(identificador.toString());
        return casoRepository.save(guardado);
    }
}
