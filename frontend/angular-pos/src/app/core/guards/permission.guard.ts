import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Permission guard protecting routes requiring specific permissions.
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const notify = inject(NotificationService);
  const router = inject(Router);

  const disallowRoles = route.data['disallowRoles'] as string[] | undefined;
  const userRole = (authService.user()?.role || '').toUpperCase();
  if (disallowRoles && disallowRoles.includes(userRole)) {
    notify.error("Sizda bu bo'limga kirish huquqi yo'q!");
    router.navigate([authService.getDefaultRoute()]);
    return false;
  }

  if (authService.isAdmin()) {
    return true;
  }

  const permission = route.data['permission'] as string;

  if (!permission || authService.hasPermission(permission)) {
    return true;
  }

  notify.error('You do not have permission to access this page.');
  router.navigate([authService.getDefaultRoute()]);
  return false;
};
