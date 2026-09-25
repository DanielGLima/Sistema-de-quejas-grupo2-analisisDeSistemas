import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http'; // 1. Importado HttpClient

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

  // Bloqueo de fechas futuras en el calendario
  fechaMaximaHoy: string = this.obtenerFechaHoyISO();

  // Lista de nacionalidades
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

  // Control de flujo en dos pasos (Datos -> Contraseña)
  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;

  // 2. Inyectado HttpClient en el constructor
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
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100)]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', Validators.required]
    }, { validators: this.validarPasswordsIguales });
  }

  validarPasswordsIguales(control: AbstractControl): ValidationErrors | null {
    const pass = control.get('password')?.value;
    const confirm = control.get('confirmarPassword')?.value;
    return pass === confirm ? null : { noCoincide: true };
  }

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

    if (formInvalido) {
      this.mensajeError = 'Debe completar todos los datos personales obligatorios antes de continuar';
      return;
    }

    this.pasoPassword = true;
  }

  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      if (this.registroForm.hasError('noCoincide')) {
        this.mensajeError = 'Las contraseñas no coinciden';
      } else {
        this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      }
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  // 3. Envío real a Spring Boot (RegistroRequest)
  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado la creación de la cuenta';
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
        console.log('Usuario registrado con éxito en backend:', res);
        this.mensajeExito = 'Usuario registrado exitosamente. Redirigiendo al inicio de sesión...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 1500);
      },
      error: (err) => {
        console.error('Error al registrar usuario:', err);
        if (err.status === 400 && err.error?.message) {
          this.mensajeError = err.error.message;
        } else {
          this.mensajeError = 'No se pudo completar el registro. Verifique que el correo no esté en uso.';
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