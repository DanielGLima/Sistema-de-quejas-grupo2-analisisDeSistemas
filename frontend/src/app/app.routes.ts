import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegistroComponent } from './registro/registro.component';
import { RecuperarPasswordComponent } from './recuperar-password/recuperar-password.component';
import { ActualizarPerfilComponent } from './actualizar-perfil/actualizar-perfil.component';
import { RegistrarCasoComponent } from './registrar-caso/registrar-caso.component';
import { ConsultarCasosComponent } from './consultar-casos/consultar-casos.component';

export const routes: Routes = [
  // Redirección inicial
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  // Módulos del Cliente (CU-00 a CU-06)
  { path: 'login', component: LoginComponent },
  { path: 'registro', component: RegistroComponent },
  { path: 'recuperar-password', component: RecuperarPasswordComponent },
  { path: 'actualizar-perfil', component: ActualizarPerfilComponent },
  { path: 'registrar-caso', component: RegistrarCasoComponent },
  { path: 'consultar-casos', component: ConsultarCasosComponent },

  // Ruta comodín (por si se ingresa una URL inexistente)
  { path: '**', redirectTo: 'login' }
];