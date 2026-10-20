import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AuthService } from '../services/auth.service';
import { NACIONALIDADES } from '../core/nacionalidades';

@Component({
  selector: 'app-actualizar-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './actualizar-perfil.component.html',
  styleUrls: ['./actualizar-perfil.component.scss']
})
export class ActualizarPerfilComponent implements OnInit {
  perfilForm!: FormGroup;
  mensajeError: string = '';
  mensajeExito: string = '';
  cargando: boolean = true;
  guardando: boolean = false;
  listaNacionalidades: string[] = NACIONALIDADES;

  constructor(private fb: FormBuilder, private authService: AuthService) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarPerfil();
  }

  // Paso 3: Definición del formulario con validaciones reactivas según especificación
  inicializarFormulario(): void {
    this.perfilForm = this.fb.group({
      nombreCompleto: [''],
      fechaNacimiento: [''],
      nacionalidad: [''],
      correoElectronico: [''],
      codigoArea: [''],
      telefono: [''],
      direccion: [''],
      passwordActual: [''],
      passwordNueva: ['']
    });
  }

  private convertirFechaADDMMAAAA(fechaISO: string): string {
    if (!fechaISO) return '';
    const [anio, mes, dia] = fechaISO.split('-');
    return `${dia}/${mes}/${anio}`;
  }

  // Convierte DD/MM/AAAA al formato que recibe el backend; si no tiene tres partes se envía vacía.
  private convertirFechaAISO(fechaDDMMAAAA: string | null): string | null {
    const partes = (fechaDDMMAAAA ?? '').trim().split('/');
    if (partes.length !== 3) return null;
    const [dia, mes, anio] = partes;
    return `${anio}-${mes}-${dia}`;
  }

  // Paso 3 y Paso 9: Cargar o recargar los datos actuales del perfil autenticado
  cargarPerfil(): void {
    this.authService.obtenerSesion().subscribe({
      next: (usuario) => {
        this.perfilForm.patchValue({
          nombreCompleto: usuario.nombreCompleto,
          fechaNacimiento: this.convertirFechaADDMMAAAA(usuario.fechaNacimiento),
          nacionalidad: usuario.nacionalidad,
          correoElectronico: usuario.correo,
          codigoArea: usuario.codigoArea,
          telefono: usuario.telefono,
          direccion: usuario.direccion
        });
        this.cargando = false;
      },
      error: () => {
        this.mensajeError = 'Debe iniciar sesión para ver su perfil';
        this.cargando = false;
      }
    });
  }

  // Paso 4: Botón "Guardar cambios"
  guardarCambios(): void {
    this.limpiarMensajes();

    this.guardando = true;
    const { nombreCompleto, fechaNacimiento, nacionalidad, correoElectronico, codigoArea, telefono, direccion, passwordActual, passwordNueva } = this.perfilForm.value;

    // Paso 6 y 7: Actualización de datos en el sistema y auditoría interna
    this.authService.actualizarPerfil({
      nombreCompleto: (nombreCompleto ?? '').trim(),
      fechaNacimiento: this.convertirFechaAISO(fechaNacimiento),
      nacionalidad,
      correo: (correoElectronico ?? '').trim(),
      codigoArea: String(codigoArea ?? '').trim(),
      telefono: String(telefono ?? '').trim(),
      direccion: (direccion ?? '').trim(),
      passwordActual: passwordActual ? passwordActual.trim() : undefined,
      passwordNueva: passwordNueva ? passwordNueva.trim() : undefined
    }).subscribe({
      next: () => {
        this.guardando = false;
        // Paso 8: Mensaje de éxito
        this.mensajeExito = 'Datos actualizados correctamente';
        this.perfilForm.patchValue({ passwordActual: '', passwordNueva: '' });
        this.perfilForm.markAsPristine();

        // Paso 9: Recarga de información actualizada
        this.cargarPerfil();
      },
      error: (err) => {
        this.guardando = false;
        // FA19 a FA21: el backend indica el motivo (contraseña actual, correo repetido o campo con error)
        this.mensajeError = err.error?.message || 'No se pudo actualizar el perfil';
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}