import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
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
  cargando: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Paso 3: Correo electrónico
    this.correoForm = this.fb.group({
      correoElectronico: ['']
    });

    // Paso 8: Código; Paso 9: Nueva contraseña y confirmación. El backend valida todo.
    this.validacionForm = this.fb.group({
      codigoRecuperacion: [''],
      nuevaPassword: [''],
      confirmacionPassword: ['']
    });
  }

  // Paso 5: Botón "ENVIAR" (Evalúa FA08, FA09, FA10)
  enviarCorreo(): void {
    this.limpiarMensajes();

    const control = this.correoForm.get('correoElectronico');
    const valor = control?.value?.trim() || '';

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
        // FA08, FA09 y FA10: mensaje del backend
        this.mensajeError = err.error?.message || 'No se pudo procesar la solicitud';
      }
    });
  }

  // Paso 10: Botón "Validar" (Evalúa FA11 a FA17)
  validarYRestablecer(): void {
    this.limpiarMensajes();

    const codigo = this.validacionForm.get('codigoRecuperacion')?.value?.trim() || '';
    const pass = this.validacionForm.get('nuevaPassword')?.value || '';
    const confirm = this.validacionForm.get('confirmacionPassword')?.value || '';

    this.cargando = true;

    // Paso 12 y 13: Valida código, actualiza contraseña e invalida sesiones previas
    this.authService.recuperarConfirmar(this.correoIngresado, codigo, pass, confirm).subscribe({
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
          // FA11 a FA17: se permanece en el paso 8 con el mensaje del backend
          this.mensajeError = msgBackend || 'No se pudo restablecer la contraseña';
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