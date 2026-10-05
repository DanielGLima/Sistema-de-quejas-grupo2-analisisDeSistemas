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
  correoGuardado: string = '';

  mensajeError: string = '';
  mensajeExito: string = '';
  cargando: boolean = false;

  // Formato estricto para email: usuario@dominio.extension
  private emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  // Al menos una mayúscula, un número y un carácter especial
  private passwordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$\%^&*(),.?":{}\vert{}<>_\-]).+$/;
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.correoForm = this.fb.group({
      correoElectronico: ['', [
        Validators.required, 
        Validators.maxLength(100),
        Validators.pattern(this.emailPattern)
      ]]
    });

    this.validacionForm = this.fb.group({
      codigoRecuperacion: ['', [Validators.required, Validators.maxLength(6)]],
      nuevaPassword: ['', [
        Validators.required, 
        Validators.minLength(6), 
        Validators.maxLength(20),
        Validators.pattern(this.passwordPattern)
      ]],
      confirmacionPassword: ['', [Validators.required]]
    });
  }

  // Paso 4: Botón "ENVIAR" -> Valida correo y conecta con Spring Boot
  enviarCorreo(): void {
    this.limpiarMensajes();

    const control = this.correoForm.get('correoElectronico');
    const valor = control?.value?.trim() || '';

    if (!valor) {
      control?.markAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico';
      return;
    }

    if (!this.emailPattern.test(valor)) {
      control?.markAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico válido (ejemplo: usuario@correo.com)';
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

  // Paso 8: Botón "Validar" -> Valida código, complejidad de contraseña y actualiza
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
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      return;
    }

    if (pass !== confirm) {
      this.mensajeError = 'Las contraseñas no coinciden';
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
        this.limpiarMensajes();
        this.mensajeExito = 'Contraseña restablecida exitosamente';
        this.validacionForm.reset();
        
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