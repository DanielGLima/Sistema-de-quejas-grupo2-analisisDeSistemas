import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.scss']
})
export class RegistroComponent implements OnInit {
  registroForm!: FormGroup;
  mensajeError: string = '';
  mensajeExito: string = '';
  mensajeInfo: string = '';

  // Arreglos listos para recibir datos del Backend
  listaNacionalidades: string[] = [];

  // Control de flujo en dos pasos (Datos -> Contraseña)
  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;

  constructor(private fb: FormBuilder, private router: Router) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100)]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', Validators.required]
    }, { validators: this.validarPasswordsIguales });
  }

  validarPasswordsIguales(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('password')?.value;
    const confirm = control.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoincide: true };
  }

  habilitarPassword(): void {
    this.limpiarMensajes();

    // Validar solo los campos de la primera sección
    const camposPaso1 = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion'];
    let formInvalido = false;

    camposPaso1.forEach(campo => {
      const control = this.registroForm.get(campo);
      if (!control || control.invalid) {
        control?.markAsTouched();
        formInvalido = true;
      }
    });

    if (formInvalido) {
      this.mensajeError = 'Debe completar todos los datos personales obligatorios antes de continuar';
      return;
    }

    this.pasoPassword = true;
  }

  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      if (this.registroForm.hasError('noCoincide')) {
        this.mensajeError = 'Las contraseñas no coinciden';
      } else {
        this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      }
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado la creación de la cuenta';
      return;
    }

    this.mensajeExito = 'Usuario registrado exitosamente. Redirigiendo al inicio de sesión...';
    setTimeout(() => {
      this.router.navigate(['/login']);
    }, 2000);
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}