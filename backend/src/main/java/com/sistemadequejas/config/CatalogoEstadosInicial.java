package com.sistemadequejas.config;

import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.repository.EstadoCasoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Deja el catalogo de estados de los casos con los nombres exactos que usa el flujo del sistema
// (CU-07): "Nuevo", "En espera", "En Proceso", "Resuelto", "Cerrado", "Cancelado por el usuario"
// y "Reapertura solicitada".
//
// Una base creada con versiones anteriores puede tener variantes ("En proceso", "Pendiente",
// "Cancelado") o carecer de alguno de los estados. Al arrancar:
//  - se renombra la variante conservando su id (los casos mantienen su estado);
//  - se crean los estados que falten;
//  - se dejan activos los estados del flujo.
// Es idempotente: si el catalogo ya es correcto no cambia nada.
@Component
@Order(1)
public class CatalogoEstadosInicial implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogoEstadosInicial.class);

    // Nombre oficial -> orden dentro del flujo.
    private static final Map<String, Integer> ESTADOS = new LinkedHashMap<>();

    static {
        ESTADOS.put("Nuevo", 1);
        ESTADOS.put("En espera", 2);
        ESTADOS.put("En Proceso", 3);
        ESTADOS.put("Resuelto", 4);
        ESTADOS.put("Cerrado", 5);
        ESTADOS.put("Cancelado por el usuario", 6);
        ESTADOS.put("Reapertura solicitada", 7);
    }

    // Nombres antiguos (en minusculas) que equivalen a un estado oficial.
    private static final Map<String, String> ALIAS = Map.of(
            "pendiente", "Nuevo",
            "cancelado", "Cancelado por el usuario");

    @Autowired
    private EstadoCasoRepository estadoCasoRepository;

    @Override
    public void run(String... args) {
        try {
            normalizar();
        } catch (Exception e) {
            // Nunca impide el arranque: si falla, el detalle queda en el log.
            log.error("No se pudo normalizar el catalogo de estados de casos: {}", e.getMessage());
        }
    }

    private void normalizar() {
        List<EstadoCaso> existentes = estadoCasoRepository.findAll();

        for (Map.Entry<String, Integer> oficial : ESTADOS.entrySet()) {
            String nombre = oficial.getKey();

            // 1) Ya existe con el nombre exacto: solo se asegura que este activo.
            EstadoCaso exacto = existentes.stream().filter(e -> nombre.equals(e.getNombre())).findFirst().orElse(null);
            if (exacto != null) {
                activar(exacto);
                continue;
            }

            // 2) Existe una variante (otra capitalizacion o nombre antiguo): se renombra.
            EstadoCaso variante = existentes.stream()
                    .filter(e -> equivale(e.getNombre(), nombre))
                    .findFirst()
                    .orElse(null);
            if (variante != null) {
                String anterior = variante.getNombre();
                variante.setNombre(nombre);
                variante.setOrden(oficial.getValue());
                variante.setActivo(true);
                estadoCasoRepository.save(variante);
                log.info("Catalogo de estados: \"{}\" se renombro a \"{}\".", anterior, nombre);
                continue;
            }

            // 3) No existe: se crea.
            EstadoCaso nuevo = new EstadoCaso();
            nuevo.setNombre(nombre);
            nuevo.setOrden(oficial.getValue());
            nuevo.setActivo(true);
            estadoCasoRepository.save(nuevo);
            log.info("Catalogo de estados: se creo el estado \"{}\".", nombre);
        }
    }

    private static boolean equivale(String existente, String oficial) {
        if (existente == null) {
            return false;
        }
        String limpio = existente.trim().toLowerCase(Locale.ROOT);
        return limpio.equals(oficial.toLowerCase(Locale.ROOT)) || oficial.equals(ALIAS.get(limpio));
    }

    private void activar(EstadoCaso estado) {
        if (!Boolean.TRUE.equals(estado.getActivo())) {
            estado.setActivo(true);
            estadoCasoRepository.save(estado);
        }
    }
}
