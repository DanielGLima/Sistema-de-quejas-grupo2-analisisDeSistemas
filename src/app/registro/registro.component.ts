import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
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
  mensajeError: string = '';
  mensajeExito: string = '';

  // Arreglo vacío listo para catálogo del backend
  nacionalidades: string[] = [];

  constructor(private fb: FormBuilder, private router: Router) {}

  ngOnInit(): void {
    this.registroForm = this.fb.group({
      nombreCompleto: ['', [Validators.required, Validators.maxLength(100)]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
      codigoArea: ['502', [Validators.required, Validators.pattern(/^[0-9]{1,4}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]]
    });
  }

  registrarUsuario(): void {
    this.mensajeError = '';
    this.mensajeExito = '';

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      return;
    }

    this.mensajeExito = 'Cuenta creada exitosamente.';
  }
}