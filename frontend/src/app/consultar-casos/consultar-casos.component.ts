import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { CasoService } from '../services/caso.service';

// Forma "aplanada" que usa esta pantalla, distinta del modelo que devuelve la API
// (que trae objetos anidados para tipoCaso/sucursal/estadoCaso).
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
  idCaso: number;
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

  constructor(private casoService: CasoService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.evaluacionForm = this.fb.group({
      calificacion: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      comentarios: ['', [Validators.maxLength(500)]]
    });

    this.reaperturaForm = this.fb.group({
      motivo: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(500)]]
    });

    this.cargarCasos();
  }

  // CU-06
  cargarCasos(): void {
    this.casoService.misCasos().subscribe(casosApi => {
      this.casos = casosApi.map(c => ({
        idCaso: c.idCaso,
        id: c.identificadorVisible,
        tipoCaso: c.tipoCaso.nombre,
        sucursal: c.sucursal.nombreSucursal,
        motivo: c.categoriaCaso?.nombre ?? 'No aplica',
        fechaIncidente: c.fechaCreacion,
        fechaCreacion: c.fechaCreacion,
        estado: c.estadoCaso.nombre,
        detalle: c.descripcion,
        evaluacion: c.evaluacion ? {
          calificacion: c.evaluacion.calificacion,
          comentarios: c.evaluacion.comentario,
          fechaEvaluacion: c.evaluacion.fechaEvaluacion
        } : undefined
      }));

      this.estadosDisponibles = ['Todos', ...new Set(this.casos.map(c => c.estado))];
      this.tiposCaso = ['Todos', ...new Set(this.casos.map(c => c.tipoCaso))];
      this.aplicarFiltros();
    });
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

  // Regla CU-07: solo en estado "Nuevo" o "En espera" (catalogo real de estado_caso)
  puedeCancelar(caso: Caso): boolean {
    const estado = caso.estado?.toLowerCase();
    return estado === 'nuevo' || estado === 'en espera';
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
      this.casoService.cancelar(this.casoSeleccionado.idCaso, motivoLimpio).subscribe({
        next: (casoApi) => {
          if (this.casoSeleccionado) {
            this.casoSeleccionado.estado = casoApi.estadoCaso.nombre;
            this.casoSeleccionado.motivoCancelacion = motivoLimpio;

            const index = this.casos.findIndex(c => c.idCaso === this.casoSeleccionado?.idCaso);
            if (index !== -1) {
              this.casos[index] = { ...this.casoSeleccionado };
            }
          }

          this.aplicarFiltros();
          this.cerrarModalCancelar();
        },
        error: (err) => {
          // FA01: el caso ya esta en atencion y no puede cancelarse
          this.errorMotivoCancelacion = err.error?.message ?? 'No se pudo cancelar el caso';
        }
      });
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
      this.casoService.evaluar(this.casoSeleccionado.idCaso, calificacion, comentarios.trim()).subscribe({
        next: (evaluacionApi) => {
          if (this.casoSeleccionado) {
            // Paso 8: Registro de la evaluación asociada al caso
            this.casoSeleccionado.evaluacion = {
              calificacion: evaluacionApi.calificacion,
              comentarios: evaluacionApi.comentario,
              fechaEvaluacion: evaluacionApi.fechaEvaluacion
            };

            // Sincronizar en la lista general para reflejar bloqueo (Paso 10 / FA01)
            const index = this.casos.findIndex(c => c.idCaso === this.casoSeleccionado?.idCaso);
            if (index !== -1) {
              this.casos[index] = { ...this.casoSeleccionado };
            }
          }

          this.aplicarFiltros();

          // Paso 9: Mensaje de éxito exacto
          this.mensajeExitoEvaluar = 'Gracias por evaluar nuestro servicio';

          // Paso 10: Cierre del modal tras confirmación
          setTimeout(() => {
            this.cerrarModalEvaluar();
          }, 1500);
        },
        error: (err) => {
          this.mensajeErrorEvaluar = err.error?.message ?? 'No se pudo registrar la evaluación';
        }
      });
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
      case 'nuevo': return 'badge-pendiente';
      case 'en espera': return 'badge-pendiente';
      case 'en proceso': return 'badge-proceso';
      case 'resuelto': return 'badge-resuelto';
      case 'cerrado': return 'badge-cerrado';
      case 'reapertura solicitada': return 'badge-reapertura';
      case 'rechazado': return 'badge-rechazado';
      case 'cancelado': return 'badge-cancelado';
      case 'cancelado por el usuario': return 'badge-cancelado';
      default: return 'badge-default';
    }
  }
}