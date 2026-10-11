import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../core/api-config';
import { Usuario } from '../core/models/usuario';

export interface RegistroRequest {
  nombreCompleto: string;
  fechaNacimiento: string;
  nacionalidad: string;
  correo: string;
  codigoArea: string;
  telefono: string;
  direccion: string;
  password: string;
}

export interface ActualizarPerfilRequest {
  nombreCompleto: string;
  fechaNacimiento: string;
  nacionalidad: string;
  correo: string;
  codigoArea: string;
  telefono: string;
  direccion: string;
  passwordActual?: string;
  passwordNueva?: string;
}

export interface LoginResponse {
  id: number;
  nombreCompleto: string;
  correo: string;
  rol: string; // 'CLIENTE', 'OPERADOR', 'GERENTE', 'ADMINISTRADOR'
  tipoUsuario: string; // 'CLIENTE' | 'PERSONAL'
}

export interface RecuperarSolicitarResponse {
  mensaje: string;
}

export interface LogoutResponse {
  message: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly STORAGE_USER = 'currentUser';
  private readonly STORAGE_ROL = 'userRole';

  constructor(private http: HttpClient) {}

  // CU-01
  registrar(datos: RegistroRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_BASE_URL}/auth/registro`, datos, { withCredentials: true });
  }

  // CU-01, FA01.1
  correoDisponible(correo: string): Observable<{ disponible: boolean }> {
    return this.http.get<{ disponible: boolean }>(`${API_BASE_URL}/auth/correo-disponible`, {
      params: { correo }
    });
  }

  // CU-00: Iniciar sesión y guardar datos locales
  login(correo: string, password: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(
      `${API_BASE_URL}/auth/login`,
      { correo, password },
      { withCredentials: true }
    ).pipe(
      tap((res) => {
        localStorage.setItem(this.STORAGE_USER, JSON.stringify(res));
        localStorage.setItem(this.STORAGE_ROL, res.rol);
      })
    );
  }

  // CU-00: Flujo 2 y FA06 - Cerrar sesión y limpiar sesión local
  logout(): Observable<LogoutResponse> {
    return this.http.post<LogoutResponse>(
      `${API_BASE_URL}/auth/logout`,
      {},
      { withCredentials: true }
    ).pipe(
      tap({
        next: () => this.limpiarSesionLocal(),
        error: () => this.limpiarSesionLocal() // FA06: Si la sesión ya expiró, limpia y permite salir
      })
    );
  }

  limpiarSesionLocal(): void {
    localStorage.removeItem(this.STORAGE_USER);
    localStorage.removeItem(this.STORAGE_ROL);
  }

  getUsuarioActual(): LoginResponse | null {
    const raw = localStorage.getItem(this.STORAGE_USER);
    return raw ? JSON.parse(raw) : null;
  }

  getRol(): string | null {
    return localStorage.getItem(this.STORAGE_ROL);
  }

  estaAutenticado(): boolean {
    return !!localStorage.getItem(this.STORAGE_USER);
  }

  // Sesión activa / carga de perfil (CU-03)
  obtenerSesion(): Observable<Usuario> {
    return this.http.get<Usuario>(`${API_BASE_URL}/auth/me`, { withCredentials: true });
  }

  // CU-03
  actualizarPerfil(datos: ActualizarPerfilRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${API_BASE_URL}/auth/perfil`, datos, { withCredentials: true });
  }

  // CU-02 paso 1
  recuperarSolicitar(correo: string): Observable<RecuperarSolicitarResponse> {
    return this.http.post<RecuperarSolicitarResponse>(
      `${API_BASE_URL}/auth/recuperar/solicitar`,
      { correo },
      { withCredentials: true }
    );
  }

  // CU-02 paso 2
  recuperarConfirmar(correo: string, codigo: string, passwordNueva: string): Observable<void> {
    return this.http.post<void>(
      `${API_BASE_URL}/auth/recuperar/confirmar`,
      { correo, codigo, passwordNueva },
      { withCredentials: true }
    );
  }
}