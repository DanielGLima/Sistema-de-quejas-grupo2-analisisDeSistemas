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
  mensajeExito: string = '';
  servicioFueraDeLinea: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private adminService: AdminService
  ) {}

  ngOnInit(): void {
    // Cierre de sesión (CU-00, flujo 2): aviso de éxito al volver al portal.
    if (history.state?.sesionFinalizada) {
      this.mensajeExito = 'Sesión finalizada correctamente';
    }
    this.loginForm = this.fb.group({
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      password: ['', [Validators.required, Validators.maxLength(20)]]
    });
  }

  iniciarSesion(): void {
    this.mensajeExito = '';
    this.mensajeError = '';
    this.servicioFueraDeLinea = false;

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
        // FA02: portal no disponible
        if (err.status === 0) {
          this.mostrarFueraDeLinea();
          return;
        }
        const mensajeCliente = err.error?.message ?? 'Correo electrónico o contraseña incorrectos';
        this.intentarComoPersonal(correoElectronico, password, mensajeCliente);
      }
    });
  }

  // FA02 / FA03: el servicio no responde; se informa y se ofrece reintentar (vuelve al paso 1).
  private mostrarFueraDeLinea(): void {
    this.servicioFueraDeLinea = true;
    this.mensajeError = 'El servicio se encuentra temporalmente fuera de línea';
  }

  reintentar(): void {
    window.location.reload();
  }

  // El personal interno (Administrador General, Gerente, Operador) usa la misma pantalla de acceso.
  private intentarComoPersonal(correo: string, password: string, mensajeCliente: string): void {
    this.adminService.login(correo, password).subscribe({
      // Paso 6: según el rol, el Administrador General va a su panel de administración (CU-13)
      // y el Gerente / Operador a la bandeja de gestión y filtrado de casos (CU-10 / CU-12).
      next: (personal) => this.router.navigate([personal.rol.nombre === 'Administrador General' ? '/administrar' : '/gestionar-casos']),
      // FA05 - Credenciales incorrectas (mismo mensaje genérico, no se revela qué campo falló)
      error: (err) => {
        if (err.status === 0) {
          this.mostrarFueraDeLinea();
          return;
        }
        this.mensajeError = mensajeCliente;
      }
    });
  }
}
