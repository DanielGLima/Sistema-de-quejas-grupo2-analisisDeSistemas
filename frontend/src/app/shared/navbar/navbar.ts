import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent {
  constructor(private router: Router, private authService: AuthService, private adminService: AdminService) {}

  // Rol del personal interno si hay una sesión de personal (solo para armar el menú).
  get rolPersonal(): string | null {
    return this.adminService.rolGuardado();
  }

  // CU-04: Cerrar Sesión
  cerrarSesion(): void {
    const cierre = this.rolPersonal ? this.adminService.logout() : this.authService.logout();
    cierre.subscribe({
      next: () => this.finalizarSesionLocal(),
      // FA01: si la sesión ya había expirado, igual se redirige al portal sin error
      error: () => this.finalizarSesionLocal()
    });
  }

  private finalizarSesionLocal(): void {
    localStorage.clear();
    sessionStorage.clear();

    // Paso 5: Mensaje de éxito especificado en el caso de uso
    alert('Sesión finalizada correctamente');

    // Paso 6: Redirección al portal público CU-00
    this.router.navigate(['/login']);
  }
}
