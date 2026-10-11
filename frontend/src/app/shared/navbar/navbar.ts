import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    document.body.classList.add('con-menu');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('con-menu');
  }

  // Rol del usuario actual (CLIENTE, OPERADOR, GERENTE, ADMINISTRADOR)
  get rolUsuario(): string | null {
    return this.authService.getRol();
  }

  // Propiedades requeridas por navbar.html
  get esPersonal(): boolean {
    const rol = this.rolUsuario?.toUpperCase();
    return rol === 'OPERADOR' || rol === 'GERENTE' || rol === 'ADMINISTRADOR' || rol === 'ADMINISTRADOR GENERAL';
  }

  get esAdministrador(): boolean {
    const rol = this.rolUsuario?.toUpperCase();
    return rol === 'ADMINISTRADOR' || rol === 'ADMINISTRADOR GENERAL';
  }

  get etiquetaRol(): string {
    const rol = this.rolUsuario?.toUpperCase();
    if (rol === 'ADMINISTRADOR' || rol === 'ADMINISTRADOR GENERAL') return 'Administrador General';
    if (rol === 'GERENTE') return 'Gerente';
    if (rol === 'OPERADOR') return 'Operador';
    return 'Cliente';
  }

  // CU-00 Flujo Normal 2: Cerrar sesión
  cerrarSesion(): void {
    this.authService.logout().subscribe({
      next: () => {
        // Pasos 3, 4, 5 y 6: Sesión destruida en servidor y navegador -> Redirige al portal con aviso de éxito
        this.finalizarSesionLocal(true);
      },
      error: () => {
        // FA06: Si la sesión ya había expirado por inactividad, redirige al portal público sin errores
        this.finalizarSesionLocal(false);
      }
    });
  }

  private finalizarSesionLocal(mostrarMensajeExito: boolean): void {
    this.authService.limpiarSesionLocal();
    localStorage.clear();
    sessionStorage.clear();

    if (mostrarMensajeExito) {
      this.router.navigate(['/login'], { state: { sesionFinalizada: true } });
    } else {
      this.router.navigate(['/login']);
    }
  }
}