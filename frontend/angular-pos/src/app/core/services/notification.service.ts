import { Injectable, signal } from '@angular/core';

export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

/**
 * Toast notification service using Angular Signals.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly notifications = signal<Notification[]>([]);

  success(message: string, duration = 4000): void {
    this.add({ type: 'success', message, duration });
  }

  error(message: string, duration = 6000): void {
    this.add({ type: 'error', message, duration });
  }

  warn(message: string, duration = 5000): void {
    this.add({ type: 'warning', message, duration });
  }

  warning(message: string, duration = 5000): void {
    this.warn(message, duration);
  }

  info(message: string, duration = 4000): void {
    this.add({ type: 'info', message, duration });
  }

  remove(id: string): void {
    this.notifications.update(ns => ns.filter(n => n.id !== id));
  }

  private add(notification: Omit<Notification, 'id'>): void {
    const id = Date.now().toString();
    this.notifications.update(ns => [...ns, { ...notification, id }]);

    if (notification.duration) {
      setTimeout(() => this.remove(id), notification.duration);
    }
  }
}
