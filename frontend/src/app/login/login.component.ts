import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AdminService } from '../services/admin.service';

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
    private authService: AuthService,
    private adminService: AdminService
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

    this.authService.login(correoElectronico, password).subscribe({
      next: () => {
        localStorage.removeItem('rolPersonal');
        // Paso 5: Inicio exitoso y redirección según rol
        this.router.navigate(['/consultar-casos']);
      },
      error: (err) => {
        const mensajeCliente = err.error?.message ?? 'Correo electrónico o contraseña incorrectos';
        this.intentarComoPersonal(correoElectronico, password, mensajeCliente);
      }
    });
  }

  // El personal interno (Administrador General, Gerente, Operador) usa la misma pantalla de acceso.
  private intentarComoPersonal(correo: string, password: string, mensajeCliente: string): void {
    this.adminService.login(correo, password).subscribe({
      next: () => this.router.navigate(['/gestionar-casos']),
      // FA05 - Credenciales incorrectas (mismo mensaje genérico, no se revela qué campo falló)
      error: () => this.mensajeError = mensajeCliente
    });
  }
}
