import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';

export interface Caso {
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
}

@Component({
  selector: 'app-consultar-casos',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './consultar-casos.component.html',
  styleUrls: ['./consultar-casos.component.scss']
})
export class ConsultarCasosComponent implements OnInit {
  filtroEstado: string = 'Todos';
  filtroTipoCaso: string = 'Todos';
  filtroFechaDesde: string = '';
  filtroFechaHasta: string = '';

  // Arreglos vacíos listos para recibir datos del Backend
  estadosDisponibles: string[] = ['Todos'];
  tiposCaso: string[] = ['Todos'];
  casos: Caso[] = [];
  casosFiltrados: Caso[] = [];

  casoSeleccionado: Caso | null = null;
  mostrarModalDetalle: boolean = false;

  ngOnInit(): void {
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

  obtenerClaseEstado(estado: string): string {
    switch (estado?.toLowerCase()) {
      case 'pendiente': return 'badge-pendiente';
      case 'en proceso': return 'badge-proceso';
      case 'resuelto': return 'badge-resuelto';
      case 'rechazado': return 'badge-rechazado';
      default: return 'badge-default';
    }
  }
}