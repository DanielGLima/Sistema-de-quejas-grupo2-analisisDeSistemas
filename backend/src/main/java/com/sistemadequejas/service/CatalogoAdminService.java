package com.sistemadequejas.service;

import com.sistemadequejas.dto.CatalogoRequest;
import com.sistemadequejas.model.CategoriaCaso;
import com.sistemadequejas.model.EstadoCaso;
import com.sistemadequejas.model.Sucursal;
import com.sistemadequejas.model.TipoCaso;
import com.sistemadequejas.repository.CasoRepository;
import com.sistemadequejas.repository.CategoriaCasoRepository;
import com.sistemadequejas.repository.EstadoCasoRepository;
import com.sistemadequejas.repository.SucursalRepository;
import com.sistemadequejas.repository.TipoCasoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

// CU-13: administracion de catalogos (sucursales, tipos de caso, categorias, estados).
// Se prioriza la desactivacion logica; nunca se borra fisicamente.
@Service
public class CatalogoAdminService {

    private static final Pattern PATRON_ALFANUMERICO = Pattern.compile("^[A-Za-z0-9ÁÉÍÓÚÜÑáéíóúüñ .,\\-]+$");
    private static final Pattern PATRON_LETRAS = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ /]+$");
    private static final Pattern PATRON_PREFIJO = Pattern.compile("^[A-Za-z]{3}$");
    private static final Set<String> ESTADOS_MAESTROS = Set.of("Nuevo", "En espera", "En Proceso", "Resuelto", "Cerrado",
            "Cancelado por el usuario", "Reapertura solicitada");

    @Autowired
    private SucursalRepository sucursalRepository;

    @Autowired
    private TipoCasoRepository tipoCasoRepository;

    @Autowired
    private CategoriaCasoRepository categoriaCasoRepository;

    @Autowired
    private EstadoCasoRepository estadoCasoRepository;

    @Autowired
    private CasoRepository casoRepository;

    public List<Sucursal> sucursales() {
        return sucursalRepository.findAll();
    }

    public List<TipoCaso> tiposCaso() {
        return tipoCasoRepository.findAll();
    }

    public List<CategoriaCaso> categorias() {
        return categoriaCasoRepository.findAll();
    }

    public List<EstadoCaso> estados() {
        return estadoCasoRepository.findAll();
    }

    // ---------------------------- Sucursal ----------------------------
    public Sucursal guardarSucursal(Integer id, CatalogoRequest request, boolean confirmar) {
        String nombre = texto(request.nombre());
        String direccion = texto(request.direccion());
        String telefono = texto(request.telefono());

        if (nombre.isEmpty() || nombre.length() > 30 || !PATRON_ALFANUMERICO.matcher(nombre).matches()) {
            throw datosInvalidos("Nombre de la sucursal: obligatorio, alfanumerico, maximo 30 caracteres");
        }
        if (direccion.isEmpty() || direccion.length() > 150) {
            throw datosInvalidos("Direccion de la sucursal: obligatoria, maximo 150 caracteres");
        }
        if (telefono.length() > 20) {
            throw datosInvalidos("Telefono de la sucursal: maximo 20 caracteres");
        }
        boolean repetido = sucursalRepository.findAll().stream()
                .anyMatch(s -> s.getNombreSucursal().equalsIgnoreCase(nombre) && !s.getIdSucursal().equals(id));
        if (repetido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sucursal con ese nombre");
        }

        Sucursal sucursal = id == null ? new Sucursal() : sucursalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sucursal no encontrada"));

        boolean desactivando = id != null && sucursal.isActivo() && Boolean.FALSE.equals(request.activo());
        if (desactivando) {
            advertirCasosActivos(casoRepository.countBySucursalIdSucursalAndEstadoCasoNombreNotIn(id, CasoService.ESTADOS_FINALES), "la sucursal", confirmar);
        }

        sucursal.setNombreSucursal(nombre);
        sucursal.setDireccionSucursal(direccion);
        sucursal.setTelefonoSucursal(telefono.isEmpty() ? null : telefono);
        if (request.activo() != null) {
            sucursal.setActivo(request.activo());
        }
        return sucursalRepository.save(sucursal);
    }

    // ---------------------------- Tipo de caso ----------------------------
    public TipoCaso guardarTipoCaso(Integer id, CatalogoRequest request, boolean confirmar) {
        String nombre = texto(request.nombre());
        String codigo = texto(request.codigo()).toUpperCase();

        if (nombre.isEmpty() || nombre.length() > 20 || !PATRON_ALFANUMERICO.matcher(nombre).matches()) {
            throw datosInvalidos("Nombre del tipo de caso: obligatorio, alfanumerico, maximo 20 caracteres");
        }
        if (!PATRON_PREFIJO.matcher(codigo).matches()) {
            throw datosInvalidos("Prefijo del tipo de caso: exactamente 3 letras");
        }
        boolean repetido = tipoCasoRepository.findAll().stream()
                .anyMatch(t -> (t.getNombre().equalsIgnoreCase(nombre) || t.getCodigo().equals(codigo)) && !t.getIdTipoCaso().equals(id));
        if (repetido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un tipo de caso con ese nombre o prefijo");
        }

        TipoCaso tipo = id == null ? new TipoCaso() : tipoCasoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de caso no encontrado"));

        boolean desactivando = id != null && Boolean.TRUE.equals(tipo.getActivo()) && Boolean.FALSE.equals(request.activo());
        if (desactivando) {
            advertirCasosActivos(casoRepository.countByTipoCasoIdTipoCasoAndEstadoCasoNombreNotIn(id, CasoService.ESTADOS_FINALES), "el tipo de caso", confirmar);
        }

        tipo.setNombre(nombre);
        tipo.setCodigo(codigo);
        if (request.activo() != null) {
            tipo.setActivo(request.activo());
        }
        return tipoCasoRepository.save(tipo);
    }

    // ---------------------------- Categoria ----------------------------
    public CategoriaCaso guardarCategoria(Integer id, CatalogoRequest request, boolean confirmar) {
        String nombre = texto(request.nombre());
        String codigo = texto(request.codigo()).toUpperCase();

        if (nombre.isEmpty() || nombre.length() > 30 || !PATRON_LETRAS.matcher(nombre).matches()) {
            throw datosInvalidos("Nombre de la categoria: obligatorio, solo letras, maximo 30 caracteres");
        }
        if (!PATRON_PREFIJO.matcher(codigo).matches()) {
            throw datosInvalidos("Prefijo de la categoria: exactamente 3 letras");
        }
        boolean repetido = categoriaCasoRepository.findAll().stream()
                .anyMatch(c -> (c.getNombre().equalsIgnoreCase(nombre) || c.getCodigo().equals(codigo)) && !c.getIdCategoria().equals(id));
        if (repetido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoria con ese nombre o prefijo");
        }

        CategoriaCaso categoria = id == null ? new CategoriaCaso() : categoriaCasoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria no encontrada"));

        boolean desactivando = id != null && Boolean.TRUE.equals(categoria.getActivo()) && Boolean.FALSE.equals(request.activo());
        if (desactivando) {
            advertirCasosActivos(casoRepository.countByCategoriaCasoIdCategoriaAndEstadoCasoNombreNotIn(id, CasoService.ESTADOS_FINALES), "la categoria", confirmar);
        }

        categoria.setNombre(nombre);
        categoria.setCodigo(codigo);
        if (request.activo() != null) {
            categoria.setActivo(request.activo());
        }
        return categoriaCasoRepository.save(categoria);
    }

    // ---------------------------- Estado ----------------------------
    // Catalogo maestro: el nombre debe ser uno de los estados del proceso y no se puede
    // desactivar un estado que el flujo de CU-10 necesita.
    public EstadoCaso guardarEstado(Integer id, CatalogoRequest request, boolean confirmar) {
        EstadoCaso estado = estadoCasoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estado no encontrado"));

        String nombre = texto(request.nombre());
        if (nombre.isEmpty() || nombre.length() > 25 || !ESTADOS_MAESTROS.contains(nombre)) {
            throw datosInvalidos("Nombre del estado: debe ser uno del catalogo maestro (" + String.join(", ", ESTADOS_MAESTROS) + ")");
        }
        if (!estado.getNombre().equals(nombre)) {
            throw datosInvalidos("El nombre de un estado del flujo no se puede cambiar");
        }

        boolean desactivando = Boolean.TRUE.equals(estado.getActivo()) && Boolean.FALSE.equals(request.activo());
        if (desactivando) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El estado \"" + estado.getNombre() + "\" forma parte del flujo de atencion y no se puede desactivar");
        }
        if (request.activo() != null) {
            estado.setActivo(request.activo());
        }
        return estadoCasoRepository.save(estado);
    }

    // FA01: advierte del impacto; el administrador confirma reenviando confirmar=true.
    private void advertirCasosActivos(long casosActivos, String elemento, boolean confirmar) {
        if (casosActivos > 0 && !confirmar) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Advertencia: existen " + casosActivos + " caso(s) activo(s) asociados a " + elemento
                            + ". Confirme la operacion para desactivarlo de todas formas");
        }
    }

    private ResponseStatusException datosInvalidos(String detalle) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos invalidos. Corrija: " + detalle);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
