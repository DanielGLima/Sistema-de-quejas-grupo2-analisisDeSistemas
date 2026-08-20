import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-registrar-caso',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registrar-caso.component.html',
  styleUrls: ['./registrar-caso.component.scss']
})
export class RegistrarCasoComponent implements OnInit {
  casoForm!: FormGroup;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeInfo: string = '';
  nombreArchivo: string = '';

  // Listas vacías: se poblarán desde el backend
  tiposCaso: string[] = [];
  sucursales: string[] = [];
  listaMotivosDisponibles: string[] = [];

  constructor(private fb: FormBuilder, private router: Router) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.casoForm = this.fb.group({
      tipoCaso: ['', Validators.required],
      sucursal: ['', Validators.required],
      motivo: ['', Validators.required],
      fechaIncidente: ['', Validators.required],
      detalle: ['', [Validators.required, Validators.maxLength(1000)]],
      evidencia: [null]
    });
  }

  alCambiarTipoCaso(): void {
    // Al seleccionar el tipo de caso se restablece el motivo para esperar los datos
    this.casoForm.get('motivo')?.setValue('');
    this.listaMotivosDisponibles = [];
  }

  alSeleccionarArchivo(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const archivo = input.files[0];
      
      // Validación de tamaño máximo (5MB)
      if (archivo.size > 5 * 1024 * 1024) {
        this.mensajeError = 'El archivo supera el tamaño máximo permitido de 5MB';
        input.value = '';
        this.nombreArchivo = '';
        return;
      }

      this.nombreArchivo = archivo.name;
      this.casoForm.patchValue({ evidencia: archivo });
      this.mensajeError = '';
    }
  }

  solicitarConfirmacion(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';

    if (this.casoForm.invalid) {
      this.casoForm.markAllAsTouched();
      this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
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

    // Redirección al panel tras confirmar
    this.router.navigate(['/consultar-casos']);
  }
}