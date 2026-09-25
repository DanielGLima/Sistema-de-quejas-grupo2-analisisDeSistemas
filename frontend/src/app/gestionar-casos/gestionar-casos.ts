import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { NotificacionesService } from '../services/notificaciones.service';
import { RegistroAuditoriaNotificacion } from '../models/notificacion.model';
import { AuditoriaService } from '../services/auditoria.service';
import { RegistroAuditoria } from '../models/auditoria.model';

export interface RespuestaOficial {
  titulo: string;
  cuerpo: string;
  compensacion?: string;
  fechaEmision: string;
  usuarioEmisor: string;
}

export interface HistorialReasignacion {
  fecha: string;
  responsableAnterior: string;
  responsableNuevo: string;
  motivo: string;
  usuarioEjecutor: string;
}

export interface CasoAdmin {
  id: string;
  tipoCaso: string;
  categoria: string;
  sucursal: string;
  motivo: string;
  fechaCreacion: string;
  estado: string;
  detalle: string;
  correoCliente?: string; // Requerido para envío de notificación al cliente (CU-14)
  empleadoAsignado?: string;
  notasInternas?: string;
  motivoReapertura?: string;
  respuesta?: RespuestaOficial;
  historialReasignaciones?: HistorialReasignacion[];
}

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

  casoSeleccionado: CasoAdmin | null = null;
  mostrarModalGestion: boolean = false;
  mostrarModalResponder: boolean = false;
  mostrarModalReasignar: boolean = false;
  mostrarModalNotificaciones: boolean = false; // CU-14: Modal para auditar envíos
  mostrarModalAuditoria: boolean = false;       // CU-15: Modal Bitácora
  registroAuditoriaSeleccionado: RegistroAuditoria | null = null; // CU-15: Detalle JSON

  mensajeExito: string = '';
  mensajeError: string = '';
  mensajeExitoRespuesta: string = '';
  mensajeErrorRespuesta: string = '';
  mensajeExitoReasignar: string = '';
  mensajeErrorReasignar: string = '';

  // CU-14: Toast reactivo de notificación automática generada
  notificacionAlerta: RegistroAuditoriaNotificacion | null = null;

  // Catálogos
  empleados: string[] = ['Carlos Morales', 'Ana Sofía Ruiz', 'Marcos Estrada', 'Lucía Méndez'];
  empleadosReasignables: string[] = [];
  estadosPosibles: string[] = ['Nuevo', 'En espera', 'En Proceso', 'Resuelto', 'Cerrado'];
  estadosPermitidos: string[] = [];

  // Filtros CU-12
  filtroTexto: string = '';
  filtroSucursal: string = 'Todas';
  filtroTipo: string = 'Todos';
  filtroEstado: string = 'Todos';
  filtroResponsable: string = 'Todos';
  filtroFechaDesde: string = '';
  filtroFechaHasta: string = '';

  sucursalesDisponibles: string[] = ['Todas', 'Zona 10', 'Miraflores', 'Cayalá'];
  tiposDisponibles: string[] = ['Todos', 'Queja', 'Reclamo', 'Felicitación', 'Sugerencia', 'Denuncia'];
  estadosFiltroDisponibles: string[] = ['Todos', 'Nuevo', 'En espera', 'En Proceso', 'Resuelto', 'Cerrado', 'Reapertura solicitada'];
  responsablesFiltroDisponibles: string[] = ['Todos', 'Sin asignar', 'Carlos Morales', 'Ana Sofía Ruiz', 'Marcos Estrada', 'Lucía Méndez'];

  casos: CasoAdmin[] = [
    {
      id: 'QUE-2026-001',
      tipoCaso: 'Queja',
      categoria: 'Servicio al cliente',
      sucursal: 'Zona 10',
      motivo: 'Demora en entrega de alimentos',
      fechaCreacion: '2026-09-18',
      estado: 'Nuevo',
      detalle: 'El pedido tardó más de 45 minutos en servirse.',
      correoCliente: 'cliente1@gmail.com',
      empleadoAsignado: ''
    },
    {
      id: 'REC-2026-002',
      tipoCaso: 'Reclamo',
      categoria: 'Facturación/Cobro',
      sucursal: 'Miraflores',
      motivo: 'Cobro no reconocido',
      fechaCreacion: '2026-09-19',
      estado: 'En Proceso',
      detalle: 'Se visualiza doble cobro en tarjeta de crédito.',
      correoCliente: 'cliente2@hotmail.com',
      empleadoAsignado: 'Carlos Morales',
      notasInternas: 'Solicitado comprobante al banco emisor.'
    },
    {
      id: 'QUE-2026-003',
      tipoCaso: 'Queja',
      categoria: 'Comida',
      sucursal: 'Cayalá',
      motivo: 'Platillo frío y orden incorrecta',
      fechaCreacion: '2026-09-15',
      estado: 'Reapertura solicitada',
      detalle: 'La queja se había cerrado pero el cliente no recibió su cupón de compensación.',
      motivoReapertura: 'No se me hizo efectiva la cortesía prometida en la sucursal.',
      correoCliente: 'cliente3@yahoo.com',
      empleadoAsignado: 'Ana Sofía Ruiz'
    }
  ];

  casosFiltrados: CasoAdmin[] = [];

  constructor(
    private fb: FormBuilder,
    public notifService: NotificacionesService,
    public auditoriaService: AuditoriaService
  ) {}

  ngOnInit(): void {
    this.gestionForm = this.fb.group({
      empleadoAsignado: [''],
      nuevoEstado: ['', Validators.required],
      notasInternas: ['', [Validators.maxLength(500)]]
    });

    this.respuestaForm = this.fb.group({
      titulo: ['', [Validators.required, Validators.maxLength(100)]],
      cuerpo: ['', [Validators.required, Validators.minLength(20), Validators.maxLength(1000)]],
      compensacion: ['', [Validators.maxLength(300)]]
    });

    this.reasignarForm = this.fb.group({
      nuevoResponsable: ['', Validators.required],
      motivoReasignacion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(300)]]
    });

    this.aplicarFiltros();
  }

  // --- CU-12: Filtros ---
  aplicarFiltros(): void {
    const texto = this.filtroTexto.trim().toLowerCase();

    this.casosFiltrados = this.casos.filter(caso => {
      const coincideTexto = !texto ||
        caso.id.toLowerCase().includes(texto) ||
        caso.motivo.toLowerCase().includes(texto) ||
        caso.detalle.toLowerCase().includes(texto);

      const coincideSucursal = this.filtroSucursal === 'Todas' || caso.sucursal === this.filtroSucursal;
      const coincideTipo = this.filtroTipo === 'Todos' || caso.tipoCaso === this.filtroTipo;
      const coincideEstado = this.filtroEstado === 'Todos' || caso.estado.toLowerCase() === this.filtroEstado.toLowerCase();

      let coincideResponsable = true;
      if (this.filtroResponsable === 'Sin asignar') {
        coincideResponsable = !caso.empleadoAsignado || caso.empleadoAsignado.trim() === '';
      } else if (this.filtroResponsable !== 'Todos') {
        coincideResponsable = caso.empleadoAsignado === this.filtroResponsable;
      }

      const coincideDesde = !this.filtroFechaDesde || caso.fechaCreacion >= this.filtroFechaDesde;
      const coincideHasta = !this.filtroFechaHasta || caso.fechaCreacion <= this.filtroFechaHasta;

      return coincideTexto && coincideSucursal && coincideTipo && coincideEstado && coincideResponsable && coincideDesde && coincideHasta;
    });
  }

  limpiarFiltros(): void {
    this.filtroTexto = '';
    this.filtroSucursal = 'Todas';
    this.filtroTipo = 'Todos';
    this.filtroEstado = 'Todos';
    this.filtroResponsable = 'Todos';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.aplicarFiltros();
  }

  // --- CU-10: Gestión de Estados + CU-14 Notificación + CU-15 Auditoría ---
  calcularEstadosPermitidos(caso: CasoAdmin): string[] {
    if (caso.estado === 'Reapertura solicitada') {
      return ['En Proceso', 'Cerrado'];
    }

    switch (caso.estado) {
      case 'Nuevo':
        return ['En espera', 'En Proceso', 'Cerrado'];
      case 'En espera':
        return ['En Proceso', 'Cerrado'];
      case 'En Proceso':
        return ['En espera', 'Resuelto', 'Cerrado'];
      case 'Resuelto':
        return ['Cerrado', 'En Proceso'];
      case 'Cerrado':
        return ['Reapertura solicitada'];
      default:
        return this.estadosPosibles;
    }
  }

  abrirModalGestion(caso: CasoAdmin): void {
    this.casoSeleccionado = caso;
    this.mensajeExito = '';
    this.mensajeError = '';
    this.estadosPermitidos = this.calcularEstadosPermitidos(caso);

    this.gestionForm.reset({
      empleadoAsignado: caso.empleadoAsignado || '',
      nuevoEstado: caso.estado === 'Reapertura solicitada' ? '' : caso.estado,
      notasInternas: caso.notasInternas || ''
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
    if (!this.casoSeleccionado || this.gestionForm.invalid) {
      this.mensajeError = 'Debe seleccionar un estado válido.';
      return;
    }

    const { empleadoAsignado, nuevoEstado, notasInternas } = this.gestionForm.value;

    if (!this.estadosPermitidos.includes(nuevoEstado)) {
      this.mensajeError = 'El estado no es permitido según el flujo del caso.';
      return;
    }

    const estadoPrevio = this.casoSeleccionado.estado;
    this.casoSeleccionado.estado = nuevoEstado;
    this.casoSeleccionado.empleadoAsignado = empleadoAsignado || undefined;
    this.casoSeleccionado.notasInternas = notasInternas?.trim() || undefined;

    const idx = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
    if (idx !== -1) {
      this.casos[idx] = { ...this.casoSeleccionado };
    }

    // DISPARO AUTOMÁTICO CU-14: Notificar al cliente si cambió el estado
    if (estadoPrevio !== nuevoEstado) {
      this.notificacionAlerta = this.notifService.enviarNotificacionAutomatica({
        codigoCaso: this.casoSeleccionado.id,
        evento: 'CAMBIO_ESTADO',
        emailDestino: this.casoSeleccionado.correoCliente,
        destinatarioRol: 'Cliente',
        detalles: { estadoNuevo: nuevoEstado }
      });
      setTimeout(() => this.notificacionAlerta = null, 4000);
    }

    // DISPARO AUTOMÁTICO CU-15: Registrar en Bitácora de Auditoría
    this.auditoriaService.registrarEvento({
      usuarioId: 'ADM-01 (Heydi Herrera)',
      rolUsuario: 'Administrador General',
      tipoAccion: 'CAMBIO_ESTADO',
      moduloAfectado: 'GESTION_CASOS',
      valoresAnteriores: { caso: this.casoSeleccionado.id, estado: estadoPrevio },
      valoresNuevos: { caso: this.casoSeleccionado.id, estado: nuevoEstado, empleado: empleadoAsignado, notas: notasInternas }
    });

    this.aplicarFiltros();
    this.mensajeExito = 'Gestión guardada exitosamente.';
    setTimeout(() => this.cerrarModal(), 1400);
  }

  // --- CU-11: Responder Caso + CU-14 Notificación + CU-15 Auditoría ---
  puedeResponder(caso: CasoAdmin): boolean {
    return caso.estado?.toLowerCase() === 'en proceso' && !!caso.empleadoAsignado && caso.empleadoAsignado.trim() !== '';
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
    if (!this.casoSeleccionado || this.respuestaForm.invalid) {
      this.mensajeErrorRespuesta = 'Complete los campos obligatorios (cuerpo mín. 20 caracteres).';
      return;
    }

    const { titulo, cuerpo, compensacion } = this.respuestaForm.value;

    this.casoSeleccionado.respuesta = {
      titulo: titulo.trim(),
      cuerpo: cuerpo.trim(),
      compensacion: compensacion?.trim() || undefined,
      fechaEmision: new Date().toISOString(),
      usuarioEmisor: this.casoSeleccionado.empleadoAsignado || 'Administrador'
    };

    this.casoSeleccionado.estado = 'Resuelto';

    const idx = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
    if (idx !== -1) {
      this.casos[idx] = { ...this.casoSeleccionado };
    }

    // DISPARO AUTOMÁTICO CU-14: Notificar al cliente la respuesta oficial
    this.notificacionAlerta = this.notifService.enviarNotificacionAutomatica({
      codigoCaso: this.casoSeleccionado.id,
      evento: 'RESPUESTA_OFICIAL',
      emailDestino: this.casoSeleccionado.correoCliente,
      destinatarioRol: 'Cliente',
      detalles: { tituloRespuesta: titulo }
    });
    setTimeout(() => this.notificacionAlerta = null, 4000);

    // DISPARO AUTOMÁTICO CU-15: Registrar en Bitácora de Auditoría
    this.auditoriaService.registrarEvento({
      usuarioId: this.casoSeleccionado.empleadoAsignado || 'ADM-01',
      rolUsuario: 'Gerente / Encargado',
      tipoAccion: 'EMISION_RESPUESTA_OFICIAL',
      moduloAfectado: 'RESPUESTA_CASOS',
      valoresAnteriores: { caso: this.casoSeleccionado.id, estado: 'En Proceso' },
      valoresNuevos: { caso: this.casoSeleccionado.id, estado: 'Resuelto', titulo, compensacion }
    });

    this.aplicarFiltros();
    this.mensajeExitoRespuesta = 'Respuesta oficial enviada con éxito. Estado actualizado a Resuelto.';
    setTimeout(() => this.cerrarModalResponder(), 1400);
  }

  // --- CU-13: Reasignar Caso + CU-14 Notificación + CU-15 Auditoría ---
  puedeReasignar(caso: CasoAdmin): boolean {
    const tieneAsignado = !!caso.empleadoAsignado && caso.empleadoAsignado.trim() !== '';
    const estadoValido = !['cerrado', 'cancelado', 'resuelto'].includes(caso.estado?.toLowerCase());
    return tieneAsignado && estadoValido;
  }

  abrirModalReasignar(caso: CasoAdmin): void {
    this.casoSeleccionado = caso;
    this.mensajeExitoReasignar = '';
    this.mensajeErrorReasignar = '';

    this.empleadosReasignables = this.empleados.filter(e => e !== caso.empleadoAsignado);

    this.reasignarForm.reset({
      nuevoResponsable: '',
      motivoReasignacion: ''
    });

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

    if (this.reasignarForm.invalid) {
      const resp = this.reasignarForm.get('nuevoResponsable');
      const mot = this.reasignarForm.get('motivoReasignacion');

      if (!resp?.value) {
        this.mensajeErrorReasignar = 'Debe seleccionar un nuevo empleado responsable.';
        return;
      }
      if (!mot?.value || mot.value.trim().length < 10) {
        this.mensajeErrorReasignar = 'Debe ingresar el motivo de la reasignación (mínimo 10 caracteres).';
        return;
      }
      return;
    }

    const { nuevoResponsable, motivoReasignacion } = this.reasignarForm.value;
    const anteriorResponsable = this.casoSeleccionado.empleadoAsignado || 'Sin asignar';

    if (!this.casoSeleccionado.historialReasignaciones) {
      this.casoSeleccionado.historialReasignaciones = [];
    }

    this.casoSeleccionado.historialReasignaciones.push({
      fecha: new Date().toISOString(),
      responsableAnterior: anteriorResponsable,
      responsableNuevo: nuevoResponsable,
      motivo: motivoReasignacion.trim(),
      usuarioEjecutor: 'Gerente General'
    });

    this.casoSeleccionado.empleadoAsignado = nuevoResponsable;

    const idx = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
    if (idx !== -1) {
      this.casos[idx] = { ...this.casoSeleccionado };
    }

    // DISPARO AUTOMÁTICO CU-14: Notificar internamente al nuevo empleado asignado
    this.notificacionAlerta = this.notifService.enviarNotificacionAutomatica({
      codigoCaso: this.casoSeleccionado.id,
      evento: 'REASIGNACION',
      emailDestino: `${nuevoResponsable.toLowerCase().replace(' ', '.')}@lasdelicias.com.gt`,
      destinatarioRol: 'Personal Administrativo',
      detalles: { responsableNuevo: nuevoResponsable }
    });
    setTimeout(() => this.notificacionAlerta = null, 4000);

    // DISPARO AUTOMÁTICO CU-15: Registrar en Bitácora de Auditoría
    this.auditoriaService.registrarEvento({
      usuarioId: 'ADM-01 (Heydi Herrera)',
      rolUsuario: 'Administrador General',
      tipoAccion: 'REASIGNACION_RESPONSABLE',
      moduloAfectado: 'ASIGNACION_CASOS',
      valoresAnteriores: { caso: this.casoSeleccionado.id, responsable: anteriorResponsable },
      valoresNuevos: { caso: this.casoSeleccionado.id, responsable: nuevoResponsable, motivo: motivoReasignacion }
    });

    this.aplicarFiltros();
    this.mensajeExitoReasignar = `Caso reasignado a ${nuevoResponsable} exitosamente.`;
    setTimeout(() => {
      this.cerrarModalReasignar();
    }, 1400);
  }

  // --- CU-14: Modal Notificaciones ---
  abrirModalNotificaciones(): void {
    this.mostrarModalNotificaciones = true;
  }

  cerrarModalNotificaciones(): void {
    this.mostrarModalNotificaciones = false;
  }

  // --- CU-15: Modal Bitácora de Auditoría ---
  abrirModalAuditoria(): void {
    this.mostrarModalAuditoria = true;
  }

  cerrarModalAuditoria(): void {
    this.mostrarModalAuditoria = false;
    this.registroAuditoriaSeleccionado = null;
  }

  verDetalleAuditoria(reg: RegistroAuditoria): void {
    this.registroAuditoriaSeleccionado = reg;
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

  // CU-15: Parsea el JSON/objeto para mostrar pares Clave - Valor legibles en la vista
  obtenerPropiedadesAuditoria(data: any): { clave: string; valor: string }[] {
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