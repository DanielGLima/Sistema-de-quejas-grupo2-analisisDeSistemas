export interface TipoCaso {
  idTipoCaso: number;
  codigo: string;
  nombre: string;
  activo: boolean;
}

export interface CategoriaCaso {
  idCategoria: number;
  codigo: string;
  nombre: string;
  activo: boolean;
}

export interface Sucursal {
  idSucursal: number;
  nombreSucursal: string;
  direccionSucursal: string;
  telefonoSucursal?: string;
  activo: boolean;
}
