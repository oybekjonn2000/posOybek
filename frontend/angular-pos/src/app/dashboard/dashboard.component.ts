import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { OrderService, Order } from '../core/services/order.service';
import { TableService, RestaurantTable } from '../core/services/table.service';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="dashboard-page fade-in">
      <!-- Welcome Header -->
      <div class="welcome-header">
        <div>
          <h1 class="welcome-title">Xush kelibsiz, {{ currentUserName }}! 👋</h1>
          <p class="welcome-subtitle">Bugungi restoran faoliyati va asosiy ko'rsatkichlar monitoringi</p>
        </div>

        <div class="header-right">
          <span class="role-badge">{{ currentUserRole }}</span>
          <button class="pos-btn pos-btn--secondary" (click)="loadDashboardData()" [disabled]="loading">
            <span [class.spinning]="loading">🔄</span>
            <span>Yangilash</span>
          </button>
        </div>
      </div>

      <!-- KPI Cards Grid -->
      <div class="kpi-grid">
        <!-- KPI 1: Today's Revenue -->
        <div class="kpi-card kpi-card--revenue">
          <div class="kpi-header">
            <span class="kpi-title">Bugungi Tushum</span>
            <div class="kpi-icon">💰</div>
          </div>
          <div class="kpi-value">{{ totalRevenue | number:'1.0-0' }} <small>so'm</small></div>
          <div class="kpi-footer">
            <span class="kpi-subtext">To'langan {{ paidOrdersCount }} ta buyurtma bo'yicha</span>
          </div>
        </div>

        <!-- KPI 2: Active Orders -->
        <div class="kpi-card kpi-card--orders">
          <div class="kpi-header">
            <span class="kpi-title">Faol Buyurtmalar</span>
            <div class="kpi-icon">📋</div>
          </div>
          <div class="kpi-value">{{ activeOrdersCount }} <small>ta</small></div>
          <div class="kpi-footer">
            <span class="kpi-subtext">{{ kitchenOrdersCount }} tasi oshxonada tayyorlanmoqda</span>
          </div>
        </div>

        <!-- KPI 3: Occupied Tables -->
        <div class="kpi-card kpi-card--tables">
          <div class="kpi-header">
            <span class="kpi-title">Band Stollar</span>
            <div class="kpi-icon">🪑</div>
          </div>
          <div class="kpi-value">{{ occupiedTablesCount }} / {{ tables.length }} <small>band</small></div>
          <div class="kpi-footer">
            <span class="kpi-subtext">{{ freeTablesCount }} ta stol bo'sh</span>
          </div>
        </div>

        <!-- KPI 4: Ready Orders -->
        <div class="kpi-card kpi-card--ready">
          <div class="kpi-header">
            <span class="kpi-title">Tayyor Buyurtmalar</span>
            <div class="kpi-icon">✅</div>
          </div>
          <div class="kpi-value">{{ readyOrdersCount }} <small>ta</small></div>
          <div class="kpi-footer">
            <span class="kpi-subtext">Yetkazishga tayyor holatda</span>
          </div>
        </div>
      </div>

      <!-- Quick Action Shortcuts -->
      <div class="quick-actions-bar">
        <a routerLink="/pos" class="action-card action-card--pos">
          <span class="act-icon">➕</span>
          <div class="act-info">
            <strong>Yangi Buyurtma</strong>
            <span>POS terminalni ochish</span>
          </div>
        </a>

        <a routerLink="/tables" class="action-card action-card--tables">
          <span class="act-icon">🪑</span>
          <div class="act-info">
            <strong>Stollar Rejasi</strong>
            <span>Zallar va stollar holati</span>
          </div>
        </a>

        <a routerLink="/kitchen" class="action-card action-card--kitchen">
          <span class="act-icon">👨‍🍳</span>
          <div class="act-info">
            <strong>Oshxona Ekrani (KDS)</strong>
            <span>Taomlarni pishirish</span>
          </div>
        </a>

        <a routerLink="/orders" class="action-card action-card--cashier">
          <span class="act-icon">💳</span>
          <div class="act-info">
            <strong>Kassa & To'lov</strong>
            <span>Cheklar va to'lovlar</span>
          </div>
        </a>
      </div>

      <!-- Two Column Layout: Recent Orders & Tables Overview -->
      <div class="dashboard-columns">
        <!-- Left: Active Orders List -->
        <div class="pos-card section-card">
          <div class="pos-card__header">
            <h2 class="pos-card__title">🕒 Oxirgi Faol Buyurtmalar</h2>
            <a routerLink="/orders" class="view-all-link">Barchasi →</a>
          </div>

          <div *ngIf="activeOrders.length === 0" class="empty-list">
            <p>Hozirda faol buyurtmalar mavjud emas.</p>
          </div>

          <div *ngIf="activeOrders.length > 0" class="recent-orders-list">
            <div *ngFor="let order of recentActiveOrders" class="order-item-row">
              <div class="row-left">
                <span class="tbl-tag">{{ order.tableName || order.tableNumber || 'Stol' }}</span>
                <div>
                  <strong>#{{ order.orderNumber }}</strong>
                  <div class="meta-sub">{{ order.items?.length || 0 }} xil taom • {{ order.waiterName || 'Ofitsiant' }}</div>
                </div>
              </div>
              <div class="row-right">
                <span class="sum-tag">{{ (order.total || order.subtotal || 0) | number:'1.0-0' }} so'm</span>
                <span class="badge-mini" [ngClass]="order.status?.toLowerCase()">{{ order.status }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Tables Floor Overview -->
        <div class="pos-card section-card">
          <div class="pos-card__header">
            <h2 class="pos-card__title">🪑 Stollar Xaritasi</h2>
            <a routerLink="/tables" class="view-all-link">Boshqarish →</a>
          </div>

          <div class="tables-mini-grid">
            <div
              *ngFor="let t of tables"
              class="mini-table-box"
              [class.occupied]="t.status === 'OCCUPIED'"
              [class.free]="t.status !== 'OCCUPIED'">
              <span class="t-name">{{ t.name || ('Stol ' + t.tableNumber) }}</span>
              <span class="t-status">{{ t.status === 'OCCUPIED' ? 'Band' : 'Bo‘sh' }}</span>
            </div>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .dashboard-page {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .welcome-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 20px 24px;
    }

    .welcome-title {
      font-size: 24px;
      font-weight: 800;
      color: var(--text-primary);
      margin: 0;
    }

    .welcome-subtitle {
      font-size: 13px;
      color: var(--text-muted);
      margin: 4px 0 0 0;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .role-badge {
      background: rgba(99, 102, 241, 0.2);
      color: var(--primary-light);
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 700;
      border: 1px solid rgba(99, 102, 241, 0.4);
    }

    /* KPI Cards */
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 16px;
    }

    .kpi-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
      }

      &--revenue { border-left: 4px solid #10b981; }
      &--orders { border-left: 4px solid #6366f1; }
      &--tables { border-left: 4px solid #f59e0b; }
      &--ready { border-left: 4px solid #3b82f6; }
    }

    .kpi-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .kpi-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .kpi-icon {
      font-size: 24px;
    }

    .kpi-value {
      font-size: 28px;
      font-weight: 800;
      color: var(--text-primary);

      small {
        font-size: 14px;
        color: var(--text-muted);
        font-weight: 500;
      }
    }

    .kpi-footer {
      font-size: 12px;
      color: var(--text-muted);
      border-top: 1px solid var(--divider);
      padding-top: 8px;
    }

    /* Quick Actions */
    .quick-actions-bar {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }

    .action-card {
      display: flex;
      align-items: center;
      gap: 14px;
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      text-decoration: none;
      color: var(--text-primary);
      transition: all var(--transition);

      &:hover {
        background: var(--bg-hover);
        border-color: var(--primary);
        transform: translateY(-2px);
      }

      .act-icon {
        font-size: 28px;
        background: var(--bg-card);
        padding: 10px;
        border-radius: var(--radius-sm);
      }

      .act-info {
        display: flex;
        flex-direction: column;
        strong { font-size: 15px; }
        span { font-size: 12px; color: var(--text-muted); }
      }
    }

    /* Dashboard Columns */
    .dashboard-columns {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 20px;

      @media (max-width: 900px) {
        grid-template-columns: 1fr;
      }
    }

    .section-card {
      padding: 20px;
    }

    .view-all-link {
      font-size: 13px;
      color: var(--primary-light);
      text-decoration: none;
      font-weight: 600;
      &:hover { text-decoration: underline; }
    }

    .recent-orders-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }

    .order-item-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 12px 16px;

      .row-left {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .tbl-tag {
        background: var(--primary);
        color: white;
        font-weight: 700;
        font-size: 12px;
        padding: 4px 8px;
        border-radius: 4px;
      }

      .meta-sub {
        font-size: 12px;
        color: var(--text-muted);
      }

      .row-right {
        display: flex;
        align-items: center;
        gap: 10px;
      }

      .sum-tag {
        font-weight: 700;
        color: #34d399;
        font-size: 14px;
      }

      .badge-mini {
        font-size: 10px;
        padding: 2px 6px;
        border-radius: 4px;
        font-weight: 600;
        text-transform: uppercase;
        background: var(--bg-card);
        color: var(--text-secondary);
      }
    }

    /* Mini Tables Grid */
    .tables-mini-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
      gap: 10px;
    }

    .mini-table-box {
      background: var(--bg-secondary);
      border: 2px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 12px 8px;
      text-align: center;
      display: flex;
      flex-direction: column;
      gap: 4px;

      .t-name {
        font-weight: 700;
        font-size: 13px;
        color: var(--text-primary);
      }

      .t-status {
        font-size: 11px;
        font-weight: 600;
      }

      &.free {
        border-color: rgba(16, 185, 129, 0.4);
        background: rgba(16, 185, 129, 0.05);
        .t-status { color: var(--success); }
      }

      &.occupied {
        border-color: rgba(239, 68, 68, 0.5);
        background: rgba(239, 68, 68, 0.1);
        .t-status { color: #f87171; }
      }
    }

    .empty-list {
      padding: 30px;
      text-align: center;
      color: var(--text-muted);
      font-size: 14px;
    }

    .spinning {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }
  `]
})
export class DashboardComponent implements OnInit {
  orders: Order[] = [];
  tables: RestaurantTable[] = [];
  loading = false;

  constructor(
    private orderService: OrderService,
    private tableService: TableService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  get currentUserName(): string {
    const user = this.authService.getCurrentUser();
    return user?.fullName || user?.username || 'Foydalanuvchi';
  }

  get currentUserRole(): string {
    const user = this.authService.getCurrentUser();
    return user?.role || 'Xodim';
  }

  loadDashboardData(): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.orderService.getActiveOrders().subscribe({
      next: (ordersRes) => {
        this.orders = ordersRes.data || [];
        this.cdr.markForCheck();

        this.tableService.getTables().subscribe({
          next: (tablesRes) => {
            this.tables = tablesRes.data || [];
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Failed to load tables', err);
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get totalRevenue(): number {
    return this.orders
      .filter(o => o.status === 'PAID')
      .reduce((sum, o) => sum + (o.total || o.subtotal || 0), 0);
  }

  get paidOrdersCount(): number {
    return this.orders.filter(o => o.status === 'PAID').length;
  }

  get activeOrders(): Order[] {
    return this.orders.filter(o => o.status !== 'PAID' && o.status !== 'CANCELLED');
  }

  get recentActiveOrders(): Order[] {
    return this.activeOrders.slice(0, 5);
  }

  get activeOrdersCount(): number {
    return this.activeOrders.length;
  }

  get kitchenOrdersCount(): number {
    return this.orders.filter(o => o.status === 'SENT_TO_KITCHEN' || o.status === 'PREPARING').length;
  }

  get readyOrdersCount(): number {
    return this.orders.filter(o => o.status === 'READY').length;
  }

  get occupiedTablesCount(): number {
    return this.tables.filter(t => t.status === 'OCCUPIED').length;
  }

  get freeTablesCount(): number {
    return this.tables.filter(t => t.status !== 'OCCUPIED').length;
  }
}
