import { Injectable, signal, computed } from '@angular/core';
import { fromEvent, merge } from 'rxjs';
import { environment } from '../../../environments/environment';

export type ConnectionStatus = 'online' | 'offline' | 'syncing' | 'sync-error';

/**
 * Connection monitoring service — tracks online/offline status.
 * Central to the offline-first architecture.
 */
@Injectable({ providedIn: 'root' })
export class ConnectionService {
  private _isOnline = signal<boolean>(navigator.onLine);
  private _status = signal<ConnectionStatus>(navigator.onLine ? 'online' : 'offline');
  private _offlineSimulation = signal<boolean>(environment.offlineSimulation);
  private _pendingSyncCount = signal<number>(0);

  readonly isOnline = computed(() => this._isOnline() && !this._offlineSimulation());
  readonly status = this._status.asReadonly();
  readonly offlineSimulation = this._offlineSimulation.asReadonly();
  readonly pendingSyncCount = this._pendingSyncCount.asReadonly();

  constructor() {
    this.setupEventListeners();
  }

  private setupEventListeners(): void {
    merge(
      fromEvent(window, 'online'),
      fromEvent(window, 'offline')
    ).subscribe(() => {
      const online = navigator.onLine;
      this._isOnline.set(online);

      if (!online || this._offlineSimulation()) {
        this._status.set('offline');
      } else {
        this._status.set('online');
      }
    });
  }

  toggleOfflineSimulation(): void {
    const current = this._offlineSimulation();
    this._offlineSimulation.set(!current);
    this._status.set(!current ? 'offline' : (navigator.onLine ? 'online' : 'offline'));
  }

  setSyncing(): void {
    this._status.set('syncing');
  }

  setOnline(): void {
    if (this.isOnline()) {
      this._status.set('online');
    }
  }

  setSyncError(): void {
    this._status.set('sync-error');
  }

  updatePendingSyncCount(count: number): void {
    this._pendingSyncCount.set(count);
  }
}
