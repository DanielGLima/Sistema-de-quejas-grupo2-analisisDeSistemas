import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

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

  constructor(private fb: FormBuilder, private router: Router) {}

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

    // Simulación de autenticación (cuando integres el backend, esto irá en un servicio HTTP)
    // Ejemplo de FA05 - Credenciales incorrectas:
    // this.mensajeError = 'Correo electrónico o contraseña incorrectos';
    
    // Paso 5: Inicio exitoso y redirección según rol
    console.log('Inicio de sesión válido:', { correoElectronico, password });
  }
}