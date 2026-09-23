import { Injectable } from '@angular/core';
import { RegistroAuditoriaNotificacion, TipoEventoNotificacion, RolDestinatario } from '../models/notificacion.model';

@Injectable({
  providedIn: 'root'
})
export class NotificacionesService {
  private historialNotificaciones: RegistroAuditoriaNotificacion[] = [];

  enviarNotificacionAutomatica(params: {
    codigoCaso: string;
    evento: TipoEventoNotificacion;
    emailDestino?: string;
    destinatarioRol: RolDestinatario;
    detalles: {
      estadoNuevo?: string;
      responsableNuevo?: string;
      tituloRespuesta?: string;
    };
  }): RegistroAuditoriaNotificacion {
    const fechaHora = new Date().toLocaleString('es-GT', { hour12: false });
    const idNotif = 'NOTIF-' + Math.floor(100000 + Math.random() * 900000);

    // FA02: Datos de contacto no disponibles
    if (!params.emailDestino || !params.emailDestino.includes('@')) {
      const falloContacto: RegistroAuditoriaNotificacion = {
        id: idNotif,
        codigoCaso: params.codigoCaso,
        destinatarioEmail: 'No disponible',
        destinatarioRol: params.destinatarioRol,
        asunto: `Alerta: Contacto no disponible para caso ${params.codigoCaso}`,
        cuerpo: `No se pudo enviar notificación del evento ${params.evento} debido a falta de correo válido.`,
        fechaHora,
        estadoEnvio: 'Fallido',
        reintentos: 0
      };
      this.historialNotificaciones.unshift(falloContacto);
      return falloContacto;
    }

    let asunto = '';
    let cuerpo = '';

    switch (params.evento) {
      case 'CAMBIO_ESTADO':
        asunto = `Actualización de Estado - Caso ${params.codigoCaso}`;
        cuerpo = `Estimado(a), le informamos que su caso ${params.codigoCaso} ha cambiado de estado a "${params.detalles.estadoNuevo}". Restaurante Las Delicias está dando seguimiento continuo.`;
        break;

      case 'RESPUESTA_OFICIAL':
        asunto = `Respuesta Oficial Emitida - Caso ${params.codigoCaso}`;
        cuerpo = `Estimado(a), se ha emitido una resolución formal sobre su caso ${params.codigoCaso}: "${params.detalles.tituloRespuesta}". Puede ingresar al portal para revisar los detalles y compensaciones.`;
        break;

      case 'REASIGNACION':
        asunto = `Asignación de Responsabilidad - Caso ${params.codigoCaso}`;
        cuerpo = `Aviso interno: Se le ha asignado la gestión del caso ${params.codigoCaso} a ${params.detalles.responsableNuevo} para su pronta atención y resolución.`;
        break;

      default:
        asunto = `Notificación de Caso ${params.codigoCaso}`;
        cuerpo = `Se ha generado una actualización sobre su caso registrado.`;
        break;
    }

    const nuevaNotificacion: RegistroAuditoriaNotificacion = {
      id: idNotif,
      codigoCaso: params.codigoCaso,
      destinatarioEmail: params.emailDestino,
      destinatarioRol: params.destinatarioRol,
      asunto,
      cuerpo,
      fechaHora,
      estadoEnvio: 'Enviado',
      reintentos: 0
    };

    this.historialNotificaciones.unshift(nuevaNotificacion);
    return nuevaNotificacion;
  }

  obtenerHistorial(): RegistroAuditoriaNotificacion[] {
    return this.historialNotificaciones;
  }
}