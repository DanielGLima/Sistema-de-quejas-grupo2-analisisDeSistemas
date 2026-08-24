import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
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

export interface RecuperarSolicitarResponse {
  mensaje: string;
  codigo?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  constructor(private http: HttpClient) {}

  // CU-01
  registrar(datos: RegistroRequest): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_BASE_URL}/auth/registro`, datos, { withCredentials: true });
  }

  // CU-00
  login(correo: string, password: string): Observable<Usuario> {
    return this.http.post<Usuario>(`${API_BASE_URL}/auth/login`, { correo, password }, { withCredentials: true });
  }

  // CU-04
  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/logout`, {}, { withCredentials: true });
  }

  // Sesion activa / carga de perfil (CU-03)
  obtenerSesion(): Observable<Usuario> {
    return this.http.get<Usuario>(`${API_BASE_URL}/auth/me`, { withCredentials: true });
  }

  // CU-03
  actualizarPerfil(datos: ActualizarPerfilRequest): Observable<Usuario> {
    return this.http.put<Usuario>(`${API_BASE_URL}/auth/perfil`, datos, { withCredentials: true });
  }

  // CU-02 paso 1
  recuperarSolicitar(correo: string): Observable<RecuperarSolicitarResponse> {
    return this.http.post<RecuperarSolicitarResponse>(`${API_BASE_URL}/auth/recuperar/solicitar`, { correo }, { withCredentials: true });
  }

  // CU-02 paso 2
  recuperarConfirmar(correo: string, codigo: string, passwordNueva: string): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/recuperar/confirmar`, { correo, codigo, passwordNueva }, { withCredentials: true });
  }
}
