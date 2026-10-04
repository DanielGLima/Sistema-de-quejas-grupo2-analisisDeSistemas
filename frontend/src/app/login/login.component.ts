import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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

  // Formato estricto: usuario@dominio.extension (ej: usuario@correo.com)
  private emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      correoElectronico: ['', [
        Validators.required, 
        Validators.maxLength(100),
        Validators.pattern(this.emailPattern)
      ]],
      password: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  iniciarSesion(): void {
    this.mensajeError = '';

    const correoControl = this.loginForm.get('correoElectronico');
    const passwordControl = this.loginForm.get('password');
    const valorCorreo = correoControl?.value?.trim() || '';
    const valorPassword = passwordControl?.value || '';

    // Si ambos campos o alguno está vacío
    if (!valorCorreo || !valorPassword) {
      this.loginForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // Validación de formato de correo (FA)
    if (!this.emailPattern.test(valorCorreo)) {
      correoControl?.markAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico válido (ejemplo: usuario@correo.com)';
      return;
    }

    const credenciales = {
      correo: valorCorreo,
      password: valorPassword
    };

    this.http.post<any>('http://localhost:8081/api/auth/login', credenciales, {
      withCredentials: true
    }).subscribe({
      next: (usuario) => {
        console.log('¡Login exitoso!', usuario);
        this.router.navigate(['/consultar-casos']);
      },
      error: (error) => {
        console.error('Error al iniciar sesión:', error);
        if (error.status === 401 || error.status === 400) {
          this.mensajeError = 'Correo electrónico o contraseña incorrectos';
        } else {
          this.mensajeError = 'Error de conexión con el servidor backend';
        }
      }
    });
  }
}