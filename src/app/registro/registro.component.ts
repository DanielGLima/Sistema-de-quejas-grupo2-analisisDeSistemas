import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.scss']
})
export class RegistroComponent implements OnInit {
  registroForm!: FormGroup;
  pasoPassword: boolean = false;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeInfo: string = '';

  // Catálogo de nacionalidades para CU-01
  listaNacionalidades: string[] = ['Guatemalteca', 'Salvadoreña', 'Hondureña', 'Mexicana', 'Costarricense', 'Otra'];

  private correosExistentes: string[] = ['admin@delicias.com', 'cliente@delicias.com'];

  constructor(private fb: FormBuilder, private router: Router) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.maxLength(20), this.validarPasswordFormato]],
      confirmarPassword: ['', [Validators.required, Validators.maxLength(20)]]
    }, { validators: this.validarPasswordCoincidencia });
  }

  // FA02 - Formato de contraseña: mín 6, máx 20, al menos una mayúscula, un número y un carácter especial
  validarPasswordFormato(control: AbstractControl): ValidationErrors | null {
    const value = control.value || '';
    if (!value) return null;
    const regex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>_\-])[A-Za-z\d!@#$%^&*(),.?":{}|<>_\-]{6,20}$/;
    return regex.test(value) ? null : { passwordInvalida: true };
  }

  validarPasswordCoincidencia(formGroup: AbstractControl): ValidationErrors | null {
    const password = formGroup.get('password')?.value;
    const confirmarPassword = formGroup.get('confirmarPassword')?.value;
    return password === confirmarPassword ? null : { passwordNoCoincide: true };
  }

  // Paso 4: Botón "Crear nueva cuenta"
  habilitarPassword(): void {
    this.limpiarMensajes();

    const camposBasicos = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion'];
    const invalidos = camposBasicos.some(campo => this.registroForm.get(campo)?.invalid);

    // FA01: Campos obligatorios incompletos o inválidos
    if (invalidos) {
      camposBasicos.forEach(campo => this.registroForm.get(campo)?.markAsTouched());
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // FA01.1: Correo electrónico ya registrado
    const correoIngresado = this.registroForm.get('correoElectronico')?.value?.toLowerCase();
    if (this.correosExistentes.includes(correoIngresado)) {
      this.mensajeError = 'El correo electrónico ya se encuentra registrado';
      return;
    }

    this.pasoPassword = true;
  }

  // Paso 6: Solicita confirmación
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    const passwordControl = this.registroForm.get('password');
    const confirmarControl = this.registroForm.get('confirmarPassword');

    // FA01: Campos vacíos
    if (!passwordControl?.value || !confirmarControl?.value) {
      this.mensajeError = 'Debe ingresar los campos obligatorios';
      return;
    }

    // FA02: Formato de la contraseña no válido
    if (passwordControl.invalid) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número';
      return;
    }

    if (this.registroForm.hasError('passwordNoCoincide')) {
      this.mensajeError = 'Las contraseñas ingresadas no coinciden';
      return;
    }

    // Muestra modal o diálogo de confirmación
    this.mostrarModalConfirmacion = true;
  }

  // Paso 7, 8, 9 y 10 / FA03
  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      // FA03: El usuario cancela la confirmación
      this.mensajeInfo = 'Se ha cancelado el registro satisfactoriamente';
      return;
    }

    // Pasos 8 y 9: Registro exitoso
    alert('El registro fue completado exitosamente');

    // Paso 10: Redirección automática a CU-00
    this.router.navigate(['/login']);
  }

  private limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';
  }
}