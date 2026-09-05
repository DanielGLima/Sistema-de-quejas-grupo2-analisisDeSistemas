package com.sistemadequejas.service;

import com.sistemadequejas.model.BitacoraAuditoria;
import com.sistemadequejas.model.Caso;
import com.sistemadequejas.model.Usuario;
import com.sistemadequejas.repository.BitacoraAuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// CU-15 (soporte minimo, solo escritura): registra acciones auditables
// disparadas desde otros casos de uso (CU-03, CU-07, ...).
@Service
public class BitacoraAuditoriaService {

    @Autowired
    private BitacoraAuditoriaRepository bitacoraAuditoriaRepository;

    public void registrarAccionUsuario(Usuario usuario, String accion, String moduloAfectado, String detalle) {
        registrar("Usuario", usuario, null, null, accion, moduloAfectado, detalle);
    }

    public void registrarAccionUsuarioSobreCaso(Usuario usuario, Caso caso, String accion, String moduloAfectado, String detalle) {
        registrar("Usuario", usuario, null, caso, accion, moduloAfectado, detalle);
    }

    private void registrar(String tipoActor, Usuario usuario, Integer idPersonal, Caso caso, String accion, String moduloAfectado, String detalle) {
        BitacoraAuditoria registro = new BitacoraAuditoria();
        registro.setTipoActor(tipoActor);
        registro.setUsuario(usuario);
        registro.setIdPersonal(idPersonal);
        registro.setCaso(caso);
        registro.setAccion(accion);
        registro.setModuloAfectado(moduloAfectado);
        registro.setDetalle(detalle);
        bitacoraAuditoriaRepository.save(registro);
    }
}
