import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent {
  mostrarModalLogout: boolean = false;
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(
    private http: HttpClient,
    private router: Router,
    private cd: ChangeDetectorRef
  ) {}

  // Paso 1: El usuario selecciona la opción "Cerrar sesión"
  abrirModalLogout(): void {
    this.mostrarModalLogout = true;
    this.cd.detectChanges();
  }

  // Paso 1 (FA01) y Pasos 2 al 6 del Flujo Básico
  confirmarCierreSesion(acepta: boolean): void {
    this.mostrarModalLogout = false;

    // FA01: El usuario selecciona "No"
    if (!acepta) {
      this.cd.detectChanges();
      return;
    }

    // Paso 2 y 3: Solicitud de cierre y destrucción de sesión en servidor
    this.http.post<void>(`${this.apiUrl}/logout`, {}, { withCredentials: true }).subscribe({
      next: () => {
        this.limpiarSesionYRedirigir();
      },
      error: () => {
        // En caso de sesión previa caducada, limpia igualmente en cliente
        this.limpiarSesionYRedirigir();
      }
    });
  }

  private limpiarSesionYRedirigir(): void {
    // Paso 4: Elimina los datos locales de sesión en el navegador
    localStorage.removeItem('usuario_actual');
    sessionStorage.clear();

    // Paso 5: Mensaje exacto de éxito
    alert('Sesión finalizada correctamente');

    // Paso 6: Redirige al portal público (CU-00)
    this.router.navigate(['/login']);
  }
}