import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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
  correoGuardado: string = ''; // Guarda el correo para enviarlo en el paso 2

  mensajeError: string = '';
  mensajeExito: string = '';
  cargando: boolean = false;

  private passwordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-])/;
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.correoForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]]
    });

    this.validacionForm = this.fb.group({
      codigoRecuperacion: ['', [Validators.required, Validators.maxLength(6)]],
      nuevaPassword: ['', [Validators.required, Validators.maxLength(20)]],
      confirmacionPassword: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  // Paso 4: Botón "ENVIAR" -> Conecta con POST /api/auth/recuperar/solicitar
  enviarCorreo(): void {
    this.limpiarMensajes();

    const control = this.correoForm.get('correoElectronico');
    const valor = control?.value?.trim() || '';

    if (!valor) {
      this.mensajeError = 'Debe ingresar un correo electrónico';
      return;
    }

    if (control?.invalid) {
      this.mensajeError = 'Debe ingresar un correo electrónico válido';
      return;
    }

    this.cargando = true;
    this.correoGuardado = valor;

    this.http.post<any>(`${this.apiUrl}/recuperar/solicitar`, { correo: valor }).subscribe({
      next: (res) => {
        this.cargando = false;
        this.mensajeExito = res.mensaje || 'Se enviaron instrucciones de recuperación a tu correo';
        this.faseCodigoEnviado = true;
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err.error?.message || err.error?.error || 'Correo no registrado o error de conexión';
      }
    });
  }

  // Paso 8: Botón "Validar" -> Conecta con POST /api/auth/recuperar/confirmar
  validarYRestablecer(): void {
    this.limpiarMensajes();

    const codigo = this.validacionForm.get('codigoRecuperacion')?.value?.trim() || '';
    const pass = this.validacionForm.get('nuevaPassword')?.value || '';
    const confirm = this.validacionForm.get('confirmacionPassword')?.value || '';

    if (!codigo) {
      this.mensajeError = 'Debe ingresar el código de recuperación';
      return;
    }

    if (codigo.length !== 6) {
      this.mensajeError = 'El código ingresado es incorrecto';
      return;
    }

    if (!pass || !confirm) {
      this.mensajeError = 'Debe ingresar y confirmar su nueva contraseña';
      return;
    }

    if (pass.length < 6 || pass.length > 20) {
      this.mensajeError = 'La contraseña debe tener entre 6 y 20 caracteres';
      return;
    }

    if (!this.passwordPattern.test(pass)) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un número y un carácter especial';
      return;
    }

    if (pass !== confirm) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    this.cargando = true;

    const payload = {
      correo: this.correoGuardado,
      codigo: codigo,
      passwordNueva: pass
    };

    this.http.post<any>(`${this.apiUrl}/recuperar/confirmar`, payload).subscribe({
      next: () => {
        this.cargando = false;
        this.mensajeExito = 'Contraseña restablecida exitosamente. Redirigiendo al inicio de sesión...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.cargando = false;
        this.mensajeError = err.error?.message || 'El código de recuperación ingresado es incorrecto o ya expiró';
      }
    });
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}