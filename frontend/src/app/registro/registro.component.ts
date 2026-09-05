import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Formato exigido por CU-01: correo con @ y dominio con al menos un punto.
const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// Solo letras (con acentos/ñ) y espacios, sin números.
const PATRON_SOLO_LETRAS = /^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ]+$/;
// Al menos una mayúscula, un número y un carácter especial (FA02).
const PATRON_PASSWORD = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-]).+$/;

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

  // CU-01: catalogo de nacionalidad (el documento de casos de uso no definio
  // valores concretos, asi que se deja como lista fija en el frontend).
  listaNacionalidades: string[] = [
    'Guatemalteca', 'Mexicana', 'Salvadoreña', 'Hondureña', 'Nicaragüense',
    'Costarricense', 'Panameña', 'Colombiana', 'Estadounidense', 'Otra'
  ];

  // Control de flujo en dos pasos (Datos -> Contraseña)
  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;
  verificandoCorreo: boolean = false;

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_SOLO_LETRAS)]],
      fechaNacimiento: ['', [Validators.required, this.validarFechaDDMMAAAA]],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(PATRON_CORREO)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(20), Validators.pattern(PATRON_PASSWORD)]],
      confirmarPassword: ['', Validators.required]
    }, { validators: this.validarPasswordsIguales });
  }

  // Valida el formato dd/mm/aaaa escrito a mano y que no sea una fecha futura.
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

    // No se toca "input.value" a mano: se deja que Angular actualice el
    // input desde el FormControl (mezclar ambos pierde caracteres al escribir rápido).
    this.registroForm.get('fechaNacimiento')?.setValue(formateado);
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
    this.registroForm.get(controlName)?.setValue(limpio);
  }

  // Filtra en tiempo real para que el nombre completo no acepte números.
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

  habilitarPassword(): void {
    this.limpiarMensajes();

    // Validar solo los campos de la primera sección
    const camposPaso1 = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion'];
    let formInvalido = false;

    camposPaso1.forEach(campo => {
      const control = this.registroForm.get(campo);
      if (!control || control.invalid) {
        control?.markAsTouched();
        formInvalido = true;
      }
    });

    if (formInvalido) {
      this.mensajeError = 'Debe completar todos los datos personales obligatorios antes de continuar';
      return;
    }

    // FA01.1: verificar que el correo no este ya registrado antes de pedir la contraseña
    this.verificandoCorreo = true;
    const correo = this.registroForm.get('correoElectronico')?.value;

    this.authService.correoDisponible(correo).subscribe({
      next: ({ disponible }) => {
        this.verificandoCorreo = false;
        if (!disponible) {
          this.mensajeError = 'El correo electrónico ya se encuentra registrado';
          return;
        }
        this.pasoPassword = true;
      },
      error: () => {
        this.verificandoCorreo = false;
        this.mensajeError = 'No se pudo validar el correo electrónico, intente nuevamente';
      }
    });
  }

  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();

      const passwordControl = this.registroForm.get('password');
      if (this.registroForm.hasError('noCoincide')) {
        this.mensajeError = 'Las contraseñas no coinciden';
      } else if (passwordControl?.hasError('pattern')) {
        // FA02: Formato de la contraseña no válido
        this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      } else {
        this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      }
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado el registro satisfactoriamente';
      return;
    }

    const { nombreCompleto, fechaNacimiento, nacionalidad, correoElectronico, codigoArea, telefono, direccion, password } = this.registroForm.value;

    this.authService.registrar({
      nombreCompleto,
      fechaNacimiento: this.convertirFechaAISO(fechaNacimiento),
      nacionalidad,
      correo: correoElectronico, codigoArea, telefono, direccion, password
    }).subscribe({
      next: () => {
        this.mensajeExito = 'Usuario registrado exitosamente. Redirigiendo al inicio de sesión...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        // FA01.1 - Correo ya registrado
        this.mensajeError = err.error?.message ?? 'No se pudo completar el registro';
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}
