import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize } from 'rxjs';
import { LoadingService } from '../services/loading.service';

/**
 * HTTP interceptor that shows global loading indicator during requests.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Skip loading indicator for background sync requests
  if (req.headers.has('X-Skip-Loading')) {
    return next(req);
  }

  loadingService.show();
  return next(req).pipe(
    finalize(() => loadingService.hide())
  );
};
