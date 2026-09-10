import { Component, computed } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { ConnectionService } from '../../core/services/connection.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [DatePipe],
  template: `
    <header class="topbar">
      <div class="topbar__left">
        <div class="topbar__title">
          {{ pageTitle }}
        </div>
      </div>

      <div class="topbar__right">
        <!-- Current time -->
        <div class="topbar__time">
          {{ currentTime | date:'HH:mm:ss' }}
        </div>

        <!-- Connection status -->
        <div class="connection-indicator"
             [class]="connection.status()"
             (click)="connection.toggleOfflineSimulation()"
             [title]="getStatusTooltip()">
          <span class="dot"></span>
          <span>{{ getStatusLabel() }}</span>
          @if (connection.pendingSyncCount() > 0) {
            <span style="background: var(--primary); color: white; border-radius: 100px; padding: 1px 6px; font-size: 11px; font-weight: 700;">
              {{ connection.pendingSyncCount() }}
            </span>
          }
        </div>

        <!-- User menu -->
        <div class="topbar__user" (click)="logout()">
          <span>{{ auth.user()?.fullName }}</span>
          <span style="color: var(--text-muted)">⟵ Logout</span>
        </div>
      </div>
    </header>
  `,
  styles: [`
    .topbar {
      position: fixed;
      top: 0;
      right: 0;
      left: var(--sidebar-width);
      height: var(--topbar-height);
      background: var(--bg-secondary);
      border-bottom: 1px solid var(--border);
      display: flex;
      align-items: center;
      padding: 0 20px;
      z-index: 999;
      gap: 16px;
      transition: left var(--transition-slow);

      &__left { flex: 1; }

      &__title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-primary);
      }

      &__right {
        display: flex;
        align-items: center;
        gap: 16px;
      }

      &__time {
        font-family: var(--font-mono);
        font-size: 14px;
        color: var(--text-secondary);
        background: var(--bg-tertiary);
        padding: 6px 12px;
        border-radius: var(--radius-sm);
        border: 1px solid var(--border);
      }

      &__user {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 6px 14px;
        border-radius: var(--radius-sm);
        cursor: pointer;
        font-size: 13px;
        font-weight: 500;
        color: var(--text-secondary);
        border: 1px solid var(--border);
        transition: all var(--transition);

        &:hover {
          background: var(--bg-hover);
          color: var(--danger);
          border-color: var(--danger);
        }
      }
    }
  `]
})
export class TopbarComponent {
  pageTitle = 'Restaurant POS';
  currentTime = new Date();

  constructor(
    public auth: AuthService,
    public connection: ConnectionService
  ) {
    setInterval(() => this.currentTime = new Date(), 1000);
  }

  getStatusLabel(): string {
    switch (this.connection.status()) {
      case 'online': return 'Online';
      case 'offline': return 'Offline';
      case 'syncing': return 'Syncing...';
      case 'sync-error': return 'Sync Error';
      default: return 'Unknown';
    }
  }

  getStatusTooltip(): string {
    const sim = this.connection.offlineSimulation();
    const base = sim ? 'SIMULATING OFFLINE - Click to toggle' : 'Click to simulate offline mode';
    const pending = this.connection.pendingSyncCount();
    return pending > 0 ? `${base} | ${pending} pending sync events` : base;
  }

  logout(): void {
    this.auth.logout();
  }
}
