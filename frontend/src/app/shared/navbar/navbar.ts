import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrls: ['./navbar.scss']
})
export class NavbarComponent {
  constructor(private router: Router) {}

  // CU-04: Cerrar Sesión
  cerrarSesion(): void {
    // Paso 4: Eliminar datos locales de sesión
    localStorage.clear();
    sessionStorage.clear();

    // Paso 5: Mensaje de éxito especificado en el caso de uso[cite: 1]
    alert('Sesión finalizada correctamente');

    // Paso 6: Redirección al portal público CU-00[cite: 1]
    this.router.navigate(['/login']);
  }
}