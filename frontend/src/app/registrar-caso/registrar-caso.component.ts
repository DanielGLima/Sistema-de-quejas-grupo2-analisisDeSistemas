import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar';
import { CasoService } from '../services/caso.service';
import { CatalogoService } from '../services/catalogo.service';
import { TipoCaso, CategoriaCaso, Sucursal } from '../core/models/catalogos';

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

  // Paso 2: Solicitud de campos (todas las validaciones las realiza el backend)
  inicializarFormulario(): void {
    this.casoForm = this.fb.group({
      idTipoCaso: [''],
      idSucursal: [''],
      idCategoria: [''],
      descripcion: [''],
      numeroFactura: [''],
      nombreEmpleadoInvolucrado: [''],
      esAnonimo: [false]
    });

    // FA04: Desmarca automáticamente la opción si el tipo seleccionado no es Denuncia
    this.casoForm.get('idTipoCaso')?.valueChanges.subscribe(() => {
      if (!this.esDenuncia) {
        this.casoForm.get('esAnonimo')?.setValue(false);
      }
    });
  }

  cargarCatalogos(): void {
    this.catalogoService.tiposCaso().subscribe(tipos => this.tiposCaso = tipos);
    this.catalogoService.categoriasCaso().subscribe(categorias => this.categorias = categorias);
    this.catalogoService.sucursales().subscribe(sucursales => this.sucursales = sucursales);
  }

  // FA03: el backend valida el tamaño (máximo 2 MB) y el formato (JPG, PNG o PDF) de los archivos
  alSeleccionarArchivos(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      this.archivosSeleccionados = [];
      return;
    }

    this.mensajeError = '';
    this.archivosSeleccionados = Array.from(input.files);
  }

  // FA04: Disponibilidad exclusiva para tipo Denuncia
  get esDenuncia(): boolean {
    const tipo = this.tiposCaso.find(t => String(t.idTipoCaso) === String(this.casoForm.get('idTipoCaso')?.value));
    return tipo?.nombre?.trim().toLowerCase() === 'denuncia';
  }

  // Paso 3: El usuario completa los campos y presiona el botón "Registrar caso"
  registrarCaso(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';
    this.mensajeExito = '';

    const { idTipoCaso, idSucursal, idCategoria, descripcion, numeroFactura, nombreEmpleadoInvolucrado, esAnonimo } = this.casoForm.value;

    this.registrando = true;
    // Paso 5 a 7: Creación en el sistema, generación de código único y asignación de estado "Nuevo"
    this.casoService.crear({
      idTipoCaso,
      idSucursal,
      idCategoria,
      descripcion: (descripcion ?? '').trim(),
      numeroFactura: numeroFactura ? numeroFactura.trim() : undefined,
      nombreEmpleadoInvolucrado: nombreEmpleadoInvolucrado ? nombreEmpleadoInvolucrado.trim() : undefined,
      esAnonimo: this.esDenuncia && !!esAnonimo,
      archivos: this.archivosSeleccionados
    }).subscribe({
      next: (caso) => {
        this.registrando = false;
        // Paso 6: Mensaje de éxito indicando el código asignado
        this.mensajeExito = `Caso registrado exitosamente. Código asignado: ${caso.identificadorVisible}`;
        // Paso 10: Redirección automática a la sección "Mis casos" (CU-03)
        setTimeout(() => this.router.navigate(['/consultar-casos']), 2200);
      },
      error: (err) => {
        this.registrando = false;
        this.mensajeError = err.error?.message || 'Debe ingresar los campos obligatorios';
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}