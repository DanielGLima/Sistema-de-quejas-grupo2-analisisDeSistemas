import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AdminService } from '../services/admin.service';

// Permite entrar solo con sesión de personal interno; con roles opcionales (FA04 de CU-13).
export const personalGuard = (...roles: string[]): CanActivateFn => () => {
  const admin = inject(AdminService);
  const router = inject(Router);

  return admin.sesion().pipe(
    map(personal => {
      if (roles.length === 0 || roles.includes(personal.rol.nombre)) {
        localStorage.setItem('rolPersonal', personal.rol.nombre);
        return true;
      }
      alert('No posee permisos suficientes para realizar la acción');
      return router.createUrlTree(['/gestionar-casos']);
    }),
    catchError(() => of(router.createUrlTree(['/login'])))
  );
};
