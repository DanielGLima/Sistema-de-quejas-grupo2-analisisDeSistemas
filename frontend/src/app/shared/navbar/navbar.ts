import { Component, OnDestroy, OnInit } from '@angular/core';
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
export class NavbarComponent implements OnInit, OnDestroy {
  constructor(private router: Router, private authService: AuthService, private adminService: AdminService) {}

  // Con el menú lateral visible, el contenido de la página deja espacio a la izquierda (ver styles.scss).
  ngOnInit(): void {
    document.body.classList.add('con-menu');
  }

  ngOnDestroy(): void {
    document.body.classList.remove('con-menu');
  }

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

    // Pasos 5 y 6: redirección al portal público (CU-00) con el mensaje de éxito en pantalla, sin alert.
    this.router.navigate(['/login'], { state: { sesionFinalizada: true } });
  }
}
