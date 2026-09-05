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

export interface Caso {
  idCaso: number;
  id: string;
  tipoCaso: string;
  sucursal: string;
  motivo: string;
  fechaIncidente: string;
  fechaCreacion: string;
  estado: string;
  detalle: string;
  respuesta?: string;
  archivoEvidencia?: string;
  motivoCancelacion?: string;
  evaluacion?: EvaluacionCaso;
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

  estadosDisponibles: string[] = ['Todos', 'Pendiente', 'En proceso', 'Resuelto', 'Cerrado', 'Rechazado', 'Cancelado'];
  tiposCaso: string[] = ['Todos', 'Queja', 'Reclamo', 'Felicitación'];
  casos: Caso[] = [];
  casosFiltrados: Caso[] = [];

  casoSeleccionado: Caso | null = null;
  mostrarModalDetalle: boolean = false;

  // Variables CU-07: Cancelar Caso
  mostrarModalCancelar: boolean = false;
  motivoCancelacion: string = '';
  errorMotivoCancelacion: string = '';

  // Variables CU-08: Evaluar Atención
  mostrarModalEvaluar: boolean = false;
  evaluacionForm!: FormGroup;
  mensajeErrorEvaluar: string = '';
  mensajeExitoEvaluar: string = '';

  constructor(private casoService: CasoService, private fb: FormBuilder) {}

  ngOnInit(): void {
    this.evaluacionForm = this.fb.group({
      calificacion: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
      comentarios: ['', [Validators.maxLength(500)]]
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

    if (!motivoLimpio) {
      this.errorMotivoCancelacion = 'El motivo de cancelación es obligatorio.';
      return;
    }

    if (motivoLimpio.length < 10) {
      this.errorMotivoCancelacion = 'El motivo debe tener al menos 10 caracteres.';
      return;
    }

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

  // Reglas CU-08: Solo en estado Resuelto o Cerrado, y sin evaluación previa (FA01)
  puedeEvaluar(caso: Caso): boolean {
    const estado = caso.estado?.toLowerCase();
    const esEstadoValido = estado === 'resuelto' || estado === 'cerrado';
    return esEstadoValido && !caso.evaluacion;
  }

  abrirModalEvaluar(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.mensajeErrorEvaluar = '';
    this.mensajeExitoEvaluar = '';
    this.evaluacionForm.reset({ calificacion: 0, comentarios: '' });
    this.mostrarModalEvaluar = true;
  }

  // FA04: Cancelar evaluación
  cerrarModalEvaluar(): void {
    this.mostrarModalEvaluar = false;
    this.mensajeErrorEvaluar = '';
    this.mensajeExitoEvaluar = '';
    this.evaluacionForm.reset({ calificacion: 0, comentarios: '' });
  }

  seleccionarEstrella(valor: number): void {
    this.evaluacionForm.patchValue({ calificacion: valor });
  }

  // Paso 7: Enviar evaluación
  enviarEvaluacion(): void {
    this.mensajeErrorEvaluar = '';

    const calificacion = this.evaluacionForm.get('calificacion')?.value;
    const comentarios = this.evaluacionForm.get('comentarios')?.value || '';

    // FA02: Calificación no seleccionada
    if (!calificacion || calificacion < 1 || calificacion > 5) {
      this.mensajeErrorEvaluar = 'Debe seleccionar una calificación para continuar';
      return;
    }

    // FA03: Longitud de comentario excede 500 caracteres
    if (comentarios.length > 500) {
      this.mensajeErrorEvaluar = 'El comentario no debe exceder los 500 caracteres';
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

  obtenerClaseEstado(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'nuevo': return 'badge-pendiente';
      case 'en espera': return 'badge-pendiente';
      case 'en proceso': return 'badge-proceso';
      case 'resuelto': return 'badge-resuelto';
      case 'cerrado': return 'badge-resuelto';
      case 'cancelado por el usuario': return 'badge-cancelado';
      case 'reapertura solicitada': return 'badge-rechazado';
      default: return 'badge-default';
    }
  }
}