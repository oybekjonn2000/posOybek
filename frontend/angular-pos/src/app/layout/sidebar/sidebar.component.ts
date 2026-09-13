import { Component, Output, EventEmitter, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  icon: string;
  label: string;
  route: string;
  permission?: string;
  adminOnly?: boolean;
  disallowRoles?: string[];
  badge?: number;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <nav class="sidebar" [class.collapsed]="collapsed()">
      <!-- Logo -->
      <div class="sidebar__logo" (click)="toggleCollapse()">
        <div class="sidebar__logo-icon">🍽️</div>
        @if (!collapsed()) {
          <div class="sidebar__logo-text">
            <span class="sidebar__brand">RestaurantPOS</span>
            <span class="sidebar__version">v1.0.0</span>
          </div>
        }
      </div>

      <!-- Navigation Items -->
      <div class="sidebar__nav">
        @for (item of visibleNavItems(); track item.route) {
          <a [routerLink]="item.route"
             routerLinkActive="active"
             class="sidebar__item"
             [title]="collapsed() ? item.label : ''">
            <span class="sidebar__icon">{{ item.icon }}</span>
            @if (!collapsed()) {
              <span class="sidebar__label">{{ item.label }}</span>
              @if (item.badge) {
                <span class="sidebar__badge">{{ item.badge }}</span>
              }
            }
          </a>
        }
      </div>

      <!-- Bottom User Section -->
      <div class="sidebar__footer">
        <div class="sidebar__user" [title]="collapsed() ? auth.user()?.fullName ?? '' : ''">
          <div class="sidebar__avatar">
            {{ getUserInitials() }}
          </div>
          @if (!collapsed()) {
            <div class="sidebar__user-info">
              <div class="sidebar__user-name">{{ auth.user()?.fullName }}</div>
              <div class="sidebar__user-role">{{ auth.user()?.role || auth.user()?.username }}</div>
            </div>
          }
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .sidebar {
      position: fixed;
      top: 0;
      left: 0;
      height: 100vh;
      width: var(--sidebar-width);
      background: var(--sidebar-bg);
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--border);
      transition: width var(--transition-slow);
      z-index: 1000;
      overflow: hidden;

      &.collapsed {
        width: var(--sidebar-collapsed-width);
      }

      &__logo {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 16px;
        height: var(--topbar-height);
        border-bottom: 1px solid var(--divider);
        cursor: pointer;
        user-select: none;
        flex-shrink: 0;
      }

      &__logo-icon {
        font-size: 24px;
        flex-shrink: 0;
      }

      &__brand {
        font-size: 15px;
        font-weight: 700;
        color: var(--text-primary);
        white-space: nowrap;
      }

      &__version {
        font-size: 11px;
        color: var(--text-muted);
        display: block;
      }

      &__nav {
        flex: 1;
        overflow-y: auto;
        padding: 12px 8px;
        display: flex;
        flex-direction: column;
        gap: 2px;
      }

      &__item {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 12px;
        border-radius: var(--radius-sm);
        color: var(--text-secondary);
        text-decoration: none;
        font-size: 14px;
        font-weight: 500;
        transition: all var(--transition);
        white-space: nowrap;
        position: relative;

        &:hover {
          background: var(--sidebar-active);
          color: var(--text-primary);
        }

        &.active {
          background: rgba(var(--primary-rgb), 0.15);
          color: var(--primary-light);
        }
      }

      &__icon { font-size: 18px; flex-shrink: 0; }

      &__badge {
        margin-left: auto;
        background: var(--danger);
        color: white;
        font-size: 11px;
        font-weight: 700;
        padding: 2px 6px;
        border-radius: 100px;
        min-width: 20px;
        text-align: center;
      }

      &__footer {
        padding: 12px 8px;
        border-top: 1px solid var(--divider);
        flex-shrink: 0;
      }

      &__user {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 10px 12px;
        border-radius: var(--radius-sm);
        cursor: pointer;
        transition: background var(--transition);

        &:hover { background: var(--sidebar-active); }
      }

      &__avatar {
        width: 34px;
        height: 34px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--primary), var(--accent));
        color: white;
        font-size: 13px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      }

      &__user-name {
        font-size: 13px;
        font-weight: 600;
        color: var(--text-primary);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        max-width: 150px;
      }

      &__user-role {
        font-size: 11px;
        color: var(--text-muted);
      }
    }
  `]
})
export class SidebarComponent {
  @Output() collapsedChange = new EventEmitter<boolean>();

  collapsed = signal(false);

  navItems: NavItem[] = [
    { icon: '📊', label: 'Boshqaruv paneli', route: '/dashboard', permission: 'VIEW_DASHBOARD' },
    { icon: '🖥️', label: 'Kassa (POS)', route: '/pos', permission: 'CREATE_ORDER' },
    { icon: '🪑', label: 'Stollar', route: '/tables' },
    { icon: '📋', label: 'Buyurtmalar', route: '/orders' },
    { icon: '👨‍🍳', label: 'Oshxona', route: '/kitchen', permission: 'KITCHEN_VIEW' },
    { icon: '🍔', label: 'Mahsulotlar', route: '/products', permission: 'MANAGE_PRODUCTS' },
    { icon: '📁', label: 'Kategoriyalar', route: '/categories', permission: 'MANAGE_CATEGORIES' },
    { icon: '🥘', label: 'Oshxonalar', route: '/kitchens', permission: 'MANAGE_SETTINGS', disallowRoles: ['KITCHEN', 'WAITER'] },
    // { icon: '📦', label: 'Ombor', route: '/inventory', permission: 'VIEW_STOCK' }, // Hozircha disable qilindi
    { icon: '👥', label: 'Mijozlar', route: '/customers', adminOnly: true },
    { icon: '👤', label: 'Xodimlar', route: '/employees', permission: 'MANAGE_USERS' },
    { icon: '📈', label: 'Hisobotlar', route: '/reports', permission: 'VIEW_REPORTS' },
    { icon: '📱', label: 'Qurilmalar', route: '/devices', permission: 'MANAGE_DEVICES' },
    { icon: '⚙️', label: 'Sozlamalar', route: '/settings', permission: 'MANAGE_SETTINGS' },
  ];

  constructor(public auth: AuthService) {}

  visibleNavItems() {
    const role = (this.auth.user()?.role || '').toUpperCase();
    return this.navItems.filter(item => {
      if (item.adminOnly && !this.auth.isAdmin()) {
        return false;
      }
      if (item.disallowRoles && item.disallowRoles.includes(role)) {
        return false;
      }
      // Kitchen user must NOT see Orders, POS, Tables, Dashboard, etc.
      if (role === 'KITCHEN' && item.route !== '/kitchen') {
        return false;
      }
      // Waiter user must NOT see Kitchen
      if (role === 'WAITER' && item.route === '/kitchen') {
        return false;
      }
      if (this.auth.isAdmin()) {
        return true;
      }
      return !item.permission || this.auth.hasPermission(item.permission);
    });
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
    this.collapsedChange.emit(this.collapsed());
  }

  getUserInitials(): string {
    const name = this.auth.user()?.fullName ?? 'U';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }
}
