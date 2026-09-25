export interface RegistroAuditoria {
  id: string;
  usuarioId: string;           // Alfanumérico, 50 caracteres
  rolUsuario: string;          // Texto alfabético, 30 caracteres
  tipoAccion: string;          // Operación ejecutada, 50 caracteres
  moduloAfectado: string;      // Módulo del sistema, 40 caracteres
  direccionIp: string;         // IP y origen, 45 caracteres
  fechaHoraExacta: string;     // YYYY-MM-DD HH:MM:SS.mmm, 23 caracteres
  valoresAnteriores: string;   // Formato JSON/Texto
  valoresNuevos: string;       // Formato JSON/Texto
}