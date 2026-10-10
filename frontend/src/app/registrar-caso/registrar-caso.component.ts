import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar';
import { CasoService } from '../services/caso.service';
import { CatalogoService } from '../services/catalogo.service';
import { TipoCaso, CategoriaCaso, Sucursal } from '../core/models/catalogos';

const TAMANO_MAXIMO_BYTES = 2 * 1024 * 1024; // 2 MB (FA03)
const TIPOS_PERMITIDOS = ['image/jpeg', 'image/png', 'application/pdf'];
const PATRON_FACTURA = /^[A-Za-z0-9]{1,20}$/; // Alfanumérico sin símbolos (FA02)
const PATRON_EMPLEADO = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]{1,100}$/; // Solo letras (FA05)

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

  // Paso 2: Solicitud de campos y definición de validaciones reactivas
  inicializarFormulario(): void {
    this.casoForm = this.fb.group({
      idTipoCaso: ['', Validators.required],
      idSucursal: ['', Validators.required],
      idCategoria: ['', Validators.required],
      descripcion: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
      numeroFactura: ['', [Validators.maxLength(20), Validators.pattern(PATRON_FACTURA)]],
      nombreEmpleadoInvolucrado: ['', [Validators.maxLength(100), Validators.pattern(PATRON_EMPLEADO)]],
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

  // FA03: Validación de archivos adjuntos (máximo 2 MB por archivo, formatos JPG, PNG o PDF)
  alSeleccionarArchivos(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      this.archivosSeleccionados = [];
      return;
    }

    this.mensajeError = '';
    const archivos = Array.from(input.files);

    const archivoInvalido = archivos.some(a => {
      const extValida = /\.(jpg|jpeg|png|pdf)$/i.test(a.name);
      return a.size > TAMANO_MAXIMO_BYTES || (!TIPOS_PERMITIDOS.includes(a.type) && !extValida);
    });

    if (archivoInvalido) {
      this.mensajeError = 'El archivo adjunto supera los 2 MB o no corresponde a un formato permitido (PDF/Imagen)';
      input.value = '';
      this.archivosSeleccionados = [];
      return;
    }

    this.archivosSeleccionados = archivos;
  }

  // FA04: Disponibilidad exclusiva para tipo Denuncia
  get esDenuncia(): boolean {
    const tipo = this.tiposCaso.find(t => String(t.idTipoCaso) === String(this.casoForm.get('idTipoCaso')?.value));
    return tipo?.nombre?.trim().toLowerCase() === 'denuncia';
  }

  soloTexto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]/g, '');
    this.casoForm.get('nombreEmpleadoInvolucrado')?.setValue(limpio);
  }

  // Paso 3: El usuario completa los campos y presiona el botón "Registrar caso"
  registrarCaso(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';
    this.mensajeExito = '';

    const { idTipoCaso, idSucursal, idCategoria, descripcion, numeroFactura, nombreEmpleadoInvolucrado, esAnonimo } = this.casoForm.value;

    // Paso 4: Validación de campos obligatorios y evidencia adjunta
    // FA01: Campos obligatorios incompletos o vacíos
    if (!idTipoCaso || !idSucursal || !idCategoria || !descripcion || descripcion.trim().length < 10 || this.archivosSeleccionados.length === 0) {
      this.casoForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // FA02: Formato del número de factura no válido
    if (numeroFactura && !PATRON_FACTURA.test(numeroFactura.trim())) {
      this.mensajeError = 'El formato de la factura es inválido';
      return;
    }

    // FA05: Formato del nombre del empleado involucrado no válido
    if (nombreEmpleadoInvolucrado && !PATRON_EMPLEADO.test(nombreEmpleadoInvolucrado.trim())) {
      this.mensajeError = 'El nombre del empleado involucrado solo puede contener letras (máximo 100 caracteres)';
      return;
    }

    this.registrando = true;
    // Paso 5 a 7: Creación en el sistema, generación de código único y asignación de estado "Nuevo"
    this.casoService.crear({
      idTipoCaso,
      idSucursal,
      idCategoria,
      descripcion: descripcion.trim(),
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