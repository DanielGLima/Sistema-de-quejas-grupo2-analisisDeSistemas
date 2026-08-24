export interface Caso {
  idCaso: number;
  identificadorVisible: string;
  descripcion: string;
  numeroFactura?: string;
  nombreEmpleadoInvolucrado?: string;
  esAnonimo: boolean;
  fechaCreacion: string;
  fechaActualizacion: string;
  sucursal: { idSucursal: number; nombre: string };
  tipoCaso: { idTipoCaso: number; nombre: string };
  categoriaCaso?: { idCategoria: number; nombre: string };
  estadoCaso: { idEstado: number; nombre: string };
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
