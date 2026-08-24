import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-recuperar-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recuperar-password.component.html',
  styleUrls: ['./recuperar-password.component.scss']
})
export class RecuperarPasswordComponent implements OnInit {
  // Formularios para cada fase del caso de uso
  correoForm!: FormGroup;
  validacionForm!: FormGroup;

  // Control de fases: Paso 3 (solicitar correo) -> Paso 6 y 7 (código y nueva contraseña)
  faseCodigoEnviado: boolean = false;
  correoIngresado: string = '';

  mensajeError: string = '';
  mensajeExito: string = '';

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    // Paso 3: correo electrónico (alfanumérico / formato email, 100 caracteres)
    this.correoForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });

    // Pasos 6 y 7: código de recuperación (6 caracteres), nueva contraseña y confirmación
    this.validacionForm = this.fb.group({
      codigoRecuperacion: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]],
      nuevaPassword: ['', [Validators.required, Validators.maxLength(20), this.validarPasswordFormato]],
      confirmacionPassword: ['', [Validators.required, Validators.maxLength(20)]]
    }, { validators: this.validarCoincidenciaPassword });
  }

  // FA03: Formato de la contraseña (mín 6, máx 20, al menos una mayúscula, un número y un carácter especial)
  validarPasswordFormato(control: AbstractControl): ValidationErrors | null {
    const value = control.value || '';
    if (!value) return null;
    const regex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-])[A-Za-z\d!@#$%^&*(),.?":{}|<>_\-]{6,20}$/;
    return regex.test(value) ? null : { passwordInvalida: true };
  }

  validarCoincidenciaPassword(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('nuevaPassword')?.value;
    const confirm = group.get('confirmacionPassword')?.value;
    return pass === confirm ? null : { passwordNoCoincide: true };
  }

  // Paso 4: Botón "ENVIAR"
  enviarCorreo(): void {
    this.limpiarMensajes();

    if (this.correoForm.invalid) {
      this.correoForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico válido';
      return;
    }

    this.correoIngresado = this.correoForm.value.correoElectronico;

    this.authService.recuperarSolicitar(this.correoIngresado).subscribe({
      next: (respuesta) => {
        // Paso 5 & FA01 (Mensaje de seguridad neutro genérico sin revelar existencia de cuenta)
        this.mensajeExito = respuesta.codigo
          ? `${respuesta.mensaje} (código de prueba, sin envío de correo real todavía: ${respuesta.codigo})`
          : respuesta.mensaje;
        this.faseCodigoEnviado = true;
      },
      error: (err) => {
        this.mensajeError = err.error?.message ?? 'No se pudo procesar la solicitud';
      }
    });
  }

  // Paso 8: Botón "Validar"
  validarYRestablecer(): void {
    this.limpiarMensajes();

    // Verificación de código
    const codigoControl = this.validacionForm.get('codigoRecuperacion');
    if (!codigoControl?.value || codigoControl.value.length !== 6) {
      this.mensajeError = 'Debe ingresar el código de recuperación de 6 caracteres';
      return;
    }

    // FA03 - Formato de contraseña no válido
    const passControl = this.validacionForm.get('nuevaPassword');
    if (passControl?.invalid) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      return;
    }

    // Coincidencia
    if (this.validacionForm.hasError('passwordNoCoincide')) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    const { codigoRecuperacion, nuevaPassword } = this.validacionForm.value;

    this.authService.recuperarConfirmar(this.correoIngresado, codigoRecuperacion, nuevaPassword).subscribe({
      next: () => {
        // Paso 9 & 10: Mensaje de éxito
        alert('Contraseña restablecida exitosamente');
        // Paso 11: Redirige a CU-00 (Inicio de sesión)
        this.router.navigate(['/login']);
      },
      error: (err) => {
        // FA02: código o enlace expirado
        this.mensajeError = err.error?.message ?? 'El código o enlace ha expirado';
      }
    });
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}