import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  loginForm!: FormGroup;
  mensajeError: string = '';
  mensajeExito: string = '';
  servicioFueraDeLinea: boolean = false;
  cargando: boolean = false;

  // Regex para formato de correo estándar: usuario@dominio.extension
  private readonly PATRON_CORREO = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (history.state?.sesionFinalizada) {
      this.mensajeExito = 'Sesión finalizada correctamente';
    }

    this.loginForm = this.fb.group({
      correoElectronico: ['', [
        Validators.required,
        Validators.maxLength(100),
        Validators.pattern(this.PATRON_CORREO)
      ]],
      password: ['', [
        Validators.required,
        Validators.maxLength(20)
      ]]
    });
  }

  iniciarSesion(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
    this.servicioFueraDeLinea = false;

    const correoControl = this.loginForm.get('correoElectronico');
    const passControl = this.loginForm.get('password');

    const correo = correoControl?.value?.trim() || '';
    const pass = passControl?.value || '';

    // FA04: Campos obligatorios vacíos
    if (!correo || !pass) {
      this.loginForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // Validación de formato de correo
    if (!this.PATRON_CORREO.test(correo)) {
      correoControl?.setErrors({ pattern: true });
      this.mensajeError = 'El formato del correo electrónico es inválido';
      return;
    }

    this.cargando = true;

    this.authService.login(correo, pass).subscribe({
      next: (res) => {
        this.cargando = false;
        const rol = (res.rol || '').toUpperCase();

        if (rol === 'CLIENTE') {
          this.router.navigate(['/consultar-casos']);
        } else if (rol === 'OPERADOR' || rol === 'GERENTE') {
          this.router.navigate(['/gestionar-casos']);
        } else if (rol.includes('ADMIN')) {
          this.router.navigate(['/administrar']);
        } else {
          this.router.navigate(['/consultar-casos']);
        }
      },
      error: (err) => {
        this.cargando = false;

        // FA02 / FA03: Portal o servicio fuera de línea
        if (err.status === 0) {
          this.servicioFueraDeLinea = true;
          this.mensajeError = 'El servicio se encuentra temporalmente fuera de línea.';
          return;
        }

        if (err.status === 400) {
          this.mensajeError = err.error?.message || 'Debe ingresar los campos obligatorios';
          return;
        }

        // FA05: Credenciales incorrectas
        this.mensajeError = 'Correo electrónico o contraseña incorrectos';
      }
    });
  }

  reintentar(): void {
    this.servicioFueraDeLinea = false;
    this.mensajeError = '';
    window.location.reload();
  }
}