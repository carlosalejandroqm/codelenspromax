import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    title: 'Iniciar sesion · CodeLens',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    title: 'Crear cuenta · CodeLens',
    loadComponent: () => import('./pages/register/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        title: 'Nuevo analisis · CodeLens',
        loadComponent: () => import('./pages/scan/scan.component').then((m) => m.ScanComponent),
      },
      {
        path: 'history',
        title: 'Historial · CodeLens',
        loadComponent: () => import('./pages/history/history.component').then((m) => m.HistoryComponent),
      },
      {
        path: 'reports/:id',
        title: 'Reporte · CodeLens',
        loadComponent: () => import('./pages/report/report.component').then((m) => m.ReportComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
