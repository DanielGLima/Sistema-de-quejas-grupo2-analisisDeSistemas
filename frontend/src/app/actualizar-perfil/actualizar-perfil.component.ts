import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { NavbarComponent } from '../shared/navbar/navbar';

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
  cargando: boolean = false;

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

  private nombrePattern = /^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]+$/;
  private apiUrl = 'http://localhost:8081/api/auth';

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private router: Router,
    private cd: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.inicializarFormulario();
    this.cargarDatosPerfil();
  }

  private obtenerFechaHoyISO(): string {
    const hoy = new Date();
    const anio = hoy.getFullYear();
    const mes = String(hoy.getMonth() + 1).padStart(2, '0');
    const dia = String(hoy.getDate()).padStart(2, '0');
    return `${anio}-${mes}-${dia}`;
  }

  inicializarFormulario(): void {
    this.perfilForm = this.fb.group({
      nombreCompleto: ['', [
        Validators.required, 
        Validators.maxLength(100),
        Validators.pattern(this.nombrePattern)
      ]],
      fechaNacimiento: ['', Validators.required],
      nacionalidad: ['', Validators.required],
      correoElectronico: [{ value: '', disabled: true }],
      codigoArea: ['', [Validators.required, Validators.pattern(/^[0-9]{3}$/)]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required, Validators.maxLength(150)]]
    });
  }

  cargarDatosPerfil(): void {
    this.http.get<any>(`${this.apiUrl}/me`, { withCredentials: true }).subscribe({
      next: (usuario) => {
        if (usuario) {
          this.perfilForm.patchValue({
            nombreCompleto: usuario.nombreCompleto || '',
            fechaNacimiento: usuario.fechaNacimiento ? String(usuario.fechaNacimiento).substring(0, 10) : '',
            nacionalidad: usuario.nacionalidad || '',
            correoElectronico: usuario.correo || '',
            codigoArea: usuario.codigoArea || '502',
            telefono: usuario.telefono || '',
            direccion: usuario.direccion || ''
          });
          this.cd.detectChanges();
        }
      },
      error: () => {
        // En caso de no haber sesión activa
        this.cd.detectChanges();
      }
    });
  }

  soloLetras(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const valorLimpio = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\s]/g, '');
    this.perfilForm.get(controlName)?.setValue(valorLimpio, { emitEvent: false });
    input.value = valorLimpio;
  }

  soloNumeros(event: Event, controlName: string): void {
    const input = event.target as HTMLInputElement;
    const valorLimpio = input.value.replace(/[^0-9]/g, '');
    this.perfilForm.get(controlName)?.setValue(valorLimpio, { emitEvent: false });
    input.value = valorLimpio;
  }

  // Paso 4 / FA01
  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    // FA01: Campos obligatorios vacíos o con formato inválido
    if (this.perfilForm.invalid) {
      this.perfilForm.markAllAsTouched();
      this.mensajeError = 'Debe completar todos los campos obligatorios con el formato correcto';
      this.cd.detectChanges();
      return;
    }

    // Paso 5: Despliega confirmación
    this.mostrarModalConfirmacion = true;
    this.cd.detectChanges();
  }

  // Paso 6 / FA02 / Flujo Normal
  confirmarActualizacion(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    // FA02: Cancelación de la actualización en confirmación
    if (!acepta) {
      this.limpiarMensajes();
      this.mensajeInfo = 'Se ha cancelado la actualización del perfil';
      this.cd.detectChanges();
      return;
    }

    // Flujo Normal: Acepta y actualiza
    this.cargando = true;
    this.limpiarMensajes();

    const formVal = this.perfilForm.getRawValue();
    let fecha = formVal.fechaNacimiento;
    if (fecha && typeof fecha === 'string') {
      fecha = fecha.substring(0, 10);
    }

    const payload = {
      nombreCompleto: formVal.nombreCompleto?.trim(),
      fechaNacimiento: fecha,
      nacionalidad: formVal.nacionalidad,
      correo: formVal.correoElectronico?.trim(),
      codigoArea: formVal.codigoArea?.trim(),
      telefono: formVal.telefono?.trim(),
      direccion: formVal.direccion?.trim(),
      passwordActual: null,
      passwordNueva: null
    };

    this.http.put<any>(`${this.apiUrl}/perfil`, payload, { withCredentials: true }).subscribe({
      next: () => {
        this.cargando = false;
        this.limpiarMensajes();
        this.mensajeExito = 'Perfil actualizado exitosamente';
        this.cd.detectChanges();
      },
      error: (err) => {
        this.cargando = false;
        this.limpiarMensajes();
        this.mensajeError = err.error?.message || 'No se pudo actualizar el perfil. Intente nuevamente.';
        this.cd.detectChanges();
      }
    });
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
    this.mensajeInfo = '';
  }

  // Método auxiliar para resaltar campos afectados en FA01
  esInvalido(controlName: string): boolean {
    const control = this.perfilForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }
}