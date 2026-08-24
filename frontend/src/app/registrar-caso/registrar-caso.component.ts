import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar';
import { CasoService } from '../services/caso.service';
import { CatalogoService } from '../services/catalogo.service';
import { TipoCaso, CategoriaCaso, Sucursal } from '../core/models/catalogos';

const TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024;
const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'application/pdf'];

@Component({
  selector: 'app-registrar-caso',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './registrar-caso.component.html',
  styleUrls: ['./registrar-caso.component.scss']
})
export class RegistrarCasoComponent implements OnInit {
  casoForm!: FormGroup;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeInfo: string = '';

  archivosSeleccionados: File[] = [];

  tiposCaso: TipoCaso[] = [];
  categorias: CategoriaCaso[] = [];
  sucursales: Sucursal[] = [];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private casoService: CasoService,
    private catalogoService: CatalogoService
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarCatalogos();
  }

  inicializarFormulario(): void {
    this.casoForm = this.fb.group({
      idTipoCaso: ['', Validators.required],
      idSucursal: ['', Validators.required],
      idCategoria: [''],
      descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
      numeroFactura: ['', Validators.maxLength(20)],
      nombreEmpleadoInvolucrado: ['', Validators.maxLength(100)],
      esAnonimo: [false]
    });
  }

  cargarCatalogos(): void {
    this.catalogoService.tiposCaso().subscribe(tipos => this.tiposCaso = tipos);
    this.catalogoService.categoriasCaso().subscribe(categorias => this.categorias = categorias);
    this.catalogoService.sucursales().subscribe(sucursales => this.sucursales = sucursales);
  }

  // CU-05, FA03: máximo 2MB por archivo, solo PDF/JPG/PNG
  alSeleccionarArchivos(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) {
      return;
    }

    this.mensajeError = '';
    const archivosValidos: File[] = [];

    for (const archivo of Array.from(input.files)) {
      if (archivo.size > TAMANO_MAXIMO_BYTES || !TIPOS_PERMITIDOS.includes(archivo.type)) {
        this.mensajeError = 'El archivo adjunto supera los 2 MB o no corresponde a un formato permitido (PDF/Imagen)';
        continue;
      }
      archivosValidos.push(archivo);
    }

    this.archivosSeleccionados = archivosValidos;
  }

  solicitarConfirmacion(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';

    if (this.casoForm.invalid) {
      this.casoForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado el registro del caso';
      return;
    }

    const { idTipoCaso, idSucursal, idCategoria, descripcion, numeroFactura, nombreEmpleadoInvolucrado, esAnonimo } = this.casoForm.value;

    this.casoService.crear({
      idTipoCaso, idSucursal,
      idCategoria: idCategoria || undefined,
      descripcion,
      numeroFactura: numeroFactura || undefined,
      nombreEmpleadoInvolucrado: nombreEmpleadoInvolucrado || undefined,
      esAnonimo,
      archivos: this.archivosSeleccionados
    }).subscribe({
      next: () => {
        // Redirección al panel tras confirmar
        this.router.navigate(['/consultar-casos']);
      },
      error: (err) => {
        this.mensajeError = err.error?.message ?? 'No se pudo registrar el caso';
      }
    });
  }
}
