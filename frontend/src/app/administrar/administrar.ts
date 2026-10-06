import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavbarComponent } from '../shared/navbar/navbar';
import { AdminService } from '../services/admin.service';
import { CatalogoRequest, EstadoApi, Personal, RolApi } from '../core/models/admin';
import { CategoriaCaso, Sucursal, TipoCaso } from '../core/models/catalogos';

type Pestana = 'personal' | 'sucursales' | 'tipos' | 'categorias' | 'estados';
type CatalogoApi = 'sucursales' | 'tipos-caso' | 'categorias' | 'estados';

// CU-13: administración de usuarios internos y catálogos (solo Administrador General).
@Component({
  selector: 'app-administrar',
  standalone: true,
  imports: [CommonModule, FormsModule, NavbarComponent],
  templateUrl: './administrar.html',
  styleUrl: './administrar.scss'
})
export class AdministrarComponent implements OnInit {
  pestana: Pestana = 'personal';
  mensajeExito = '';
  mensajeError = '';

  personal: Personal[] = [];
  roles: RolApi[] = [];
  sucursales: Sucursal[] = [];
  tipos: TipoCaso[] = [];
  categorias: CategoriaCaso[] = [];
  estados: EstadoApi[] = [];

  // Formulario de usuario interno (idEdicion === null => alta)
  formPersonal = this.personalVacio();
  idEdicionPersonal: number | null = null;

  // Formularios de catálogo
  formSucursal: CatalogoRequest = { nombre: '', direccion: '', telefono: '' };
  formTipo: CatalogoRequest = { nombre: '', codigo: '' };
  formCategoria: CatalogoRequest = { nombre: '', codigo: '' };
  idEdicionCatalogo: number | null = null;

  constructor(private admin: AdminService) {}

  ngOnInit(): void {
    this.recargarTodo();
  }

  private personalVacio() {
    return { nombreCompleto: '', correo: '', idRol: null as number | null, idSucursal: null as number | null, activo: true, passwordTemporal: '' };
  }

  private recargarTodo(): void {
    this.admin.personal().subscribe(p => this.personal = p);
    this.admin.roles().subscribe(r => this.roles = r);
    this.admin.sucursales().subscribe(s => this.sucursales = s);
    this.admin.tiposCaso().subscribe(t => this.tipos = t);
    this.admin.categorias().subscribe(c => this.categorias = c);
    this.admin.estados().subscribe(e => this.estados = e);
  }

  cambiarPestana(pestana: Pestana): void {
    this.pestana = pestana;
    this.limpiarMensajes();
    this.cancelarEdicion();
  }

  limpiarMensajes(): void {
    this.mensajeError = '';
    this.mensajeExito = '';
  }

  cancelarEdicion(): void {
    this.idEdicionPersonal = null;
    this.idEdicionCatalogo = null;
    this.formPersonal = this.personalVacio();
    this.formSucursal = { nombre: '', direccion: '', telefono: '' };
    this.formTipo = { nombre: '', codigo: '' };
    this.formCategoria = { nombre: '', codigo: '' };
  }

  private exito(texto: string): void {
    this.limpiarMensajes();
    this.mensajeExito = texto;
    this.cancelarEdicion();
    this.recargarTodo();
  }

  private fallo(err: any, porDefecto: string): void {
    this.mensajeExito = '';
    // FA03: el servidor indica qué dato debe corregirse; FA02/FA01: advertencias de casos activos
    this.mensajeError = err.error?.message ?? porDefecto;
  }

  // ---------------- Usuarios internos ----------------
  editarPersonal(p: Personal): void {
    this.limpiarMensajes();
    this.idEdicionPersonal = p.idPersonal;
    this.formPersonal = {
      nombreCompleto: p.nombreCompleto,
      correo: p.correo,
      idRol: p.rol.idRol,
      idSucursal: p.sucursal?.idSucursal ?? null,
      activo: p.activo,
      passwordTemporal: ''
    };
  }

  guardarPersonal(): void {
    this.limpiarMensajes();
    const f = this.formPersonal;
    const datos = {
      nombreCompleto: f.nombreCompleto,
      correo: f.correo,
      idRol: Number(f.idRol),
      idSucursal: Number(f.idSucursal),
      activo: f.activo,
      passwordTemporal: f.passwordTemporal || undefined
    };

    const peticion = this.idEdicionPersonal === null
      ? this.admin.crearPersonal(datos)
      : this.admin.actualizarPersonal(this.idEdicionPersonal, datos);

    peticion.subscribe({
      next: () => this.exito(this.idEdicionPersonal === null ? 'Usuario interno creado correctamente' : 'Usuario interno actualizado correctamente'),
      error: (err) => this.fallo(err, 'No se pudo guardar el usuario interno')
    });
  }

  alternarPersonal(p: Personal): void {
    this.limpiarMensajes();
    this.admin.actualizarPersonal(p.idPersonal, {
      nombreCompleto: p.nombreCompleto,
      correo: p.correo,
      idRol: p.rol.idRol,
      idSucursal: p.sucursal?.idSucursal ?? 0,
      activo: !p.activo
    }).subscribe({
      next: () => this.exito(p.activo ? 'Cuenta desactivada' : 'Cuenta activada'),
      // FA02: si tiene casos activos, el servidor pide reasignarlos antes de desactivar
      error: (err) => this.fallo(err, 'No se pudo cambiar el estado de la cuenta')
    });
  }

  // ---------------- Catálogos ----------------
  editarSucursal(s: Sucursal): void {
    this.limpiarMensajes();
    this.idEdicionCatalogo = s.idSucursal;
    this.formSucursal = { nombre: s.nombreSucursal, direccion: s.direccionSucursal, telefono: s.telefonoSucursal ?? '' };
  }

  editarTipo(t: TipoCaso): void {
    this.limpiarMensajes();
    this.idEdicionCatalogo = t.idTipoCaso;
    this.formTipo = { nombre: t.nombre, codigo: t.codigo };
  }

  editarCategoria(c: CategoriaCaso): void {
    this.limpiarMensajes();
    this.idEdicionCatalogo = c.idCategoria;
    this.formCategoria = { nombre: c.nombre, codigo: c.codigo };
  }

  guardarSucursal(): void {
    this.guardarCatalogo('sucursales', this.idEdicionCatalogo, this.formSucursal, 'Sucursal');
  }

  guardarTipo(): void {
    this.guardarCatalogo('tipos-caso', this.idEdicionCatalogo, { ...this.formTipo, codigo: (this.formTipo.codigo ?? '').toUpperCase() }, 'Tipo de caso');
  }

  guardarCategoria(): void {
    this.guardarCatalogo('categorias', this.idEdicionCatalogo, { ...this.formCategoria, codigo: (this.formCategoria.codigo ?? '').toUpperCase() }, 'Categoría');
  }

  private guardarCatalogo(catalogo: CatalogoApi, id: number | null, datos: CatalogoRequest, etiqueta: string): void {
    this.limpiarMensajes();
    this.admin.guardarCatalogo(catalogo, id, datos).subscribe({
      next: () => this.exito(`${etiqueta} ${id === null ? 'creada' : 'actualizada'} correctamente`),
      error: (err) => this.fallo(err, `No se pudo guardar: ${etiqueta}`)
    });
  }

  // FA01: al desactivar un elemento con casos activos se muestra la advertencia y se pide confirmar.
  alternarSucursal(s: Sucursal): void {
    this.alternarCatalogo('sucursales', s.idSucursal, s.activo,
      { nombre: s.nombreSucursal, direccion: s.direccionSucursal, telefono: s.telefonoSucursal ?? '' });
  }

  alternarTipo(t: TipoCaso): void {
    this.alternarCatalogo('tipos-caso', t.idTipoCaso, t.activo, { nombre: t.nombre, codigo: t.codigo });
  }

  alternarCategoria(c: CategoriaCaso): void {
    this.alternarCatalogo('categorias', c.idCategoria, c.activo, { nombre: c.nombre, codigo: c.codigo });
  }

  alternarEstado(e: EstadoApi): void {
    this.alternarCatalogo('estados', e.idEstado, e.activo, { nombre: e.nombre });
  }

  private alternarCatalogo(catalogo: CatalogoApi, id: number, activoActual: boolean, base: CatalogoRequest, confirmar = false): void {
    this.limpiarMensajes();
    this.admin.guardarCatalogo(catalogo, id, { ...base, activo: !activoActual }, confirmar).subscribe({
      next: () => this.exito(activoActual ? 'Elemento desactivado' : 'Elemento activado'),
      error: (err) => {
        const mensaje: string = err.error?.message ?? '';
        if (err.status === 409 && mensaje.startsWith('Advertencia') && !confirmar) {
          if (confirm(`${mensaje}\n\n¿Desea continuar?`)) {
            this.alternarCatalogo(catalogo, id, activoActual, base, true);
          }
          return;
        }
        this.fallo(err, 'No se pudo cambiar el estado del elemento');
      }
    });
  }
}
