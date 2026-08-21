import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrls: ['./registro.component.scss']
})
export class RegistroComponent implements OnInit {
  registroForm!: FormGroup;
  pasoValidado: boolean = false;
  mensajeError: string = '';

  private correosExistentes: string[] = ['admin@delicias.com', 'cliente@delicias.com'];

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.inicializarFormulario();
  }

  inicializarFormulario(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', Validators.required],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email]],
      codigoArea: ['', Validators.required],
      telefono: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      direccion: ['', Validators.required],
      password: ['', [Validators.required, this.validarPasswordFormato]]
    });
  }

  validarPasswordFormato(control: AbstractControl): ValidationErrors | null {
    const value = control.value || '';
    if (!value) return null;
    const esValido = /[A-Z]/.test(value) && /[0-9]/.test(value) && /[!@#$%^&*(),.?":{}|<>]/.test(value) && value.length >= 6;
    return esValido ? null : { passwordInvalida: true };
  }

  validarDatos(): void {
    this.mensajeError = '';
    const camposBasicos = ['nombreCompleto', 'fechaNacimiento', 'nacionalidad', 'correoElectronico', 'codigoArea', 'telefono', 'direccion'];
    const formularioInvalido = camposBasicos.some(campo => this.registroForm.get(campo)?.invalid);

    if (formularioInvalido) {
      this.mensajeError = 'Debe ingresar los campos obligatorios correctamente.';
      return;
    }

    const correoIngresado = this.registroForm.get('correoElectronico')?.value?.toLowerCase();
    if (this.correosExistentes.includes(correoIngresado)) {
      this.mensajeError = 'El correo electrónico ya se encuentra registrado.';
      return;
    }

    this.pasoValidado = true;
  }

  registrarUsuario(): void {
    this.mensajeError = '';
    const passwordControl = this.registroForm.get('password');
    if (passwordControl?.invalid) {
      this.mensajeError = 'El formato de la contraseña debe incluir al menos una letra mayúscula, un carácter especial y un número.';
      return;
    }

    if (!window.confirm('¿Desea confirmar el registro de su cuenta?')) {
      alert('Se ha cancelado el registro satisfactoriamente.');
      return;
    }

    alert('¡Usuario registrado exitosamente en el sistema!');
    this.registroForm.reset();
    this.pasoValidado = false;
  }
}
