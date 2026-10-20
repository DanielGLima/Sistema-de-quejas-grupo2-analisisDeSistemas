import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, RegistroRequest } from '../services/auth.service';
import { NACIONALIDADES } from '../core/nacionalidades';

// Este componente solo captura los datos: todas las validaciones (campos obligatorios, formatos,
// correo ya registrado y contraseña) las realiza el backend y aquí solo se muestra su mensaje.
@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.scss']
})
export class RegistroComponent implements OnInit {
  registroForm!: FormGroup;
  mensajeError: string = '';
  mensajeExito: string = '';
  mensajeInfo: string = '';

  listaNacionalidades: string[] = NACIONALIDADES;

  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;
  verificandoCorreo: boolean = false;
  guardando: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: [''],
      fechaNacimiento: [''],
      nacionalidad: [''],
      correoElectronico: [''],
      codigoArea: [''],
      telefono: [''],
      direccion: [''],
      password: [''],
      confirmarPassword: ['']
    });
  }

  // Convierte DD/MM/AAAA al formato que recibe el backend; si no tiene tres partes se envía vacía.
  private convertirFechaAISO(fechaDDMMAAAA: string | null): string | null {
    const partes = (fechaDDMMAAAA ?? '').trim().split('/');
    if (partes.length !== 3) return null;
    const [dia, mes, anio] = partes;
    return `${anio}-${mes}-${dia}`;
  }

  private datosRegistro(): RegistroRequest {
    const v = this.registroForm.value;
    return {
      nombreCompleto: (v.nombreCompleto ?? '').trim(),
      fechaNacimiento: this.convertirFechaAISO(v.fechaNacimiento),
      nacionalidad: v.nacionalidad,
      correo: (v.correoElectronico ?? '').trim(),
      codigoArea: String(v.codigoArea ?? '').trim(),
      telefono: String(v.telefono ?? '').trim(),
      direccion: (v.direccion ?? '').trim(),
      password: v.password ?? '',
      confirmacionPassword: v.confirmarPassword ?? ''
    };
  }

  // Paso 4: el backend valida los datos personales (FA01) y que el correo no esté registrado (FA01.1)
  habilitarPassword(): void {
    this.limpiarMensajes();
    this.verificandoCorreo = true;

    this.authService.validarDatosRegistro(this.datosRegistro()).subscribe({
      next: () => {
        this.verificandoCorreo = false;
        // Paso 5: Mostrar campos de contraseña
        this.pasoPassword = true;
      },
      error: (err) => {
        this.verificandoCorreo = false;
        this.mensajeError = err.error?.message || 'No se pudo validar los datos, intente nuevamente';
      }
    });
  }

  // Paso 6: el backend valida la contraseña y su confirmación (FA03, FA04 y FA05)
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    this.authService.validarPasswordRegistro(this.datosRegistro()).subscribe({
      next: () => {
        // Si todo está correcto, se muestra el modal de confirmación
        this.mostrarModalConfirmacion = true;
      },
      error: (err) => {
        this.mensajeError = err.error?.message || 'No se pudo validar la contraseña, intente nuevamente';
      }
    });
  }

  // Paso 7 y 8: Confirmación Sí/No
  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    // FA06: El usuario cancela la confirmación seleccionando No
    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado el registro satisfactoriamente';
      return;
    }

    this.guardando = true;

    this.authService.registrar(this.datosRegistro()).subscribe({
      next: () => {
        this.guardando = false;
        // Paso 9 y 10: Mensaje de éxito y redirección automática al inicio de sesión
        this.mensajeExito = 'Registro completado exitosamente';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1800);
      },
      error: (err) => {
        this.guardando = false;
        this.mensajeError = err.error?.message || 'Debe completar todos los datos personales obligatorios antes de continuar';
      }
    });
  }

  // FA07: Descartar datos y regresar al inicio de sesión
  volverAlInicio(): void {
    this.registroForm.reset();
    this.router.navigate(['/login']);
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}
