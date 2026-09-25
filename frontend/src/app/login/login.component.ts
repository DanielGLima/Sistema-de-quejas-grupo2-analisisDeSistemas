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

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  iniciarSesion(): void {
    this.mensajeError = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

const { correoElectronico, password } = this.loginForm.value;

    // Enviar exactamente los nombres que espera LoginRequest en Java:
    const credenciales = {
      correo: correoElectronico,
      password: password
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