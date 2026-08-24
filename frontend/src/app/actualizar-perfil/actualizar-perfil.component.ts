import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AuthService } from '../services/auth.service';

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
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100)]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      // FA01: la contraseña actual solo es obligatoria si se quiere cambiar la contraseña
      passwordActual: [''],
      passwordNueva: ['']
    }, { validators: this.validarCambioPassword });
  }

  cargarPerfil(): void {
    this.authService.obtenerSesion().subscribe({
      next: (usuario) => {
        this.perfilForm.patchValue({
          nombreCompleto: usuario.nombreCompleto,
          fechaNacimiento: usuario.fechaNacimiento,
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
      nombreCompleto, fechaNacimiento, nacionalidad,
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
