export interface EvaluacionCasoApi {
  idEvaluacion: number;
  calificacion: number;
  comentario?: string;
  fechaEvaluacion: string;
}

export interface Caso {
  idCaso: number;
  identificadorVisible: string;
  descripcion: string;
  numeroFactura?: string;
  nombreEmpleadoInvolucrado?: string;
  esAnonimo: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
  // Sucursal usa nombreSucursal (no "nombre") porque asi quedo mapeada la entidad en el backend.
  sucursal: { idSucursal: number; nombreSucursal: string };
  tipoCaso: { idTipoCaso: number; nombre: string };
  categoriaCaso?: { idCategoria: number; nombre: string };
  estadoCaso: { idEstado: number; nombre: string };
  evaluacion?: EvaluacionCasoApi;
}

export interface NuevoCasoRequest {
  idTipoCaso: number;
  idSucursal: number;
  idCategoria?: number;
  descripcion: string;
  numeroFactura?: string;
  nombreEmpleadoInvolucrado?: string;
  esAnonimo: boolean;
  archivos?: File[];
}
