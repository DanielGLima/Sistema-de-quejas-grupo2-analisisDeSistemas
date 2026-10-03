package com.sistemadequejas.controller;

import com.sistemadequejas.config.SesionPersonal;
import com.sistemadequejas.dto.CatalogoRequest;
import com.sistemadequejas.model.*;
import com.sistemadequejas.service.BitacoraAuditoriaService;
import com.sistemadequejas.service.CatalogoAdminService;
import com.sistemadequejas.service.PersonalService;
import com.sistemadequejas.repository.RolRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// CU-13: administracion de catalogos. Solo el Administrador General (FA04).
@RestController
@RequestMapping("/api/admin/catalogos")
public class AdminCatalogoController {

    private static final String MODULO = "CU-13";

    @Autowired
    private CatalogoAdminService catalogoAdminService;

    @Autowired
    private SesionPersonal sesionPersonal;

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Autowired
    private RolRepository rolRepository;

    @GetMapping("/roles")
    public List<Rol> roles(HttpSession session) {
        admin(session);
        return rolRepository.findAll();
    }

    // ---- Sucursales ----
    @GetMapping("/sucursales")
    public List<Sucursal> sucursales(HttpSession session) {
        admin(session);
        return catalogoAdminService.sucursales();
    }

    @PostMapping("/sucursales")
    public ResponseEntity<Sucursal> crearSucursal(@RequestBody CatalogoRequest request, HttpSession session) {
        Personal actor = admin(session);
        Sucursal guardada = catalogoAdminService.guardarSucursal(null, request, false);
        auditar(actor, "Alta de sucursal", BitacoraAuditoriaService.json("nombre", guardada.getNombreSucursal()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/sucursales/{id}")
    public Sucursal actualizarSucursal(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                       @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        Sucursal guardada = catalogoAdminService.guardarSucursal(id, request, confirmar);
        auditar(actor, "Modificacion de sucursal", BitacoraAuditoriaService.json("nombre", guardada.getNombreSucursal(),
                "activo", String.valueOf(guardada.isActivo())));
        return guardada;
    }

    // ---- Tipos de caso ----
    @GetMapping("/tipos-caso")
    public List<TipoCaso> tiposCaso(HttpSession session) {
        admin(session);
        return catalogoAdminService.tiposCaso();
    }

    @PostMapping("/tipos-caso")
    public ResponseEntity<TipoCaso> crearTipoCaso(@RequestBody CatalogoRequest request, HttpSession session) {
        Personal actor = admin(session);
        TipoCaso guardado = catalogoAdminService.guardarTipoCaso(null, request, false);
        auditar(actor, "Alta de tipo de caso", BitacoraAuditoriaService.json("nombre", guardado.getNombre(), "prefijo", guardado.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/tipos-caso/{id}")
    public TipoCaso actualizarTipoCaso(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                       @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        TipoCaso guardado = catalogoAdminService.guardarTipoCaso(id, request, confirmar);
        auditar(actor, "Modificacion de tipo de caso", BitacoraAuditoriaService.json("nombre", guardado.getNombre(),
                "activo", String.valueOf(guardado.getActivo())));
        return guardado;
    }

    // ---- Categorias ----
    @GetMapping("/categorias")
    public List<CategoriaCaso> categorias(HttpSession session) {
        admin(session);
        return catalogoAdminService.categorias();
    }

    @PostMapping("/categorias")
    public ResponseEntity<CategoriaCaso> crearCategoria(@RequestBody CatalogoRequest request, HttpSession session) {
        Personal actor = admin(session);
        CategoriaCaso guardada = catalogoAdminService.guardarCategoria(null, request, false);
        auditar(actor, "Alta de categoria", BitacoraAuditoriaService.json("nombre", guardada.getNombre(), "prefijo", guardada.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaCaso actualizarCategoria(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                             @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        CategoriaCaso guardada = catalogoAdminService.guardarCategoria(id, request, confirmar);
        auditar(actor, "Modificacion de categoria", BitacoraAuditoriaService.json("nombre", guardada.getNombre(),
                "activo", String.valueOf(guardada.getActivo())));
        return guardada;
    }

    // ---- Estados ----
    @GetMapping("/estados")
    public List<EstadoCaso> estados(HttpSession session) {
        admin(session);
        return catalogoAdminService.estados();
    }

    @PutMapping("/estados/{id}")
    public EstadoCaso actualizarEstado(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                       @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        EstadoCaso guardado = catalogoAdminService.guardarEstado(id, request, confirmar);
        auditar(actor, "Modificacion de estado", BitacoraAuditoriaService.json("nombre", guardado.getNombre(),
                "activo", String.valueOf(guardado.getActivo())));
        return guardado;
    }

    private Personal admin(HttpSession session) {
        return sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);
    }

    private void auditar(Personal actor, String accion, String detalle) {
        bitacoraAuditoriaService.registrarAccionPersonal(actor, null, accion, MODULO, detalle);
    }
}
