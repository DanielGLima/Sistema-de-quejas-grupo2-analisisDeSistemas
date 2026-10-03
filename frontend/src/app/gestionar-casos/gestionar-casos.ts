import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AdminService } from '../services/admin.service';
import { CatalogoService } from '../services/catalogo.service';
import { BitacoraApi, CasoAdminApi, NotificacionApi, Personal, RespuestaApi } from '../core/models/admin';
import { CategoriaCaso, Sucursal, TipoCaso } from '../core/models/catalogos';

export interface HistorialReasignacion {
  fecha: string;
  responsableAnterior: string;
  responsableNuevo: string;
  motivo: string;
  usuarioEjecutor: string;
}

// Forma que usa esta pantalla (aplanada a partir de la respuesta de la API).
export interface CasoAdmin {
  idCaso: number;
  id: string;
  tipoCaso: string;
  categoria: string;
  sucursal: string;
  motivo: string;
  fechaCreacion: string;
  fechaActualizacion: string;
  estado: string;
  detalle: string;
  esAnonimo: boolean;
  nombreCliente?: string;
  correoCliente?: string;
  idPersonalAsignado?: number;
  empleadoAsignado?: string;
  motivoReapertura?: string;
  estadosPermitidos: string[];
  respuestas: RespuestaApi[];
  historialReasignaciones: HistorialReasignacion[];
}

interface Propiedad {
  clave: string;
  valor: string;
}

const PENDIENTE_APROBACION = 'Pendiente de aprobación';

@Component({
  selector: 'app-gestionar-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './gestionar-casos.html',
  styleUrl: './gestionar-casos.scss'
})
export class GestionarCasosComponent implements OnInit {
  // Formularios
  gestionForm!: FormGroup;
  respuestaForm!: FormGroup;
  reasignarForm!: FormGroup;

  // Personal que inició sesión
  personal: Personal | null = null;

  casoSeleccionado: CasoAdmin | null = null;
  mostrarModalGestion = false;
  mostrarModalResponder = false;
  mostrarModalReasignar = false;
  mostrarModalNotificaciones = false;
  mostrarModalAuditoria = false;
  registroAuditoriaSeleccionado: BitacoraApi | null = null;

  mensajeExito = '';
  mensajeError = '';
  mensajeExitoRespuesta = '';
  mensajeErrorRespuesta = '';
  mensajeExitoReasignar = '';
  mensajeErrorReasignar = '';
  mensajeErrorBusqueda = '';
  mensajeAccion = '';
  errorAccion = '';

  // CU-14: aviso flotante de la última notificación generada
  notificacionAlerta: NotificacionApi | null = null;
  notificaciones: NotificacionApi[] = [];
  bitacora: BitacoraApi[] = [];

  // Catálogos
  empleados: Personal[] = [];
  empleadosReasignables: Personal[] = [];
  sucursalesDisponibles: Sucursal[] = [];
  tiposDisponibles: TipoCaso[] = [];
  categoriasDisponibles: CategoriaCaso[] = [];
  estadosDisponibles: { idEstado: number; nombre: string }[] = [];
  estadosPermitidos: string[] = [];

  // Filtros CU-12 (se ejecutan con el botón "Buscar")
  filtroTexto = '';
  filtroIdentificador = '';
  filtroCorreo = '';
  filtroSucursal: number | null = null;
  filtroTipo: number | null = null;
  filtroCategoria: number | null = null;
  filtroEstado: number | null = null;
  filtroResponsable: number | null = null;
  filtroFechaDesde = '';
  filtroFechaHasta = '';
  ordenarPor = 'fecha';
  direccion: 'asc' | 'desc' = 'desc';

  casosFiltrados: CasoAdmin[] = [];
  buscando = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private adminService: AdminService,
    private catalogoService: CatalogoService
  ) {}

  ngOnInit(): void {
    this.gestionForm = this.fb.group({
      empleadoAsignado: [null as number | null],
      nuevoEstado: ['', Validators.required],
      notasInternas: ['', [Validators.maxLength(500)]]
    });

    this.respuestaForm = this.fb.group({
      titulo: ['', [Validators.required, Validators.maxLength(100)]],
      cuerpo: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]],
      compensacion: ['', [Validators.maxLength(300)]]
    });

    this.reasignarForm = this.fb.group({
      nuevoResponsable: [null as number | null, Validators.required],
      motivoReasignacion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(300)]]
    });

    this.adminService.sesion().subscribe({
      next: (personal) => {
        this.personal = personal;
        this.cargarCatalogos();
        this.buscar();
        this.refrescarNotificaciones(false);
        this.refrescarBitacora();
      },
      error: () => this.router.navigate(['/login'])
    });
  }

  // --- Rol ---
  get rol(): string {
    return this.personal?.rol.nombre ?? '';
  }

  esAdmin(): boolean {
    return this.rol === 'Administrador General';
  }

  esAdminOGerente(): boolean {
    return this.rol === 'Administrador General' || this.rol === 'Gerente';
  }

  private cargarCatalogos(): void {
    this.catalogoService.sucursales().subscribe(s => this.sucursalesDisponibles = s);
    this.catalogoService.tiposCaso().subscribe(t => this.tiposDisponibles = t);
    this.catalogoService.categoriasCaso().subscribe(c => this.categoriasDisponibles = c);
    this.catalogoService.estados().subscribe(e => this.estadosDisponibles = e);
    this.adminService.empleadosAsignables().subscribe(e => this.empleados = e);
  }

  // --- CU-12: búsqueda en el servidor ---
  buscar(alTerminar?: () => void): void {
    this.mensajeErrorBusqueda = '';
    this.buscando = true;

    this.adminService.buscarCasos({
      texto: this.filtroTexto.trim(),
      identificador: this.filtroIdentificador.trim(),
      correoCliente: this.filtroCorreo.trim(),
      idSucursal: this.filtroSucursal,
      idTipoCaso: this.filtroTipo,
      idCategoria: this.filtroCategoria,
      idEstado: this.filtroEstado,
      idResponsable: this.filtroResponsable,
      fechaInicial: this.filtroFechaDesde,
      fechaFinal: this.filtroFechaHasta,
      ordenarPor: this.ordenarPor,
      direccion: this.direccion
    }).subscribe({
      next: (casos) => {
        this.casosFiltrados = casos.map(c => this.mapear(c));
        this.buscando = false;
        alTerminar?.();
      },
      error: (err) => {
        // FA02: criterio inválido
        this.mensajeErrorBusqueda = err.error?.message ?? 'No se pudo realizar la búsqueda';
        this.casosFiltrados = [];
        this.buscando = false;
      }
    });
  }

  limpiarFiltros(): void {
    this.filtroTexto = '';
    this.filtroIdentificador = '';
    this.filtroCorreo = '';
    this.filtroSucursal = null;
    this.filtroTipo = null;
    this.filtroCategoria = null;
    this.filtroEstado = null;
    this.filtroResponsable = null;
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.ordenarPor = 'fecha';
    this.direccion = 'desc';
    this.buscar();
  }

  private mapear(c: CasoAdminApi): CasoAdmin {
    return {
      idCaso: c.idCaso,
      id: c.identificadorVisible,
      tipoCaso: c.tipoCaso,
      categoria: c.categoria ?? 'No aplica',
      sucursal: c.sucursal,
      motivo: c.categoria ?? c.tipoCaso,
      fechaCreacion: c.fechaCreacion,
      fechaActualizacion: c.fechaActualizacion,
      estado: c.estado,
      detalle: c.descripcion,
      esAnonimo: c.esAnonimo,
      nombreCliente: c.nombreCliente,
      correoCliente: c.correoCliente,
      idPersonalAsignado: c.idPersonalAsignado,
      empleadoAsignado: c.personalAsignado,
      motivoReapertura: c.motivoReapertura,
      estadosPermitidos: c.estadosPermitidos,
      respuestas: c.respuestas ?? [],
      historialReasignaciones: (c.reasignaciones ?? []).map(r => ({
        fecha: r.fechaReasignacion,
        responsableAnterior: r.personalAnterior?.nombreCompleto ?? 'Sin asignar',
        responsableNuevo: r.personalNuevo.nombreCompleto,
        motivo: r.motivo,
        usuarioEjecutor: r.personalEjecutor.nombreCompleto
      }))
    };
  }

  // Reemplaza el caso en la lista y en la selección con los datos más recientes del servidor.
  private aplicarActualizacion(api: CasoAdminApi): CasoAdmin {
    const actualizado = this.mapear(api);
    const idx = this.casosFiltrados.findIndex(c => c.idCaso === actualizado.idCaso);
    if (idx !== -1) {
      this.casosFiltrados[idx] = actualizado;
    }
    this.casoSeleccionado = actualizado;
    return actualizado;
  }

  // FA03: si otra persona modificó el caso, se recarga la lista y la selección queda al día.
  private recargarSiConflicto(err: any): void {
    if (err.status !== 409) return;
    const idSeleccionado = this.casoSeleccionado?.idCaso;
    this.buscar(() => {
      const fresco = this.casosFiltrados.find(c => c.idCaso === idSeleccionado);
      if (fresco) {
        this.casoSeleccionado = fresco;
        this.estadosPermitidos = [fresco.estado, ...fresco.estadosPermitidos];
      }
    });
  }

  // --- CU-10: Gestión de Estados ---
  abrirModalGestion(caso: CasoAdmin): void {
    this.casoSeleccionado = caso;
    this.mensajeExito = '';
    this.mensajeError = '';
    this.estadosPermitidos = [caso.estado, ...caso.estadosPermitidos];

    this.gestionForm.reset({
      empleadoAsignado: caso.idPersonalAsignado ?? null,
      nuevoEstado: caso.estado === 'Reapertura solicitada' ? '' : caso.estado,
      notasInternas: ''
    });

    this.mostrarModalGestion = true;
  }

  cerrarModal(): void {
    this.mostrarModalGestion = false;
    this.casoSeleccionado = null;
    this.mensajeExito = '';
    this.mensajeError = '';
  }

  guardarGestion(): void {
    this.mensajeError = '';
    if (!this.casoSeleccionado || this.gestionForm.invalid) {
      this.mensajeError = 'Debe seleccionar un estado válido.';
      return;
    }

    const { empleadoAsignado, nuevoEstado, notasInternas } = this.gestionForm.value;

    this.adminService.gestionar(this.casoSeleccionado.idCaso, {
      idPersonalAsignado: empleadoAsignado,
      nuevoEstado,
      notas: notasInternas?.trim() || undefined,
      fechaActualizacion: this.casoSeleccionado.fechaActualizacion
    }).subscribe({
      next: (api) => {
        this.aplicarActualizacion(api);
        this.mensajeExito = 'Gestión guardada exitosamente.';
        this.alTerminarAccion();
        setTimeout(() => this.cerrarModal(), 1400);
      },
      error: (err) => {
        // FA01: estado no permitido (con la lista de estados válidos) / FA02: sin permisos / FA03: caso modificado
        this.mensajeError = err.error?.message ?? 'No se pudo guardar la gestión';
        this.recargarSiConflicto(err);
      }
    });
  }

  // --- CU-11: Responder Caso ---
  tieneRespuestaPendiente(caso: CasoAdmin): boolean {
    return caso.respuestas.some(r => r.estadoAprobacion === PENDIENTE_APROBACION);
  }

  puedeResponder(caso: CasoAdmin): boolean {
    if (caso.estado.toLowerCase() !== 'en proceso' || !caso.idPersonalAsignado || this.tieneRespuestaPendiente(caso)) {
      return false;
    }
    return this.rol !== 'Operador' || caso.idPersonalAsignado === this.personal?.idPersonal;
  }

  // FA01: el Gerente confirma la respuesta de un Operador.
  puedeAprobar(caso: CasoAdmin): boolean {
    return this.esAdminOGerente() && this.tieneRespuestaPendiente(caso) && caso.estado.toLowerCase() === 'en proceso';
  }

  aprobarRespuesta(caso: CasoAdmin): void {
    const pendiente = caso.respuestas.find(r => r.estadoAprobacion === PENDIENTE_APROBACION);
    if (!pendiente) return;
    this.limpiarAviso();

    this.adminService.aprobarRespuesta(pendiente.idRespuesta).subscribe({
      next: (api) => {
        this.aplicarActualizacion(api);
        this.mensajeAccion = `Respuesta aprobada. El caso ${caso.id} quedó en estado Resuelto.`;
        this.alTerminarAccion();
      },
      error: (err) => this.errorAccion = err.error?.message ?? 'No se pudo aprobar la respuesta'
    });
  }

  abrirModalResponder(caso: CasoAdmin): void {
    this.casoSeleccionado = caso;
    this.mensajeExitoRespuesta = '';
    this.mensajeErrorRespuesta = '';
    this.respuestaForm.reset({ titulo: '', cuerpo: '', compensacion: '' });
    this.mostrarModalResponder = true;
  }

  cerrarModalResponder(): void {
    this.mostrarModalResponder = false;
    this.mensajeExitoRespuesta = '';
    this.mensajeErrorRespuesta = '';
    this.respuestaForm.reset();
  }

  enviarRespuesta(): void {
    this.mensajeErrorRespuesta = '';
    if (!this.casoSeleccionado) return;
    if (this.respuestaForm.invalid) {
      this.respuestaForm.markAllAsTouched();
      this.mensajeErrorRespuesta = 'Debe completar la información requerida: título (máx. 100) y cuerpo de la respuesta (entre 20 y 1000 caracteres).';
      return;
    }

    const { titulo, cuerpo, compensacion } = this.respuestaForm.value;

    this.adminService.responder(this.casoSeleccionado.idCaso, {
      titulo: titulo.trim(),
      cuerpo: cuerpo.trim(),
      accionesSeguimiento: compensacion?.trim() || undefined
    }).subscribe({
      next: (api) => {
        const actualizado = this.aplicarActualizacion(api);
        this.mensajeExitoRespuesta = actualizado.estado === 'Resuelto'
          ? 'Respuesta oficial enviada con éxito. Estado actualizado a Resuelto.'
          : 'Respuesta registrada. Queda pendiente de aprobación del Gerente.';
        this.alTerminarAccion();
        setTimeout(() => this.cerrarModalResponder(), 1600);
      },
      error: (err) => {
        // FA02: información insuficiente / FA03: caso no disponible para respuesta
        this.mensajeErrorRespuesta = err.error?.message ?? 'No se pudo enviar la respuesta';
        this.recargarSiConflicto(err);
      }
    });
  }

  // --- CU-10: Reasignar Caso ---
  puedeReasignar(caso: CasoAdmin): boolean {
    const estadoValido = !['cerrado', 'cancelado por el usuario', 'resuelto'].includes(caso.estado.toLowerCase());
    return this.esAdminOGerente() && !!caso.idPersonalAsignado && estadoValido;
  }

  abrirModalReasignar(caso: CasoAdmin): void {
    this.casoSeleccionado = caso;
    this.mensajeExitoReasignar = '';
    this.mensajeErrorReasignar = '';
    this.empleadosReasignables = this.empleados.filter(e => e.idPersonal !== caso.idPersonalAsignado);
    this.reasignarForm.reset({ nuevoResponsable: null, motivoReasignacion: '' });
    this.mostrarModalReasignar = true;
  }

  cerrarModalReasignar(): void {
    this.mostrarModalReasignar = false;
    this.mensajeExitoReasignar = '';
    this.mensajeErrorReasignar = '';
    this.reasignarForm.reset();
  }

  guardarReasignacion(): void {
    this.mensajeErrorReasignar = '';
    this.mensajeExitoReasignar = '';
    if (!this.casoSeleccionado) return;

    const resp = this.reasignarForm.get('nuevoResponsable')?.value;
    const motivo = (this.reasignarForm.get('motivoReasignacion')?.value ?? '').trim();
    if (!resp) {
      this.mensajeErrorReasignar = 'Debe seleccionar un nuevo empleado responsable.';
      return;
    }
    if (motivo.length < 10) {
      this.mensajeErrorReasignar = 'Debe ingresar el motivo de la reasignación (mínimo 10 caracteres).';
      return;
    }

    this.adminService.reasignar(this.casoSeleccionado.idCaso, {
      idPersonalNuevo: Number(resp),
      motivo,
      fechaActualizacion: this.casoSeleccionado.fechaActualizacion
    }).subscribe({
      next: (api) => {
        const actualizado = this.aplicarActualizacion(api);
        this.mensajeExitoReasignar = `Caso reasignado a ${actualizado.empleadoAsignado} exitosamente.`;
        this.alTerminarAccion();
        setTimeout(() => this.cerrarModalReasignar(), 1400);
      },
      error: (err) => {
        this.mensajeErrorReasignar = err.error?.message ?? 'No se pudo reasignar el caso';
        this.recargarSiConflicto(err);
      }
    });
  }

  // --- CU-14 / CU-15 ---
  private alTerminarAccion(): void {
    this.refrescarNotificaciones(true);
    this.refrescarBitacora();
  }

  private refrescarNotificaciones(mostrarAviso: boolean): void {
    if (!this.esAdminOGerenteRol()) return;
    this.adminService.notificaciones().subscribe({
      next: (lista) => {
        this.notificaciones = lista;
        const ultima = lista[0];
        if (mostrarAviso && ultima && Date.now() - new Date(ultima.fechaEnvio).getTime() < 15000) {
          this.notificacionAlerta = ultima;
          setTimeout(() => this.notificacionAlerta = null, 4000);
        }
      },
      error: () => this.notificaciones = []
    });
  }

  private refrescarBitacora(): void {
    if (!this.esAdminRol()) return;
    this.adminService.bitacora().subscribe({
      next: (lista) => this.bitacora = lista,
      error: () => this.bitacora = []
    });
  }

  // El rol puede no estar cargado aún al primer refresco; se consulta el guardado como respaldo.
  private esAdminRol(): boolean {
    return (this.rol || this.adminService.rolGuardado()) === 'Administrador General';
  }

  private esAdminOGerenteRol(): boolean {
    const rol = this.rol || this.adminService.rolGuardado();
    return rol === 'Administrador General' || rol === 'Gerente';
  }

  abrirModalNotificaciones(): void {
    this.refrescarNotificaciones(false);
    this.mostrarModalNotificaciones = true;
  }

  cerrarModalNotificaciones(): void {
    this.mostrarModalNotificaciones = false;
  }

  abrirModalAuditoria(): void {
    this.refrescarBitacora();
    this.mostrarModalAuditoria = true;
  }

  cerrarModalAuditoria(): void {
    this.mostrarModalAuditoria = false;
    this.registroAuditoriaSeleccionado = null;
  }

  verDetalleAuditoria(reg: BitacoraApi): void {
    this.registroAuditoriaSeleccionado = reg;
  }

  limpiarAviso(): void {
    this.mensajeAccion = '';
    this.errorAccion = '';
  }

  obtenerClaseEstado(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'nuevo': return 'badge-nuevo';
      case 'en espera': return 'badge-espera';
      case 'en proceso': return 'badge-proceso';
      case 'resuelto': return 'badge-resuelto';
      case 'cerrado': return 'badge-cerrado';
      case 'reapertura solicitada': return 'badge-reapertura';
      default: return 'badge-default';
    }
  }

  // CU-15: el detalle viene como JSON; si trae {anterior, nuevo} se muestran en dos columnas.
  detalleAuditoria(reg: BitacoraApi): { anterior: Propiedad[] | null; nuevo: Propiedad[] } {
    let obj: any = null;
    try {
      obj = reg.detalle ? JSON.parse(reg.detalle) : null;
    } catch {
      obj = null;
    }
    if (obj && typeof obj === 'object' && obj.anterior && obj.nuevo) {
      return { anterior: this.obtenerPropiedadesAuditoria(obj.anterior), nuevo: this.obtenerPropiedadesAuditoria(obj.nuevo) };
    }
    return { anterior: null, nuevo: this.obtenerPropiedadesAuditoria(obj ?? reg.detalle) };
  }

  obtenerPropiedadesAuditoria(data: any): Propiedad[] {
    if (!data) return [];
    try {
      const obj = typeof data === 'string' ? JSON.parse(data) : data;
      return Object.entries(obj).map(([clave, valor]) => ({
        clave: clave.charAt(0).toUpperCase() + clave.slice(1),
        valor: typeof valor === 'object' ? JSON.stringify(valor) : String(valor ?? '—')
      }));
    } catch {
      return [{ clave: 'Detalle', valor: String(data) }];
    }
  }
}
