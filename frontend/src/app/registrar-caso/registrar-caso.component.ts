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
  mensajeError: string = '';
  mensajeInfo: string = '';
  mensajeExito: string = '';
  registrando: boolean = false;

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
      idCategoria: ['', Validators.required],
      descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
      numeroFactura: ['', [Validators.maxLength(20), Validators.pattern(/^[A-Za-z0-9]*$/)]],
      nombreEmpleadoInvolucrado: ['', [Validators.maxLength(100), Validators.pattern(/^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]*$/)]],
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

  // CU-05, FA04: la opción de denuncia anónima solo aplica al tipo Denuncia.
  get esDenuncia(): boolean {
    const tipo = this.tiposCaso.find(t => String(t.idTipoCaso) === String(this.casoForm.get('idTipoCaso')?.value));
    return tipo?.nombre === 'Denuncia';
  }

  // Letras y espacios únicamente en el nombre del empleado (texto alfabético).
  soloTexto(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.casoForm.get('nombreEmpleadoInvolucrado')?.setValue(input.value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]/g, ''));
  }

  // Pasos 3 y 4: botón "Registrar caso" y validación de campos y archivos.
  registrarCaso(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';
    this.mensajeExito = '';

    const obligatorios = ['idTipoCaso', 'idSucursal', 'idCategoria', 'descripcion'];
    const faltanObligatorios = obligatorios.some(c => this.casoForm.get(c)?.invalid) || this.archivosSeleccionados.length === 0;

    // FA01: campos obligatorios
    if (faltanObligatorios) {
      this.casoForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // FA02: formato de número de factura no válido (el campo es opcional)
    if (this.casoForm.get('numeroFactura')?.invalid) {
      this.casoForm.get('numeroFactura')?.markAsTouched();
      this.mensajeError = 'El formato de la factura es inválido';
      return;
    }

    if (this.casoForm.get('nombreEmpleadoInvolucrado')?.invalid) {
      this.mensajeError = 'El nombre del empleado involucrado solo puede contener letras (máximo 100 caracteres)';
      return;
    }

    const { idTipoCaso, idSucursal, idCategoria, descripcion, numeroFactura, nombreEmpleadoInvolucrado, esAnonimo } = this.casoForm.value;

    this.registrando = true;
    this.casoService.crear({
      idTipoCaso, idSucursal,
      idCategoria: idCategoria || undefined,
      descripcion,
      numeroFactura: numeroFactura || undefined,
      nombreEmpleadoInvolucrado: nombreEmpleadoInvolucrado || undefined,
      esAnonimo: this.esDenuncia && !!esAnonimo,
      archivos: this.archivosSeleccionados
    }).subscribe({
      next: (caso) => {
        // Paso 9: mensaje de éxito con el código asignado; paso 10: redirección a "Mis casos" (CU-06)
        this.mensajeExito = `Caso registrado exitosamente. Código asignado: ${caso.identificadorVisible}`;
        setTimeout(() => this.router.navigate(['/consultar-casos']), 2500);
      },
      error: (err) => {
        this.registrando = false;
        this.mensajeError = err.error?.message ?? 'No se pudo registrar el caso';
      }
    });
  }
}
