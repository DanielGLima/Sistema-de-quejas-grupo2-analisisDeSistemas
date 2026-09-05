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
  correoForm!: FormGroup;
  validacionForm!: FormGroup;

  faseCodigoEnviado: boolean = false;
  correoIngresado: string = '';

  mensajeError: string = '';
  mensajeExito: string = '';

  // Expresión para formato: al menos una mayúscula, un número y un carácter especial
  private passwordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-])/;

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    // Paso 3: correo electrónico (alfanumérico / formato email, hasta 100 caracteres)
    this.correoForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });

    // Pasos 6 y 7: código de recuperación (6 caracteres), nueva contraseña y confirmación
    this.validacionForm = this.fb.group({
      codigoRecuperacion: ['', [Validators.required, Validators.maxLength(6)]],
      nuevaPassword: ['', [Validators.required, Validators.maxLength(20)]],
      confirmacionPassword: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  // Paso 4: Botón "ENVIAR"
  enviarCorreo(): void {
    this.limpiarMensajes();

    const control = this.correoForm.get('correoElectronico');
    const valor = control?.value?.trim() || '';

    // FA01: Campo correo electrónico vacío
    if (!valor) {
      this.mensajeError = 'Debe ingresar un correo electrónico';
      return;
    }

    // FA02: Formato de correo electrónico no válido
    if (control?.invalid) {
      this.mensajeError = 'Debe ingresar un correo electrónico válido';
      return;
    }

    this.correoIngresado = this.correoForm.value.correoElectronico;

    this.authService.recuperarSolicitar(this.correoIngresado).subscribe({
      next: (respuesta) => {
        this.mensajeExito = respuesta.mensaje;
        this.faseCodigoEnviado = true;
      },
      error: (err) => {
        // Ej. "Correo no registrado" cuando el correo no existe en la BD
        this.mensajeError = err.error?.message ?? 'No se pudo procesar la solicitud';
      }
    });
  }

  // Paso 8: Botón "Validar"
  validarYRestablecer(): void {
    this.limpiarMensajes();

    const codigo = this.validacionForm.get('codigoRecuperacion')?.value?.trim() || '';
    const pass = this.validacionForm.get('nuevaPassword')?.value || '';
    const confirm = this.validacionForm.get('confirmacionPassword')?.value || '';

    // FA04: Campo código de recuperación vacío
    if (!codigo) {
      this.mensajeError = 'Debe ingresar el código de recuperación';
      return;
    }

    // FA05: Código de recuperación no válido o incorrecto
    if (codigo.length !== 6) {
      this.mensajeError = 'El código ingresado es incorrecto';
      return;
    }

    // FA07: Campos de contraseña vacíos
    if (!pass || !confirm) {
      this.mensajeError = 'Debe ingresar y confirmar su nueva contraseña';
      return;
    }

    // FA08: Longitud de la contraseña fuera de rango (Mínimo 6 y Máximo 20)
    if (pass.length < 6 || pass.length > 20) {
      this.mensajeError = 'La contraseña debe tener entre 6 y 20 caracteres';
      return;
    }

    // FA09: Formato de la contraseña no válido
    if (!this.passwordPattern.test(pass)) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un número y un carácter especial';
      return;
    }

    // FA10: Las contraseñas no coinciden
    if (pass !== confirm) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    const { codigoRecuperacion, nuevaPassword } = this.validacionForm.value;

    this.authService.recuperarConfirmar(this.correoIngresado, codigoRecuperacion, nuevaPassword).subscribe({
      next: () => {
        // Paso 9 y 10: Mensaje de confirmación en la UI
        this.mensajeExito = 'Contraseña restablecida exitosamente. Redirigiendo...';

        // Paso 11: Redirección automática a CU-00 (Login)
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        // FA02: código o enlace expirado
        this.mensajeError = err.error?.message ?? 'El código es incorrecto';
      }
    });
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}
