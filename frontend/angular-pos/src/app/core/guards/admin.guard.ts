import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Guard protecting routes accessible exclusively by ADMIN.
 */
export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const notify = inject(NotificationService);
  const router = inject(Router);

  if (authService.isAdmin()) {
    return true;
  }

  notify.error('Ushbu sahifaga faqat Administrator kira oladi.');
  router.navigate([authService.getDefaultRoute()]);
  return false;
};
