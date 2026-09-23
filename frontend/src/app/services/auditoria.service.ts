import { Injectable } from '@angular/core';
import { RegistroAuditoria } from '../models/auditoria.model';

@Injectable({
  providedIn: 'root'
})
export class AuditoriaService {
  // Almacenamiento inalterable en memoria (FA01: Manejo de errores no bloqueante)
  private bitacora: RegistroAuditoria[] = [];

  registrarEvento(params: {
    usuarioId: string;
    rolUsuario: string;
    tipoAccion: string;
    moduloAfectado: string;
    valoresAnteriores: any;
    valoresNuevos: any;
  }): void {
    try {
      const ahora = new Date();
      // Formato YYYY-MM-DD HH:MM:SS.mmm (23 caracteres)
      const anio = ahora.getFullYear();
      const mes = String(ahora.getMonth() + 1).padStart(2, '0');
      const dia = String(ahora.getDate()).padStart(2, '0');
      const horas = String(ahora.getHours()).padStart(2, '0');
      const mins = String(ahora.getMinutes()).padStart(2, '0');
      const segs = String(ahora.getSeconds()).padStart(2, '0');
      const millis = String(ahora.getMilliseconds()).padStart(3, '0');

      const fechaHoraExacta = `${anio}-${mes}-${dia} ${horas}:${mins}:${segs}.${millis}`;

      const nuevoRegistro: RegistroAuditoria = {
        id: 'AUD-' + Math.floor(10000000 + Math.random() * 90000000),
        usuarioId: params.usuarioId.substring(0, 50),
        rolUsuario: params.rolUsuario.substring(0, 30),
        tipoAccion: params.tipoAccion.substring(0, 50),
        moduloAfectado: params.moduloAfectado.substring(0, 40),
        direccionIp: '192.168.1.102 (Web Front)',
        fechaHoraExacta,
        valoresAnteriores: JSON.stringify(params.valoresAnteriores),
        valoresNuevos: JSON.stringify(params.valoresNuevos)
      };

      // Conservación cronológica inalterable
      this.bitacora.unshift(Object.freeze({ ...nuevoRegistro }));
    } catch (error) {
      // FA01: Se registra la incidencia pero no se bloquea la operación del usuario
      console.error('Error al registrar en bitácora de auditoría (FA01):', error);
    }
  }

  obtenerBitacora(): readonly RegistroAuditoria[] {
    return this.bitacora;
  }
}