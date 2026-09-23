import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GestionarCasos } from './gestionar-casos';

describe('GestionarCasos', () => {
  let component: GestionarCasos;
  let fixture: ComponentFixture<GestionarCasos>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GestionarCasos],
    }).compileComponents();

    fixture = TestBed.createComponent(GestionarCasos);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
