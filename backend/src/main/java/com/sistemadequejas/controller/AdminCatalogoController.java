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

    private static final String MODULO = "Administración de catálogos";

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
        auditar(actor, "Alta de sucursal", "{}", BitacoraAuditoriaService.json("nombre", guardada.getNombreSucursal(), "dirección", guardada.getDireccionSucursal()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/sucursales/{id}")
    public Sucursal actualizarSucursal(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                       @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        String anterior = catalogoAdminService.sucursales().stream().filter(x -> x.getIdSucursal().equals(id)).findFirst()
                .map(x -> BitacoraAuditoriaService.json("nombre", x.getNombreSucursal(), "dirección", x.getDireccionSucursal(), "situación", estadoCuenta(x.isActivo())))
                .orElse("{}");
        Sucursal guardada = catalogoAdminService.guardarSucursal(id, request, confirmar);
        auditar(actor, "Modificación de sucursal", anterior, BitacoraAuditoriaService.json("nombre", guardada.getNombreSucursal(),
                "dirección", guardada.getDireccionSucursal(), "situación", estadoCuenta(guardada.isActivo())));
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
        auditar(actor, "Alta de tipo de caso", "{}", BitacoraAuditoriaService.json("nombre", guardado.getNombre(), "prefijo", guardado.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @PutMapping("/tipos-caso/{id}")
    public TipoCaso actualizarTipoCaso(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                       @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        String anterior = catalogoAdminService.tiposCaso().stream().filter(x -> x.getIdTipoCaso().equals(id)).findFirst()
                .map(x -> BitacoraAuditoriaService.json("nombre", x.getNombre(), "prefijo", x.getCodigo(), "situación", estadoCuenta(Boolean.TRUE.equals(x.getActivo()))))
                .orElse("{}");
        TipoCaso guardado = catalogoAdminService.guardarTipoCaso(id, request, confirmar);
        auditar(actor, "Modificación de tipo de caso", anterior, BitacoraAuditoriaService.json("nombre", guardado.getNombre(),
                "prefijo", guardado.getCodigo(), "situación", estadoCuenta(Boolean.TRUE.equals(guardado.getActivo()))));
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
        auditar(actor, "Alta de categoría", "{}", BitacoraAuditoriaService.json("nombre", guardada.getNombre(), "prefijo", guardada.getCodigo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaCaso actualizarCategoria(@PathVariable Integer id, @RequestBody CatalogoRequest request,
                                             @RequestParam(defaultValue = "false") boolean confirmar, HttpSession session) {
        Personal actor = admin(session);
        String anterior = catalogoAdminService.categorias().stream().filter(x -> x.getIdCategoria().equals(id)).findFirst()
                .map(x -> BitacoraAuditoriaService.json("nombre", x.getNombre(), "prefijo", x.getCodigo(), "situación", estadoCuenta(Boolean.TRUE.equals(x.getActivo()))))
                .orElse("{}");
        CategoriaCaso guardada = catalogoAdminService.guardarCategoria(id, request, confirmar);
        auditar(actor, "Modificación de categoría", anterior, BitacoraAuditoriaService.json("nombre", guardada.getNombre(),
                "prefijo", guardada.getCodigo(), "situación", estadoCuenta(Boolean.TRUE.equals(guardada.getActivo()))));
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
        String anterior = catalogoAdminService.estados().stream().filter(x -> x.getIdEstado().equals(id)).findFirst()
                .map(x -> BitacoraAuditoriaService.json("nombre", x.getNombre(), "situación", estadoCuenta(Boolean.TRUE.equals(x.getActivo()))))
                .orElse("{}");
        EstadoCaso guardado = catalogoAdminService.guardarEstado(id, request, confirmar);
        auditar(actor, "Modificación de estado", anterior, BitacoraAuditoriaService.json("nombre", guardado.getNombre(),
                "situación", estadoCuenta(Boolean.TRUE.equals(guardado.getActivo()))));
        return guardado;
    }

    private Personal admin(HttpSession session) {
        return sesionPersonal.requerirRol(session, PersonalService.ROL_ADMINISTRADOR);
    }

    private void auditar(Personal actor, String accion, String anterior, String nuevo) {
        bitacoraAuditoriaService.registrarAccionPersonal(actor, null, accion, MODULO, BitacoraAuditoriaService.cambio(anterior, nuevo));
    }

    private String estadoCuenta(boolean activo) {
        return activo ? "Activo" : "Inactivo";
    }
}
