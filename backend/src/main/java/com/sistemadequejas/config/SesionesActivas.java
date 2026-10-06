package com.sistemadequejas.config;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Lleva el control de las sesiones abiertas por cada usuario cliente para poder cerrarlas
// todas (CU-02, postcondicion: "las sesiones activas previas quedan invalidadas").
@Component
public class SesionesActivas implements HttpSessionListener {

    private final Map<Integer, Set<HttpSession>> sesionesPorUsuario = new ConcurrentHashMap<>();

    public void registrar(Integer idUsuario, HttpSession session) {
        sesionesPorUsuario.computeIfAbsent(idUsuario, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void invalidarTodas(Integer idUsuario) {
        Set<HttpSession> sesiones = sesionesPorUsuario.remove(idUsuario);
        if (sesiones == null) {
            return;
        }
        for (HttpSession sesion : sesiones) {
            try {
                sesion.invalidate();
            } catch (IllegalStateException ignorada) {
                // la sesion ya habia expirado o se habia cerrado
            }
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent evento) {
        sesionesPorUsuario.values().forEach(sesiones -> sesiones.remove(evento.getSession()));
    }
}
