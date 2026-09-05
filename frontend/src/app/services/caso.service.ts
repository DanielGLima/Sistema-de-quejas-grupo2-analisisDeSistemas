import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../core/api-config';
import { Caso, EvaluacionCasoApi, NuevoCasoRequest } from '../core/models/caso';

@Injectable({ providedIn: 'root' })
export class CasoService {

  constructor(private http: HttpClient) {}

  // CU-05
  crear(datos: NuevoCasoRequest): Observable<Caso> {
    const formData = new FormData();
    formData.append('idTipoCaso', String(datos.idTipoCaso));
    formData.append('idSucursal', String(datos.idSucursal));
    if (datos.idCategoria) {
      formData.append('idCategoria', String(datos.idCategoria));
    }
    formData.append('descripcion', datos.descripcion);
    if (datos.numeroFactura) {
      formData.append('numeroFactura', datos.numeroFactura);
    }
    if (datos.nombreEmpleadoInvolucrado) {
      formData.append('nombreEmpleadoInvolucrado', datos.nombreEmpleadoInvolucrado);
    }
    formData.append('esAnonimo', String(datos.esAnonimo));
    (datos.archivos ?? []).forEach(archivo => formData.append('archivos', archivo));

    return this.http.post<Caso>(`${API_BASE_URL}/casos`, formData, { withCredentials: true });
  }

  // CU-06
  misCasos(): Observable<Caso[]> {
    return this.http.get<Caso[]>(`${API_BASE_URL}/casos/mios`, { withCredentials: true });
  }

  // CU-07
  cancelar(idCaso: number, motivo: string): Observable<Caso> {
    return this.http.put<Caso>(`${API_BASE_URL}/casos/${idCaso}/cancelar`, { motivo }, { withCredentials: true });
  }

  // CU-08
  evaluar(idCaso: number, calificacion: number, comentario: string): Observable<EvaluacionCasoApi> {
    return this.http.post<EvaluacionCasoApi>(`${API_BASE_URL}/casos/${idCaso}/evaluacion`, { calificacion, comentario }, { withCredentials: true });
  }
}
