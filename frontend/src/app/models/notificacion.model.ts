export type TipoEventoNotificacion = 'REGISTRO' | 'CAMBIO_ESTADO' | 'RESPUESTA_OFICIAL' | 'REASIGNACION';

export type RolDestinatario = 'Cliente' | 'Personal Administrativo';

export type EstadoEnvioNotificacion = 'Enviado' | 'Reintentando' | 'Fallido';

export interface ParametrosNotificacion {
  direccionEmail: string;       // Formato email, máx 100 car. (Flujo 2.3 Paso 5)
  asunto: string;               // Texto alfanumérico, máx 100 car.
  codigoCaso: string;           // Alfanumérico, máx 20 car.
  cuerpoMensaje: string;        // Texto alfanumérico, máx 500 car.
  fechaHoraEvento: string;      // Formato DD/MM/AAAA HH:MM:SS, 19 car.
}

export interface RegistroAuditoriaNotificacion {
  id: string;
  codigoCaso: string;
  destinatarioEmail: string;
  destinatarioRol: RolDestinatario;
  asunto: string;
  cuerpo: string;
  fechaHora: string;
  estadoEnvio: EstadoEnvioNotificacion;
  reintentos: number;
}