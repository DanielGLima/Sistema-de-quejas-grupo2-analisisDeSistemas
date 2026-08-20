import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

export interface Caso {
  id: string;
  tipoCaso: string;
  sucursal: string;
  motivo: string;
  fechaIncidente: string;
  fechaCreacion: string;
  estado: 'Pendiente' | 'En Proceso' | 'Resuelto' | 'Rechazado';
  detalle: string;
  respuesta?: string;
  archivoEvidencia?: string;
}

@Component({
  selector: 'app-consultar-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './consultar-casos.component.html',
  styleUrls: ['./consultar-casos.component.scss']
})
export class ConsultarCasosComponent implements OnInit {
  // Filtros según CU-06
  filtroEstado: string = 'Todos';
  filtroFechaDesde: string = '';
  filtroFechaHasta: string = '';
  filtroTipoCaso: string = 'Todos';

  estadosDisponibles: string[] = ['Todos', 'Pendiente', 'En Proceso', 'Resuelto', 'Rechazado'];
  tiposCaso: string[] = ['Todos', 'Queja', 'Reclamo', 'Sugerencia', 'Felicitación'];

  // Arreglo vacío listo para recibir datos reales de la base de datos/API
  casos: Caso[] = [];
  casosFiltrados: Caso[] = [];
  casoSeleccionado: Caso | null = null;
  mostrarModalDetalle: boolean = false;

  ngOnInit(): void {
    this.aplicarFiltros();
  }

  // Paso 4 y FA01: Aplicación de filtros de consulta
  aplicarFiltros(): void {
    this.casosFiltrados = this.casos.filter(caso => {
      const coincideEstado = this.filtroEstado === 'Todos' || caso.estado === this.filtroEstado;
      const coincideTipo = this.filtroTipoCaso === 'Todos' || caso.tipoCaso === this.filtroTipoCaso;
      const coincideDesde = !this.filtroFechaDesde || caso.fechaCreacion >= this.filtroFechaDesde;
      const coincideHasta = !this.filtroFechaHasta || caso.fechaCreacion <= this.filtroFechaHasta;

      return coincideEstado && coincideTipo && coincideDesde && coincideHasta;
    });
  }

  limpiarFiltros(): void {
    this.filtroEstado = 'Todos';
    this.filtroTipoCaso = 'Todos';
    this.filtroFechaDesde = '';
    this.filtroFechaHasta = '';
    this.aplicarFiltros();
  }

  // Paso 6: Ver detalle del caso seleccionado
  verDetalle(caso: Caso): void {
    this.casoSeleccionado = caso;
    this.mostrarModalDetalle = true;
  }

  cerrarModal(): void {
    this.mostrarModalDetalle = false;
    this.casoSeleccionado = null;
  }

  // Clases dinámicas de color para los estados
  obtenerClaseEstado(estado: string): string {
    switch (estado) {
      case 'Pendiente': return 'badge-pendiente';
      case 'En Proceso': return 'badge-proceso';
      case 'Resuelto': return 'badge-resuelto';
      case 'Rechazado': return 'badge-rechazado';
      default: return '';
    }
  }
}