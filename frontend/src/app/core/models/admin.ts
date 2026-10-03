export type RolPersonal = 'Administrador General' | 'Gerente' | 'Operador';

export interface Personal {
  idPersonal: number;
  nombreCompleto: string;
  correo: string;
  rol: { idRol: number; nombre: RolPersonal };
  sucursal?: { idSucursal: number; nombreSucursal: string };
  activo: boolean;
  fechaCreacion: string;
}

export interface RespuestaApi {
  idRespuesta: number;
  titulo: string;
  contenido: string;
  accionesSeguimiento?: string;
  estadoAprobacion: string;
  fechaRespuesta: string;
  personal: { idPersonal: number; nombreCompleto: string };
}

export interface ReasignacionApi {
  idReasignacion: number;
  personalAnterior?: { nombreCompleto: string };
  personalNuevo: { nombreCompleto: string };
  personalEjecutor: { nombreCompleto: string };
  motivo: string;
  fechaReasignacion: string;
}

export interface CasoAdminApi {
  idCaso: number;
  identificadorVisible: string;
  tipoCaso: string;
  categoria?: string;
  idSucursal: number;
  sucursal: string;
  estado: string;
  descripcion: string;
  fechaCreacion: string;
  fechaActualizacion: string;
  esAnonimo: boolean;
  nombreCliente?: string;
  correoCliente?: string;
  idPersonalAsignado?: number;
  personalAsignado?: string;
  motivoReapertura?: string;
  estadosPermitidos: string[];
  respuestas: RespuestaApi[];
  reasignaciones: ReasignacionApi[];
}

export interface FiltrosCasos {
  fechaInicial?: string;
  fechaFinal?: string;
  idTipoCaso?: number | null;
  idCategoria?: number | null;
  idSucursal?: number | null;
  idEstado?: number | null;
  identificador?: string;
  correoCliente?: string;
  idResponsable?: number | null;
  texto?: string;
  ordenarPor?: string;
  direccion?: 'asc' | 'desc';
}

export interface BitacoraApi {
  idBitacora: number;
  usuarioId: string;
  rolUsuario: string;
  tipoAccion: string;
  moduloAfectado: string;
  direccionIp: string;
  fechaHoraExacta: string;
  identificadorCaso?: string;
  detalle?: string;
}

export interface NotificacionApi {
  idNotificacion: number;
  caso?: { identificadorVisible: string };
  usuario?: { correo: string };
  personal?: { correo: string };
  correoDestino: string;
  tipoEvento: string;
  asunto: string;
  contenido: string;
  estadoEnvio: string;
  reintentos: number;
  fechaEnvio: string;
}

export interface EstadoApi {
  idEstado: number;
  nombre: string;
  orden: number;
  activo: boolean;
}

export interface RolApi {
  idRol: number;
  nombre: string;
}

export interface CatalogoRequest {
  nombre?: string;
  direccion?: string;
  telefono?: string;
  codigo?: string;
  activo?: boolean;
}

export interface PersonalRequest {
  nombreCompleto: string;
  correo: string;
  idRol: number;
  idSucursal: number;
  activo: boolean;
  passwordTemporal?: string;
}
