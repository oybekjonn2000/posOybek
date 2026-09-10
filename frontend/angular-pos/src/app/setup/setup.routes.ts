import { Routes } from '@angular/router';
export const SETUP_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./setup.component').then(m => m.SetupComponent) }
];
