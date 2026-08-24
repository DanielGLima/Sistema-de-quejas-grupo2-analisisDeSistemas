import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api-config';
import { TipoCaso, CategoriaCaso, Sucursal } from '../core/models/catalogos';

@Injectable({ providedIn: 'root' })
export class CatalogoService {

  constructor(private http: HttpClient) {}

  tiposCaso(): Observable<TipoCaso[]> {
    return this.http.get<TipoCaso[]>(`${API_BASE_URL}/tipos-caso`);
  }

  categoriasCaso(): Observable<CategoriaCaso[]> {
    return this.http.get<CategoriaCaso[]>(`${API_BASE_URL}/categorias-caso`);
  }

  sucursales(): Observable<Sucursal[]> {
    return this.http.get<Sucursal[]>(`${API_BASE_URL}/sucursales`);
  }
}
