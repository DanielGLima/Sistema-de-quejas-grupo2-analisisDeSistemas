import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

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

  fechaMaximaHoy: string = this.obtenerFechaHoyISO();

  listaNacionalidades: string[] = [
    'Guatemalteca',
    'Salvadoreña',
    'Hondureña',
    'Nicaragüense',
    'Costarricense',
    'Panameña',
    'Mexicana',
    'Otra'
  ];

  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;

  private passwordPattern = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$\%^&*(),.?":{}\vert{}<>_\-]).+$/;
  private nombrePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$/;
  // Exige usuario + @ + dominio + extensión (ej. usuario@gmail.com)
  private emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

  constructor(
    private fb: FormBuilder, 
    private router: Router,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  private obtenerFechaHoyISO(): string {
    const hoy = new Date();
    const anio = hoy.getFullYear();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', [
        Validators.required, 
        Validators.maxLength(100),
        Validators.pattern(this.nombrePattern)
      ]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [
        Validators.required, 
        Validators.maxLength(100),
        Validators.pattern(this.emailPattern)
      ]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{3}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [
        Validators.required, 
        Validators.minLength(6), 
        Validators.maxLength(20), 
        Validators.pattern(this.passwordPattern)
      ]],
      confirmarPassword: ['', Validators.required]
    }, { validators: this.validarPasswordsIguales });
  }

  soloLetras(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const valorLimpio = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]/g, '');
    this.registroForm.get(controlName)?.setValue(valorLimpio, { emitEvent: false });
    input.value = valorLimpio;
  }

  soloNumeros(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const valorLimpio = input.value.replace(/[^0-9]/g, '');
    this.registroForm.get(controlName)?.setValue(valorLimpio, { emitEvent: false });
    input.value = valorLimpio;
  }

  validarPasswordsIguales(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('password')?.value;
    const confirm = control.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoincide: true };
  }

  habilitarPassword(): void {
    this.limpiarMensajes();

    const correoControl = this.registroForm.get('correoElectronico');
    const valorCorreo = correoControl?.value?.trim() || '';

    // Si está vacío
    if (!valorCorreo) {
      correoControl?.markAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico';
      return;
    }

    // Si el formato no contiene @ o dominio válido
    if (correoControl?.hasError('pattern')) {
      correoControl.markAsTouched();
      this.mensajeError = 'Debe ingresar un correo electrónico válido (ejemplo: usuario@correo.com)';
      return;
    }

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
      const nom = this.registroForm.get('nombreCompleto');
      const area = this.registroForm.get('codigoArea');
      const tel = this.registroForm.get('telefono');

      if (nom?.hasError('pattern')) {
        this.mensajeError = 'El nombre completo solo debe contener letras';
        return;
      }
      if (area?.hasError('pattern')) {
        this.mensajeError = 'El código de área debe tener exactamente 3 dígitos numéricos';
        return;
      }
      if (tel?.hasError('pattern')) {
        this.mensajeError = 'El teléfono debe contener exactamente 8 dígitos numéricos';
        return;
      }

      this.mensajeError = 'Debe completar todos los datos personales obligatorios antes de continuar';
      return;
    }

    // Consulta disponibilidad en el backend
    this.http.get<{ disponible: boolean }>(`http://localhost:8081/api/auth/correo-disponible?correo=${valorCorreo}`)
      .subscribe({
        next: (res) => {
          if (res.disponible) {
            this.pasoPassword = true;
          } else {
            this.mensajeError = 'El correo electrónico ya se encuentra registrado';
          }
        },
        error: (err) => {
          console.error('Error al verificar correo:', err);
          this.mensajeError = 'No se pudo verificar la disponibilidad del correo en este momento';
        }
      });
  }

  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    const passControl = this.registroForm.get('password');
    const confirmControl = this.registroForm.get('confirmarPassword');

    passControl?.markAsTouched();
    confirmControl?.markAsTouched();

    if (!passControl?.value || !confirmControl?.value) {
      this.mensajeError = 'Debe ingresar y confirmar su contraseña';
      return;
    }

    if (passControl.hasError('minlength') || passControl.hasError('maxlength')) {
      this.mensajeError = 'La contraseña debe tener entre 6 y 20 caracteres';
      return;
    }

    if (passControl.hasError('pattern')) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      return;
    }

    if (this.registroForm.hasError('noCoincide')) {
      this.mensajeError = 'Las contraseñas no coinciden';
      return;
    }

    if (this.registroForm.invalid) {
      this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.limpiarMensajes();
      this.registroForm.reset({
        nacionalidad: ''
      });
      this.pasoPassword = false;
      this.mensajeInfo = 'Se ha cancelado el registro satisfactoriamente';
      return;
    }

    const val = this.registroForm.value;

    const payload = {
      nombreCompleto: val.nombreCompleto,
      fechaNacimiento: val.fechaNacimiento,
      nacionalidad: val.nacionalidad,
      correo: val.correoElectronico,
      codigoArea: val.codigoArea,
      telefono: val.telefono,
      direccion: val.direccion,
      password: val.password
    };

    this.http.post('http://localhost:8081/api/auth/registro', payload).subscribe({
      next: (res) => {
        this.limpiarMensajes();
        this.registroForm.reset();
        this.pasoPassword = false;
        this.mensajeExito = 'Usuario registrado exitosamente. Redirigiendo al inicio de sesión...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error al registrar usuario:', err);
        if ((err.status === 409 || err.status === 400) && (err.error?.message || err.error?.detail)) {
          this.mensajeError = err.error.message || err.error.detail;
        } else if (err.status === 409) {
          this.mensajeError = 'El correo electrónico ya se encuentra registrado';
        } else {
          this.mensajeError = 'No se pudo completar el registro. Intente nuevamente.';
        }
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }
}