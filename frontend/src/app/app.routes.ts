import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegistroComponent } from './registro/registro.component';
import { RecuperarPasswordComponent } from './recuperar-password/recuperar-password.component';
import { ActualizarPerfilComponent } from './actualizar-perfil/actualizar-perfil.component';
import { RegistrarCasoComponent } from './registrar-caso/registrar-caso.component';
import { ConsultarCasosComponent } from './consultar-casos/consultar-casos.component';
import { personalGuard } from './core/personal.guard';

export const routes: Routes = [
  // Redirección inicial
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Módulos del Cliente (CU-00 a CU-09)
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'recuperar-password', component: RecuperarPasswordComponent },
  { path: 'actualizar-perfil', component: ActualizarPerfilComponent },
  { path: 'registrar-caso', component: RegistrarCasoComponent },
  { path: 'consultar-casos', component: ConsultarCasosComponent },

  // Panel del personal interno (CU-10, CU-11, CU-12, CU-14, CU-15)
  {
    path: 'gestionar-casos',
    canActivate: [personalGuard()],
    loadComponent: () => import('./gestionar-casos/gestionar-casos').then(m => m.GestionarCasosComponent)
  },

  // Administración de usuarios internos y catálogos (CU-13): solo Administrador General
  {
    path: 'administrar',
    canActivate: [personalGuard('Administrador General')],
    loadComponent: () => import('./administrar/administrar').then(m => m.AdministrarComponent)
  },

  // Ruta comodín (por si se ingresa una URL inexistente)
  { path: '**', redirectTo: 'login' }
];
