import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AuthService } from '../services/auth.service';
import { NACIONALIDADES } from '../core/nacionalidades';

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PATRON_SOLO_LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$/;
// Misma complejidad: al menos una mayúscula, un número y un carácter especial (6 a 20 caracteres)
const PATRON_PASSWORD = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$\%^&*(),.?":{}\vert{}<>_\-]).+$/;

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

  // Nombres exactos de la tabla de campos (CU-01 Flujo 3, Paso 3) para FA21
  private etiquetasCampos: Record<string, string> = {
    nombreCompleto: 'nombre completo',
    fechaNacimiento: 'fecha de nacimiento',
    nacionalidad: 'nacionalidad',
    correoElectronico: 'correo electrónico',
    codigoArea: 'código de área telefónico',
    telefono: 'número de teléfono',
    direccion: 'dirección',
    passwordActual: 'contraseña actual',
    passwordNueva: 'nueva contraseña'
  };

  constructor(private fb: FormBuilder, private authService: AuthService) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarPerfil();
  }

  // Paso 3: Definición del formulario con validaciones reactivas según especificación
  inicializarFormulario(): void {
    this.perfilForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_SOLO_LETRAS)]],
      fechaNacimiento: ['', [Validators.required, this.validarFechaDDMMAAAA]],
      nacionalidad: ['', [Validators.required, Validators.maxLength(50)]],
      correoElectronico: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_CORREO)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      passwordActual: ['', [Validators.maxLength(20)]],
      passwordNueva: ['', [Validators.maxLength(20)]]
    }, { validators: this.validarCambioPassword });
  }

  // Valida el formato DD/MM/AAAA y restringe fechas futuras
  validarFechaDDMMAAAA(control: AbstractControl): ValidationErrors | null {
    const valor = control.value;
    if (!valor) return null;

    const coincide = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor);
    if (!coincide) return { formatoInvalido: true };

    const [, diaStr, mesStr, anioStr] = coincide;
    const dia = Number(diaStr);
    const mes = Number(mesStr);
    const anio = Number(anioStr);
    const fecha = new Date(anio, mes - 1, dia);

    const esFechaReal = fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
    if (!esFechaReal) return { formatoInvalido: true };

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    if (fecha > hoy) return { fechaFutura: true };

    return null;
  }

  formatearFecha(event: Event): void {
    const input = event.target as HTMLInputElement;
    let soloDigitos = input.value.replace(/\D/g, '').slice(0, 8);

    let formateado = soloDigitos;
    if (soloDigitos.length > 4) {
      formateado = `${soloDigitos.slice(0, 2)}/${soloDigitos.slice(2, 4)}/${soloDigitos.slice(4)}`;
    } else if (soloDigitos.length > 2) {
      formateado = `${soloDigitos.slice(0, 2)}/${soloDigitos.slice(2)}`;
    }

    this.perfilForm.get('fechaNacimiento')?.setValue(formateado);
  }

  private convertirFechaADDMMAAAA(fechaISO: string): string {
    if (!fechaISO) return '';
    const [anio, mes, dia] = fechaISO.split('-');
    return `${dia}/${mes}/${anio}`;
  }

  private convertirFechaAISO(fechaDDMMAAAA: string): string {
    const [dia, mes, anio] = fechaDDMMAAAA.split('/');
    return `${anio}-${mes}-${dia}`;
  }

  soloNumeros(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '');
    this.perfilForm.get(controlName)?.setValue(limpio);
  }

  soloTexto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]/g, '');
    this.perfilForm.get('nombreCompleto')?.setValue(limpio);
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

  // FA19: Exigir contraseña actual si se provee una nueva y validar requisitos (6 a 20 caracteres y patrón)
  validarCambioPassword(control: AbstractControl): ValidationErrors | null {
    const actual = control.get('passwordActual')?.value?.trim();
    const nueva = control.get('passwordNueva')?.value?.trim();

    if (nueva && !actual) {
      return { faltaPasswordActual: true };
    }
    if (actual && !nueva) {
      return { faltaPasswordNueva: true };
    }
    if (nueva) {
      if (nueva.length < 6 || nueva.length > 20 || !PATRON_PASSWORD.test(nueva)) {
        return { formatoPasswordNueva: true };
      }
    }
    return null;
  }

  // Paso 4: Botón "Guardar cambios"
  guardarCambios(): void {
    this.limpiarMensajes();

    // Paso 5: El sistema valida los campos ingresados
    // FA21: Formato de datos no válido. Revise el campo: <nombre del campo con error>
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();

      const campoConError = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion']
        .find(c => this.perfilForm.get(c)?.invalid);

      if (campoConError) {
        this.mensajeError = `Formato de datos no válido. Revise el campo: ${this.etiquetasCampos[campoConError]}`;
        return;
      }

      // FA19: Contraseña actual no ingresada para autorizar el cambio
      if (this.perfilForm.hasError('faltaPasswordActual')) {
        this.mensajeError = 'Contraseña actual incorrecta';
        return;
      }

      // FA21: Validación de formato sobre el campo de nueva contraseña
      if (this.perfilForm.hasError('formatoPasswordNueva')) {
        this.mensajeError = `Formato de datos no válido. Revise el campo: ${this.etiquetasCampos['passwordNueva']}`;
        return;
      }

      this.mensajeError = 'Formato de datos no válido';
      return;
    }

    this.guardando = true;
    const { nombreCompleto, fechaNacimiento, nacionalidad, correoElectronico, codigoArea, telefono, direccion, passwordActual, passwordNueva } = this.perfilForm.value;

    // Paso 6 y 7: Actualización de datos en el sistema y auditoría interna
    this.authService.actualizarPerfil({
      nombreCompleto: nombreCompleto.trim(),
      fechaNacimiento: this.convertirFechaAISO(fechaNacimiento),
      nacionalidad,
      correo: correoElectronico.trim(),
      codigoArea: String(codigoArea).trim(),
      telefono: String(telefono).trim(),
      direccion: direccion.trim(),
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
        const msg = err.error?.message || '';

        // FA19: Contraseña actual incorrecta (rechazo por backend)
        if (err.status === 400 && msg.toLowerCase().includes('actual')) {
          this.mensajeError = 'Contraseña actual incorrecta';
        }
        // FA20: Correo electrónico ya registrado en otra cuenta
        else if (err.status === 409 || msg.toLowerCase().includes('correo')) {
          this.mensajeError = 'El correo electrónico ya se encuentra registrado';
        } else {
          this.mensajeError = msg || 'No se pudo actualizar el perfil';
        }
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }
}