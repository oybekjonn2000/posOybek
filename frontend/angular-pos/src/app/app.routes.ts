import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { authGuard } from './core/guards/auth.guard';
import { permissionGuard } from './core/guards/permission.guard';
import { adminGuard } from './core/guards/admin.guard';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then(m => m.AUTH_ROUTES)
  },
  {
    path: 'setup',
    loadChildren: () => import('./setup/setup.routes').then(m => m.SETUP_ROUTES)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => inject(AuthService).getDefaultRoute()
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
        data: { permission: 'VIEW_DASHBOARD', title: 'Dashboard' }
      },
      {
        path: 'pos',
        loadComponent: () => import('./orders/pos/pos.component').then(m => m.PosComponent),
        canActivate: [permissionGuard],
        data: { permission: 'CREATE_ORDER', disallowRoles: ['KITCHEN'], title: 'POS' }
      },
      {
        path: 'tables',
        loadComponent: () => import('./tables/tables.component').then(m => m.TablesComponent),
        canActivate: [permissionGuard],
        data: { disallowRoles: ['KITCHEN'], title: 'Tables' }
      },
      {
        path: 'kitchen',
        loadComponent: () => import('./kitchen/kitchen.component').then(m => m.KitchenComponent),
        canActivate: [permissionGuard],
        data: { permission: 'KITCHEN_VIEW', disallowRoles: ['WAITER'], title: 'Kitchen' }
      },
      {
        path: 'orders',
        loadComponent: () => import('./orders/orders-list/orders-list.component').then(m => m.OrdersListComponent),
        canActivate: [permissionGuard],
        data: { disallowRoles: ['KITCHEN'], title: 'Orders' }
      },
      {
        path: 'products',
        loadComponent: () => import('./products/products.component').then(m => m.ProductsComponent),
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_PRODUCTS', title: 'Products' }
      },
      {
        path: 'categories',
        loadComponent: () => import('./categories/categories.component').then(m => m.CategoriesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_CATEGORIES', title: 'Categories' }
      },
      // Ombor moduli vaqtinchalik disable qilingan:
      // {
      //   path: 'inventory',
      //   loadChildren: () => import('./inventory/inventory.routes').then(m => m.INVENTORY_ROUTES),
      //   data: { title: 'Inventory' }
      // },
      {
        path: 'inventory',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'customers',
        loadComponent: () => import('./customers/customers.component').then(m => m.CustomersComponent),
        canActivate: [adminGuard],
        data: { title: 'Customers' }
      },
      {
        path: 'employees',
        loadComponent: () => import('./employees/employees.component').then(m => m.EmployeesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_USERS', title: 'Employees' }
      },
      {
        path: 'reports',
        loadChildren: () => import('./reports/reports.routes').then(m => m.REPORTS_ROUTES),
        canActivate: [permissionGuard],
        data: { permission: 'VIEW_REPORTS', title: 'Reports' }
      },
      {
        path: 'settings',
        loadChildren: () => import('./settings/settings.routes').then(m => m.SETTINGS_ROUTES),
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_SETTINGS', title: 'Settings' }
      },
      {
        path: 'devices',
        loadComponent: () => import('./devices/devices.component').then(m => m.DevicesComponent),
        canActivate: [permissionGuard],
        data: { permission: 'MANAGE_DEVICES', title: 'Devices' }
      },
      {
        path: 'shifts',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: ''
  }
];
