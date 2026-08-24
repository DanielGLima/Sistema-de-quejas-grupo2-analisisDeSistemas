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

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    this.loginForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  iniciarSesion(): void {
    this.mensajeError = '';

    // FA04 - Campos obligatorios vacíos
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    const { correoElectronico, password } = this.loginForm.value;

    this.authService.login(correoElectronico, password).subscribe({
      next: () => {
        // Paso 5: Inicio exitoso y redirección según rol
        this.router.navigate(['/consultar-casos']);
      },
      error: (err) => {
        // FA05 - Credenciales incorrectas
        this.mensajeError = err.error?.message ?? 'Correo electrónico o contraseña incorrectos';
      }
    });
  }
}