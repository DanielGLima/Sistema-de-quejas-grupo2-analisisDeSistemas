import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
@Component({
  selector: 'app-actualizar-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './actualizar-perfil.component.html',
  styleUrls: ['./actualizar-perfil.component.scss']
})
export class ActualizarPerfilComponent implements OnInit {
  perfilForm!: FormGroup;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeExito: string = '';
  mensajeInfo: string = '';

  // Catálogo de género según CU-01 / CU-03
  generos: string[] = ['Masculino', 'Femenino', 'Otro'];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.perfilForm = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(50)]],
      apellido: ['', [Validators.required, Validators.maxLength(50)]],
      // El correo electrónico se inicializa bloqueado en modo solo lectura
      correoElectronico: [{ value: '', disabled: true }],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8,15}$/)]],
      genero: ['', Validators.required]
    });
  }

  // Paso 4: Botón "Actualizar Datos"
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    // FA01: Validación de campos obligatorios
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      return;
    }

    // Paso 5: Solicita confirmación
    this.mostrarModalConfirmacion = true;
  }

  // Pasos 6, 7 / FA02: Confirmación o cancelación
  confirmarActualizacion(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      // FA02: Cancelación de la actualización
      this.mensajeInfo = 'Se ha cancelado la actualización del perfil';
      return;
    }

    // Paso 7: Actualización exitosa
    this.mensajeExito = 'Perfil actualizado exitosamente';
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}