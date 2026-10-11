import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// FA16: al menos una letra mayúscula, un número y un carácter especial
const PATRON_PASSWORD = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-]).+$/;

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
  cargando: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Paso 3: Correo electrónico (alfanumérico / formato email, hasta 100 caracteres)
    this.correoForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_CORREO)]]
    });

    // Paso 8: Código (6 caracteres alfanumérico); Paso 9: Nueva contraseña y confirmación (6 a 20 caracteres)
    this.validacionForm = this.fb.group({
      codigoRecuperacion: ['', [Validators.required, Validators.maxLength(6), Validators.pattern(/^[A-Za-z0-9]{6}$/)]],
      nuevaPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]],
      confirmacionPassword: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]]
    });
  }

  // Paso 5: Botón "ENVIAR" (Evalúa FA08, FA09, FA10)
  enviarCorreo(): void {
    this.limpiarMensajes();

    const control = this.correoForm.get('correoElectronico');
    const valor = control?.value?.trim() || '';

    // FA08: Campo correo electrónico vacío
    if (!valor) {
      this.mensajeError = 'Debe ingresar un correo electrónico';
      return;
    }

    // FA09: Formato de correo electrónico no válido
    if (!PATRON_CORREO.test(valor) || valor.length > 100) {
      this.mensajeError = 'Debe ingresar un correo electrónico válido';
      return;
    }

    this.correoIngresado = valor;
    this.cargando = true;

    // Paso 7: El sistema genera el código (15 min) y lo envía al correo
    this.authService.recuperarSolicitar(this.correoIngresado).subscribe({
      next: (respuesta) => {
        this.cargando = false;
        // Paso 6: Mensaje de confirmación del envío del código
        this.mensajeExito = respuesta.mensaje || 'Se enviaron instrucciones de recuperación a tu correo';
        this.faseCodigoEnviado = true;
      },
      error: (err) => {
        this.cargando = false;
        // FA10: Correo electrónico no registrado
        if (err.status === 400 || err.status === 404) {
          this.mensajeError = 'Correo no registrado';
        } else {
          this.mensajeError = err.error?.message || 'No se pudo procesar la solicitud';
        }
      }
    });
  }

  // Paso 10: Botón "Validar" (Evalúa FA11 a FA17)
  validarYRestablecer(): void {
    this.limpiarMensajes();

    const codigo = this.validacionForm.get('codigoRecuperacion')?.value?.trim() || '';
    const pass = this.validacionForm.get('nuevaPassword')?.value || '';
    const confirm = this.validacionForm.get('confirmacionPassword')?.value || '';

    // FA11: Campo código de recuperación vacío
    if (!codigo) {
      this.mensajeError = 'Debe ingresar el código de recuperación';
      return;
    }

    // FA12: Código de recuperación no válido o de longitud diferente a 6
    if (codigo.length !== 6) {
      this.mensajeError = 'El código de recuperación ingresado es incorrecto';
      return;
    }

    // FA14: Campos de contraseña vacíos (recuperación)
    if (!pass || !confirm) {
      this.mensajeError = 'Debe ingresar y confirmar su nueva contraseña';
      return;
    }

    // FA15: Longitud de la contraseña fuera de rango (6 a 20)
    if (pass.length < 6 || pass.length > 20) {
      this.mensajeError = 'La contraseña debe tener entre 6 y 20 caracteres';
      return;
    }

    // FA16: Formato de la contraseña no válido (al menos una mayúscula, un número y un carácter especial)
    if (!PATRON_PASSWORD.test(pass)) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un número y un carácter especial';
      return;
    }

    // FA17: Las contraseñas no coinciden
    if (pass !== confirm) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    this.cargando = true;

    // Paso 12 y 13: Valida código, actualiza contraseña e invalida sesiones previas
    this.authService.recuperarConfirmar(this.correoIngresado, codigo, pass).subscribe({
      next: () => {
        this.cargando = false;
        // Paso 14: Mensaje de éxito
        this.mensajeExito = 'Contraseña restablecida exitosamente';

        // Paso 15: Redirección automática a la pantalla de inicio de sesión (CU-00)
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1800);
      },
      error: (err) => {
        this.cargando = false;
        const msgBackend = err.error?.message || '';

        // FA13: Código de recuperación expirado -> Retorna al paso 3 (reingresar correo)
        if (msgBackend.toLowerCase().includes('expirado')) {
          this.mensajeError = 'El código de recuperación ha expirado. Solicite uno nuevo';
          this.faseCodigoEnviado = false;
          this.validacionForm.reset();
        } else {
          // FA12: Código de recuperación incorrecto -> Permanece en el paso 8
          this.mensajeError = 'El código de recuperación ingresado es incorrecto';
        }
      }
    });
  }

  // Paso 4 y 11 (FA18): Cancelar recuperación y volver al inicio de sesión descartando datos
  cancelarYVolver(): void {
    this.correoForm.reset();
    this.validacionForm.reset();
    this.faseCodigoEnviado = false;
    this.router.navigate(['/login']);
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}