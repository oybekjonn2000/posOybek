import { Component } from '@angular/core';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  template: `
    <div class="toast-container">
      @for (toast of notify.notifications(); track toast.id) {
        <div class="toast" [class]="'toast--' + toast.type">
          <span class="toast__icon">{{ getIcon(toast.type) }}</span>
          <span class="toast__message">{{ toast.message }}</span>
          <button class="toast__close" (click)="notify.remove(toast.id)">✕</button>
        </div>
      }
    </div>
  `
})
export class ToastContainerComponent {
  constructor(public notify: NotificationService) {}

  getIcon(type: string): string {
    const icons: Record<string, string> = {
      success: '✅', error: '❌', warning: '⚠️', info: 'ℹ️'
    };
    return icons[type] ?? 'ℹ️';
  }
}
