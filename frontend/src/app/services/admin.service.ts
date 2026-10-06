import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../core/api-config';
import {
  BitacoraApi, CasoAdminApi, CatalogoRequest, EstadoApi, FiltrosCasos, NotificacionApi,
  Personal, PersonalRequest, RolApi
} from '../core/models/admin';
import { CategoriaCaso, Sucursal, TipoCaso } from '../core/models/catalogos';

const ADMIN = `${API_BASE_URL}/admin`;
const OPTS = { withCredentials: true };
const CLAVE_ROL = 'rolPersonal';

// Acceso y operaciones del personal interno (CU-10 a CU-15).
@Injectable({ providedIn: 'root' })
export class AdminService {

  constructor(private http: HttpClient) {}

  // ---- Sesión del personal ----
  login(correo: string, password: string): Observable<Personal> {
    return this.http.post<Personal>(`${ADMIN}/auth/login`, { correo, password }, OPTS)
      .pipe(tap(p => localStorage.setItem(CLAVE_ROL, p.rol.nombre)));
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${ADMIN}/auth/logout`, {}, OPTS);
  }

  sesion(): Observable<Personal> {
    return this.http.get<Personal>(`${ADMIN}/auth/me`, OPTS);
  }

  // Solo ayuda a la interfaz (menú); la seguridad real la aplica el backend.
  rolGuardado(): string | null {
    return localStorage.getItem(CLAVE_ROL);
  }

  // ---- CU-12 / CU-10 / CU-11 ----
  buscarCasos(filtros: FiltrosCasos): Observable<CasoAdminApi[]> {
    let params = new HttpParams();
    Object.entries(filtros).forEach(([clave, valor]) => {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(clave, String(valor));
      }
    });
    return this.http.get<CasoAdminApi[]>(`${ADMIN}/casos`, { ...OPTS, params });
  }

  gestionar(idCaso: number, datos: { idPersonalAsignado?: number | null; nuevoEstado: string; notas?: string; fechaActualizacion: string }): Observable<CasoAdminApi> {
    return this.http.put<CasoAdminApi>(`${ADMIN}/casos/${idCaso}/gestion`, datos, OPTS);
  }

  reasignar(idCaso: number, datos: { idPersonalNuevo: number; motivo: string; fechaActualizacion: string }): Observable<CasoAdminApi> {
    return this.http.put<CasoAdminApi>(`${ADMIN}/casos/${idCaso}/reasignar`, datos, OPTS);
  }

  responder(idCaso: number, datos: { titulo: string; cuerpo: string; accionesSeguimiento?: string }): Observable<CasoAdminApi> {
    return this.http.post<CasoAdminApi>(`${ADMIN}/casos/${idCaso}/respuesta`, datos, OPTS);
  }

  aprobarRespuesta(idRespuesta: number): Observable<CasoAdminApi> {
    return this.http.put<CasoAdminApi>(`${ADMIN}/casos/respuestas/${idRespuesta}/aprobar`, {}, OPTS);
  }

  empleadosAsignables(): Observable<Personal[]> {
    return this.http.get<Personal[]>(`${ADMIN}/personal/asignables`, OPTS);
  }

  // ---- CU-14 / CU-15 ----
  notificaciones(): Observable<NotificacionApi[]> {
    return this.http.get<NotificacionApi[]>(`${ADMIN}/notificaciones`, OPTS);
  }

  bitacora(limite = 200): Observable<BitacoraApi[]> {
    return this.http.get<BitacoraApi[]>(`${ADMIN}/bitacora`, { ...OPTS, params: { limite } });
  }

  // ---- CU-13: usuarios internos ----
  personal(): Observable<Personal[]> {
    return this.http.get<Personal[]>(`${ADMIN}/personal`, OPTS);
  }

  crearPersonal(datos: PersonalRequest): Observable<Personal> {
    return this.http.post<Personal>(`${ADMIN}/personal`, datos, OPTS);
  }

  actualizarPersonal(id: number, datos: PersonalRequest): Observable<Personal> {
    return this.http.put<Personal>(`${ADMIN}/personal/${id}`, datos, OPTS);
  }

  roles(): Observable<RolApi[]> {
    return this.http.get<RolApi[]>(`${ADMIN}/catalogos/roles`, OPTS);
  }

  // ---- CU-13: catálogos ----
  sucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${ADMIN}/catalogos/sucursales`, OPTS);
  }

  tiposCaso(): Observable<TipoCaso[]> {
    return this.http.get<TipoCaso[]>(`${ADMIN}/catalogos/tipos-caso`, OPTS);
  }

  categorias(): Observable<CategoriaCaso[]> {
    return this.http.get<CategoriaCaso[]>(`${ADMIN}/catalogos/categorias`, OPTS);
  }

  estados(): Observable<EstadoApi[]> {
    return this.http.get<EstadoApi[]>(`${ADMIN}/catalogos/estados`, OPTS);
  }

  guardarCatalogo<T>(catalogo: 'sucursales' | 'tipos-caso' | 'categorias' | 'estados', id: number | null,
                     datos: CatalogoRequest, confirmar = false): Observable<T> {
    if (id === null) {
      return this.http.post<T>(`${ADMIN}/catalogos/${catalogo}`, datos, OPTS);
    }
    return this.http.put<T>(`${ADMIN}/catalogos/${catalogo}/${id}`, datos, { ...OPTS, params: { confirmar } });
  }
}
