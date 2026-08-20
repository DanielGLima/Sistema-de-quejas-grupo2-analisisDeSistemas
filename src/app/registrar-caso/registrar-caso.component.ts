import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-registrar-caso',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registrar-caso.component.html',
  styleUrls: ['./registrar-caso.component.scss']
})
export class RegistrarCasoComponent implements OnInit {
  casoForm!: FormGroup;
  mostrarModalConfirmacion: boolean = false;
  mensajeError: string = '';
  mensajeInfo: string = '';

  // Tipos de caso según CU-05
  tiposCaso: string[] = ['Queja', 'Reclamo', 'Sugerencia', 'Felicitación'];
  
  sucursales: string[] = [
    'Sede Central - Zona 10',
    'Sucursal Roosevelt - Zona 11',
    'Sucursal Carretera a El Salvador',
    'Sucursal Cayalá - Zona 16'
  ];

  // Catálogo de motivos clasificados según el tipo de caso
  motivosPorTipo: { [key: string]: string[] } = {
    'Queja': [
      'Mala atención del personal',
      'Demora excesiva en el servicio',
      'Higiene o estado de las instalaciones',
      'Calidad o estado de la comida'
    ],
    'Reclamo': [
      'Cobro indebido o error en factura',
      'Incumplimiento de promociones',
      'Plato no corresponde al pedido',
      'Pedido para llevar incompleto'
    ],
    'Sugerencia': [
      'Nuevas opciones de menú',
      'Mejora en tiempos de entrega',
      'Instalaciones y ambiente',
      'Servicio a domicilio'
    ],
    'Felicitación': [
      'Excelente atención del mesero',
      'Calidad excepcional de los alimentos',
      'Ambiente y comodidad',
      'Rapidez en el servicio'
    ]
  };

  listaMotivosDisponibles: string[] = [];
  archivoAdjunto: File | null = null;
  nombreArchivo: string = '';

  constructor(private fb: FormBuilder, private router: Router) {}

  ngOnInit(): void {
    this.casoForm = this.fb.group({
      tipoCaso: ['', Validators.required],
      sucursal: ['', Validators.required],
      motivo: ['', Validators.required],
      fechaIncidente: ['', Validators.required],
      detalle: ['', [Validators.required, Validators.maxLength(1000)]],
      archivo: [null]
    });
  }

  // Se ejecuta al cambiar la selección en 'tipo de caso'
  alCambiarTipoCaso(): void {
    const tipo = this.casoForm.get('tipoCaso')?.value;
    if (tipo && this.motivosPorTipo[tipo]) {
      this.listaMotivosDisponibles = this.motivosPorTipo[tipo];
    } else {
      this.listaMotivosDisponibles = [];
    }
    this.casoForm.get('motivo')?.setValue('');
  }

  alSeleccionarArchivo(event: any): void {
    this.limpiarMensajes();
    const file = event.target.files[0];

    if (!file) {
      this.archivoAdjunto = null;
      this.nombreArchivo = '';
      return;
    }

    const formatosPermitidos = ['application/pdf', 'image/jpeg', 'image/png', 'image/jpg'];
    const tamanoMaximo = 5 * 1024 * 1024; // 5 MB

    if (!formatosPermitidos.includes(file.type)) {
      this.mensajeError = 'El archivo adjunto debe ser en formato PDF, JPG o PNG';
      event.target.value = '';
      this.archivoAdjunto = null;
      this.nombreArchivo = '';
      return;
    }

    if (file.size > tamanoMaximo) {
      this.mensajeError = 'El archivo adjunto no debe superar el tamaño máximo de 5MB';
      event.target.value = '';
      this.archivoAdjunto = null;
      this.nombreArchivo = '';
      return;
    }

    this.archivoAdjunto = file;
    this.nombreArchivo = file.name;
  }

  solicitarConfirmacion(): void {
    this.limpiarMensajes();

    if (this.casoForm.invalid) {
      this.casoForm.markAllAsTouched();
      this.mensajeError = 'Debe ingresar todos los campos obligatorios del caso';
      return;
    }

    this.mostrarModalConfirmacion = true;
  }

  confirmarRegistro(acepta: boolean): void {
    this.mostrarModalConfirmacion = false;

    if (!acepta) {
      this.mensajeInfo = 'Se ha cancelado el registro del caso';
      return;
    }

    alert('El caso fue registrado exitosamente');
    this.router.navigate(['/consultar-casos']);
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeInfo = '';
  }
}