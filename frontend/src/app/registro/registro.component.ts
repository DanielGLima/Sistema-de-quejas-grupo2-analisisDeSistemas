import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NACIONALIDADES } from '../core/nacionalidades';

// Formato de email estándar
const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// Solo texto alfabético y espacios
const PATRON_SOLO_LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$/;
// Al menos una mayúscula, un carácter especial y un número (FA04)
const PATRON_PASSWORD = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$\%^&*(),.?":{}\vert{}<>_\-]).+$/;

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
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_SOLO_LETRAS)]],
      fechaNacimiento: ['', [Validators.required, this.validarFechaDDMMAAAA]],
      nacionalidad: ['', [Validators.required, Validators.maxLength(50)]],
      correoElectronico: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_CORREO)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20), Validators.pattern(PATRON_PASSWORD)]],
      confirmarPassword: ['', Validators.required]
    }, { validators: this.validarPasswordsIguales });
  }

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

    this.registroForm.get('fechaNacimiento')?.setValue(formateado);
  }

  private convertirFechaAISO(fechaDDMMAAAA: string): string {
    const [dia, mes, anio] = fechaDDMMAAAA.split('/');
    return `${anio}-${mes}-${dia}`;
  }

  soloNumeros(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/\D/g, '');
    this.registroForm.get(controlName)?.setValue(limpio);
  }

  soloTexto(event: Event): void {
    const input = event.target as HTMLInputElement;
    const limpio = input.value.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]/g, '');
    this.registroForm.get('nombreCompleto')?.setValue(limpio);
  }

  validarPasswordsIguales(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('password')?.value;
    const confirm = control.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoincide: true };
  }

  // Paso 4: Validar datos personales del Paso 1 antes de mostrar campos de contraseña
  habilitarPassword(): void {
    this.limpiarMensajes();

    const camposPaso1 = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion'];
    let formInvalido = false;

    camposPaso1.forEach(campo => {
      const control = this.registroForm.get(campo);
      if (!control || control.invalid) {
        control?.markAsTouched();
        formInvalido = true;
      }
    });

    // FA01: Campos obligatorios incompletos o inválidos
    if (formInvalido) {
      this.mensajeError = 'Debe completar todos los datos personales obligatorios antes de continuar';
      return;
    }

    // FA02: Verificar correo ya registrado
    this.verificandoCorreo = true;
    const correo = this.registroForm.get('correoElectronico')?.value?.trim();

    this.authService.correoDisponible(correo).subscribe({
      next: ({ disponible }) => {
        this.verificandoCorreo = false;
        if (!disponible) {
          this.mensajeError = 'El correo electrónico ya se encuentra registrado';
          return;
        }
        // Paso 5: Mostrar campos de contraseña
        this.pasoPassword = true;
      },
      error: () => {
        this.verificandoCorreo = false;
        this.mensajeError = 'No se pudo validar el correo electrónico, intente nuevamente';
      }
    });
  }

  // Paso 6: Solicitar confirmación con validación estricta de FA03, FA04 y FA05
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    const passControl = this.registroForm.get('password');
    const confirmControl = this.registroForm.get('confirmarPassword');

    passControl?.markAsTouched();
    confirmControl?.markAsTouched();

    const passVal = passControl?.value || '';

    // FA03: Longitud fuera de rango (6 a 20)
    if (passControl?.hasError('required') || passVal.length < 6 || passVal.length > 20) {
      this.mensajeError = 'La contraseña debe tener entre 6 y 20 caracteres';
      return;
    }

    // FA04: Formato de contraseña inválido
    if (!PATRON_PASSWORD.test(passVal)) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      return;
    }

    // FA05: Las contraseñas no coinciden
    if (this.registroForm.hasError('noCoincide')) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    // Si todo está correcto, se muestra el modal de confirmación
    this.mostrarModalConfirmacion = true;
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
    const { nombreCompleto, fechaNacimiento, nacionalidad, correoElectronico, codigoArea, telefono, direccion, password } = this.registroForm.value;

    this.authService.registrar({
      nombreCompleto: nombreCompleto.trim(),
      fechaNacimiento: this.convertirFechaAISO(fechaNacimiento),
      nacionalidad,
      correo: correoElectronico.trim(),
      codigoArea: String(codigoArea).trim(),
      telefono: String(telefono).trim(),
      direccion: direccion.trim(),
      password
    }).subscribe({
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
        // FA02 desde backend si existió concurrencia
        if (err.status === 409) {
          this.mensajeError = 'El correo electrónico ya se encuentra registrado';
        } else {
          this.mensajeError = err.error?.message || 'Debe completar todos los datos personales obligatorios antes de continuar';
        }
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