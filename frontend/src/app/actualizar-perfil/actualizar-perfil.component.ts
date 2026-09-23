import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AuthService } from '../services/auth.service';

// Mismas reglas que CU-01 (Registrarse).
const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PATRON_SOLO_LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$/;

@Component({
  selector: 'app-actualizar-perfil',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, NavbarComponent],
  templateUrl: './actualizar-perfil.component.html',
  styleUrls: ['./actualizar-perfil.component.scss']
})
export class ActualizarPerfilComponent implements OnInit {
  perfilForm!: FormGroup;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeExito: string = '';
  mensajeInfo: string = '';
  cargando: boolean = true;

  constructor(private fb: FormBuilder, private authService: AuthService) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarPerfil();
  }

  inicializarFormulario(): void {
    this.perfilForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_SOLO_LETRAS)]],
      fechaNacimiento: ['', [Validators.required, this.validarFechaDDMMAAAA]],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_CORREO)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      // FA01: la contraseña actual solo es obligatoria si se quiere cambiar la contraseña
      passwordActual: [''],
      passwordNueva: ['']
    }, { validators: this.validarCambioPassword });
  }

  // Valida el formato dd/mm/aaaa escrito a mano y que no sea una fecha futura (igual que CU-01).
  validarFechaDDMMAAAA(control: AbstractControl): ValidationErrors | null {
    const valor = control.value;
    if (!valor) {
      return null;
    }

    const coincide = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(valor);
    if (!coincide) {
      return { formatoInvalido: true };
    }

    const [, diaStr, mesStr, anioStr] = coincide;
    const dia = Number(diaStr);
    const mes = Number(mesStr);
    const anio = Number(anioStr);
    const fecha = new Date(anio, mes - 1, dia);

    const esFechaReal = fecha.getFullYear() === anio && fecha.getMonth() === mes - 1 && fecha.getDate() === dia;
    if (!esFechaReal) {
      return { formatoInvalido: true };
    }

    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);
    if (fecha > hoy) {
      return { fechaFutura: true };
    }

    return null;
  }

  // Inserta las diagonales automáticamente mientras el usuario escribe dd/mm/aaaa.
  formatearFecha(event: Event): void {
    const input = event.target as HTMLInputElement;
    let soloDigitos = input.value.replace(/\D/g, '').slice(0, 8);

    let formateado = soloDigitos;
    if (soloDigitos.length > 4) {
      formateado = `${soloDigitos.slice(0, 2)}/${soloDigitos.slice(2, 4)}/${soloDigitos.slice(4)}`;
    } else if (soloDigitos.length > 2) {
      formateado = `${soloDigitos.slice(0, 2)}/${soloDigitos.slice(2)}`;
    }

    // Se deja que Angular actualice el input desde el FormControl.
    this.perfilForm.get('fechaNacimiento')?.setValue(formateado);
  }

  // Convierte aaaa-mm-dd (lo que manda el backend) a dd/mm/aaaa (lo que muestra el formulario).
  private convertirFechaADDMMAAAA(fechaISO: string): string {
    const [anio, mes, dia] = fechaISO.split('-');
    return `${dia}/${mes}/${anio}`;
  }

  // Convierte dd/mm/aaaa a aaaa-mm-dd (formato que espera el backend).
  private convertirFechaAISO(fechaDDMMAAAA: string): string {
    const [dia, mes, anio] = fechaDDMMAAAA.split('/');
    return `${anio}-${mes}-${dia}`;
  }

  // Filtra en tiempo real para que solo se puedan escribir números (código de área / teléfono).
  soloNumeros(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '');
    this.perfilForm.get(controlName)?.setValue(limpio);
  }

  // Filtra en tiempo real para que el nombre completo no acepte números.
  soloTexto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]/g, '');
    this.perfilForm.get('nombreCompleto')?.setValue(limpio);
  }

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

  // FA01: si se ingresa contraseña nueva, la actual es obligatoria
  validarCambioPassword(control: AbstractControl): ValidationErrors | null {
    const nueva = control.get('passwordNueva')?.value;
    const actual = control.get('passwordActual')?.value;
    return nueva && !actual ? { faltaPasswordActual: true } : null;
  }

  // Paso 4: Botón "Actualizar Datos"
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    // FA01/FA03: Validación de campos obligatorios
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      this.mensajeError = this.perfilForm.hasError('faltaPasswordActual')
        ? 'Debe ingresar su contraseña actual para poder cambiarla'
        : 'Debe completar todos los campos obligatorios con el formato correcto';
      return;
    }

    // Paso 5: Solicita confirmación
    this.mostrarModalConfirmacion = true;
  }

  // Pasos 6, 7 / FA02: Confirmación o cancelación
  confirmarActualizacion(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      // FA02: Cancelación de la actualización
      this.mensajeInfo = 'Se ha cancelado la actualización del perfil';
      return;
    }

    const { nombreCompleto, fechaNacimiento, nacionalidad, correoElectronico, codigoArea, telefono, direccion, passwordActual, passwordNueva } = this.perfilForm.value;

    this.authService.actualizarPerfil({
      nombreCompleto,
      fechaNacimiento: this.convertirFechaAISO(fechaNacimiento),
      nacionalidad,
      correo: correoElectronico, codigoArea, telefono, direccion,
      passwordActual: passwordActual || undefined,
      passwordNueva: passwordNueva || undefined
    }).subscribe({
      next: () => {
        // Paso 7: Actualización exitosa
        this.mensajeExito = 'Datos actualizados correctamente';
        this.perfilForm.patchValue({ passwordActual: '', passwordNueva: '' });
      },
      error: (err) => {
        // FA02 (correo en uso) / contraseña actual incorrecta
        this.mensajeError = err.error?.message ?? 'No se pudo actualizar el perfil';
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}
