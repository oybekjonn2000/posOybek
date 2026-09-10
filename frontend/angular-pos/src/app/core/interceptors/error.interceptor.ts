import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

/**
 * Global HTTP error interceptor — converts HTTP errors to user notifications.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notify = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 0) {
        // Network error — offline mode
        notify.warn('Server is not reachable. Working in offline mode.');
      } else if (error.status >= 500) {
        const message = error.error?.message || 'Server error occurred';
        notify.error(message);
      }
      // 400, 401, 403, 404 are handled by components
      return throwError(() => error);
    })
  );
};
