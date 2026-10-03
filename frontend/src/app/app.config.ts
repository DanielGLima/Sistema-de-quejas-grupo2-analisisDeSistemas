import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // Sin withFetch(): con zone.js, las respuestas HTTP basadas en fetch no disparaban
    // la deteccion de cambios y las pantallas quedaban sin actualizar.
    provideHttpClient()
  ]
};