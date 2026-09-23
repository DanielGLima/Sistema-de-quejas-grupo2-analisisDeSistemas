import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';

export interface EvaluacionCaso {
  calificacion: number;
  comentarios?: string;
  fechaEvaluacion: string;
}

export interface SolicitudReapertura {
  motivo: string;
  archivoEvidencia?: string;
  fechaSolicitud: string;
}

export interface Caso {
  id: string;
  tipoCaso: string;
  sucursal: string;
  motivo: string;
  fechaIncidente: string;
  fechaCreacion: string;
  fechaCierre?: string;
  estado: string;
  detalle: string;
  respuesta?: string;
  archivoEvidencia?: string;
  motivoCancelacion?: string;
  evaluacion?: EvaluacionCaso;
  reapertura?: SolicitudReapertura;
}

@Component({
  selector: 'app-consultar-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './consultar-casos.component.html',
  styleUrls: ['./consultar-casos.component.scss']
})
export class ConsultarCasosComponent implements OnInit {
  filtroEstado: string = 'Todos';
  filtroTipoCaso: string = 'Todos';
  filtroFechaDesde: string = '';
  filtroFechaHasta: string = '';

  estadosDisponibles: string[] = ['Todos', 'Pendiente', 'En proceso', 'Resuelto', 'Cerrado', 'Reapertura solicitada', 'Rechazado', 'Cancelado'];
  tiposCaso: string[] = ['Todos', 'Queja', 'Reclamo', 'Felicitación', 'Denuncia', 'Sugerencia'];
  casos: Caso[] = [];
  casosFiltrados: Caso[] = [];

  casoSeleccionado: Caso | null = null;
  mostrarModalDetalle: boolean = false;

  // Variables CU-07
  mostrarModalCancelar: boolean = false;
  motivoCancelacion: string = '';
  errorMotivoCancelacion: string = '';

  // Variables CU-08
  mostrarModalEvaluar: boolean = false;
  evaluacionForm!: FormGroup;
  mensajeErrorEvaluar: string = '';
  mensajeExitoEvaluar: string = '';

  // Variables CU-09
  mostrarModalReapertura: boolean = false;
  reaperturaForm!: FormGroup;
  archivoReaperturaNombre: string = '';
  mensajeErrorReapertura: string = '';
  mensajeExitoReapertura: string = '';

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.evaluacionForm = this.fb.group({
      calificacion: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      comentarios: ['', [Validators.maxLength(500)]]
    });

    this.reaperturaForm = this.fb.group({
      motivo: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]]
    });

    this.casos = [
      {
        id: 'CASO-001',
        tipoCaso: 'Queja',
        sucursal: 'Zona 10',
        motivo: 'Demora en entrega de pedido',
        fechaIncidente: '2026-09-10',
        fechaCreacion: '2026-09-11',
        fechaCierre: '2026-09-18',
        estado: 'Cerrado',
        detalle: 'El pedido tardó más de 50 minutos y los alimentos llegaron fríos.',
        respuesta: 'Se verificó con cocina y se aplicó descuento correctivo.'
      },
      {
        id: 'CASO-002',
        tipoCaso: 'Reclamo',
        sucursal: 'Miraflores',
        motivo: 'Cobro duplicado en factura',
        fechaIncidente: '2026-08-01',
        fechaCreacion: '2026-08-02',
        fechaCierre: '2026-08-10',
        estado: 'Cerrado',
        detalle: 'Se visualiza doble cargo en el estado de cuenta.',
        respuesta: 'Reembolso generado por el banco emisor.'
      },
      {
        id: 'CASO-003',
        tipoCaso: 'Sugerencia',
        sucursal: 'Cayalá',
        motivo: 'Ampliar opciones vegetarianas',
        fechaIncidente: '2026-09-20',
        fechaCreacion: '2026-09-21',
        estado: 'Pendiente',
        detalle: 'Sería excelente contar con más platillos a base de plantas.'
      }
    ];

    this.aplicarFiltros();
  }

  aplicarFiltros(): void {
    this.casosFiltrados = this.casos.filter(caso => {
      const cumpleEstado = this.filtroEstado === 'Todos' || caso.estado === this.filtroEstado;
      const cumpleTipo = this.filtroTipoCaso === 'Todos' || caso.tipoCaso === this.filtroTipoCaso;
      const cumpleDesde = !this.filtroFechaDesde || caso.fechaCreacion >= this.filtroFechaDesde;
      const cumpleHasta = !this.filtroFechaHasta || caso.fechaCreacion <= this.filtroFechaHasta;
      return cumpleEstado && cumpleTipo && cumpleDesde && cumpleHasta;
    });
  }

  limpiarFiltros(): void {
    this.filtroEstado = 'Todos';
    this.filtroTipoCaso = 'Todos';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.aplicarFiltros();
  }

  verDetalle(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.mostrarModalDetalle = true;
  }

  cerrarModal(): void {
    this.casoSeleccionado = null;
    this.mostrarModalDetalle = false;
  }

  puedeCancelar(caso: Caso): boolean {
    return caso.estado?.toLowerCase() === 'pendiente';
  }

  abrirModalCancelar(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.motivoCancelacion = '';
    this.errorMotivoCancelacion = '';
    this.mostrarModalCancelar = true;
  }

  cerrarModalCancelar(): void {
    this.mostrarModalCancelar = false;
    this.motivoCancelacion = '';
    this.errorMotivoCancelacion = '';
  }

  confirmarCancelacion(): void {
    const motivoLimpio = this.motivoCancelacion.trim();
    if (!motivoLimpio || motivoLimpio.length < 10) return;

    if (this.casoSeleccionado) {
      this.casoSeleccionado.estado = 'Cancelado';
      this.casoSeleccionado.motivoCancelacion = motivoLimpio;
      const index = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
      if (index !== -1) this.casos[index] = { ...this.casoSeleccionado };
      this.aplicarFiltros();
      this.cerrarModalCancelar();
    }
  }

  puedeEvaluar(caso: Caso): boolean {
    const estado = caso.estado?.toLowerCase();
    return (estado === 'resuelto' || estado === 'cerrado') && !caso.evaluacion;
  }

  abrirModalEvaluar(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.mensajeErrorEvaluar = '';
    this.mensajeExitoEvaluar = '';
    this.evaluacionForm.reset({ calificacion: 0, comentarios: '' });
    this.mostrarModalEvaluar = true;
  }

  cerrarModalEvaluar(): void {
    this.mostrarModalEvaluar = false;
    this.mensajeErrorEvaluar = '';
    this.mensajeExitoEvaluar = '';
  }

  seleccionarEstrella(valor: number): void {
    this.evaluacionForm.patchValue({ calificacion: valor });
  }

  enviarEvaluacion(): void {
    const calificacion = this.evaluacionForm.get('calificacion')?.value;
    const comentarios = this.evaluacionForm.get('comentarios')?.value || '';

    if (!calificacion || calificacion < 1 || calificacion > 5) {
      this.mensajeErrorEvaluar = 'Debe seleccionar una calificación para continuar';
      return;
    }

    if (this.casoSeleccionado) {
      this.casoSeleccionado.evaluacion = {
        calificacion,
        comentarios: comentarios.trim() || undefined,
        fechaEvaluacion: new Date().toISOString()
      };
      const index = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
      if (index !== -1) this.casos[index] = { ...this.casoSeleccionado };
      this.aplicarFiltros();
      this.mensajeExitoEvaluar = 'Gracias por evaluar nuestro servicio';
      setTimeout(() => this.cerrarModalEvaluar(), 1500);
    }
  }

  puedeSolicitarReapertura(caso: Caso): boolean {
    return caso.estado?.toLowerCase() === 'cerrado';
  }

  estaPlazoReaperturaVencido(caso: Caso): boolean {
    if (!caso.fechaCierre) return false;
    const fechaCierre = new Date(caso.fechaCierre);
    const fechaActual = new Date();
    const diferenciaDias = Math.floor((fechaActual.getTime() - fechaCierre.getTime()) / (1000 * 60 * 60 * 24));
    return diferenciaDias > 15;
  }

  abrirModalReapertura(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.mensajeErrorReapertura = '';
    this.mensajeExitoReapertura = '';
    this.archivoReaperturaNombre = '';
    this.reaperturaForm.reset({ motivo: '' });

    if (this.estaPlazoReaperturaVencido(caso)) {
      this.mensajeErrorReapertura = 'El plazo para solicitar la reapertura ha vencido. Puede registrar un nuevo caso (CU-05)';
    }
    this.mostrarModalReapertura = true;
  }

  cerrarModalReapertura(): void {
    this.mostrarModalReapertura = false;
    this.mensajeErrorReapertura = '';
    this.mensajeExitoReapertura = '';
    this.archivoReaperturaNombre = '';
  }

  onArchivoReaperturaSeleccionado(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const archivo = input.files[0];
      const formatosPermitidos = ['image/jpeg', 'image/png', 'application/pdf'];
      if (!formatosPermitidos.includes(archivo.type) || archivo.size > 2 * 1024 * 1024) {
        this.mensajeErrorReapertura = 'Archivo inválido (debe ser JPG/PNG/PDF y menor a 2MB).';
        input.value = '';
        return;
      }
      this.archivoReaperturaNombre = archivo.name;
    }
  }

  enviarSolicitudReapertura(): void {
    if (!this.casoSeleccionado) return;
    const motivo = this.reaperturaForm.get('motivo')?.value?.trim();

    if (!motivo || motivo.length < 10) {
      this.mensajeErrorReapertura = 'Debe ingresar los campos obligatorios: Motivo de reapertura (mínimo 10 caracteres)';
      return;
    }

    this.casoSeleccionado.estado = 'Reapertura solicitada';
    this.casoSeleccionado.reapertura = {
      motivo,
      archivoEvidencia: this.archivoReaperturaNombre || undefined,
      fechaSolicitud: new Date().toISOString()
    };

    const index = this.casos.findIndex(c => c.id === this.casoSeleccionado?.id);
    if (index !== -1) this.casos[index] = { ...this.casoSeleccionado };
    this.aplicarFiltros();
    this.mensajeExitoReapertura = 'Solicitud de reapertura enviada correctamente';
    setTimeout(() => this.cerrarModalReapertura(), 1500);
  }

  obtenerClaseEstado(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'pendiente': return 'badge-pendiente';
      case 'en proceso': return 'badge-proceso';
      case 'resuelto': return 'badge-resuelto';
      case 'cerrado': return 'badge-cerrado';
      case 'reapertura solicitada': return 'badge-reapertura';
      case 'rechazado': return 'badge-rechazado';
      case 'cancelado': return 'badge-cancelado';
      default: return 'badge-default';
    }
  }
}