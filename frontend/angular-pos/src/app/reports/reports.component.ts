import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ReportsService,
  SalesSummary,
  ProductSaleItem,
  ProfitLoss,
  CashierSummary,
  WaiterPerformance,
  KitchenPerformance,
  StockReportItem,
  ReportFilter
} from '../core/services/reports.service';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';
import { UserService, Employee } from '../core/services/user.service';
import { InventoryService, Warehouse } from '../core/services/inventory.service';
import { NotificationService } from '../core/services/notification.service';

type ReportTab = 'sales' | 'products' | 'profit' | 'cashier' | 'waiters' | 'kitchens' | 'stock';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="reports-page fade-in">
      <!-- HEADER -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">📈 Tizim Hisobotlari & Analitika</h1>
          <p class="page-subtitle">PostgreSQL bazasidan real-time hisoblangan savdo, mahsulotlar, oshxona va moliya hisobotlari</p>
        </div>
        <div class="header-actions">
          <button class="pos-btn pos-btn--secondary" (click)="loadCurrentReport()">
            <span>🔄 Yangilash</span>
          </button>
          <button class="pos-btn pos-btn--primary" (click)="exportReportCsv()">
            <span>📥 CSV Eksport</span>
          </button>
        </div>
      </div>

      <!-- FILTER BAR -->
      <div class="pos-card filter-bar">
        <div class="filter-presets">
          <button class="preset-btn" [class.active]="selectedPreset === 'today'" (click)="setPreset('today')">Bugun</button>
          <button class="preset-btn" [class.active]="selectedPreset === 'yesterday'" (click)="setPreset('yesterday')">Kecha</button>
          <button class="preset-btn" [class.active]="selectedPreset === 'week'" (click)="setPreset('week')">Bu hafta</button>
          <button class="preset-btn" [class.active]="selectedPreset === 'month'" (click)="setPreset('month')">Bu oy</button>
          <button class="preset-btn" [class.active]="selectedPreset === 'custom'" (click)="selectedPreset = 'custom'">Boshqa sana</button>
        </div>

        <div class="filter-inputs">
          <div class="date-range-group" *ngIf="selectedPreset === 'custom'">
            <input type="date" class="pos-input" [(ngModel)]="filters.dateFrom" (change)="onFilterChange()" />
            <span class="range-sep">—</span>
            <input type="date" class="pos-input" [(ngModel)]="filters.dateTo" (change)="onFilterChange()" />
          </div>

          <select class="pos-select" [(ngModel)]="filters.kitchenId" (change)="onFilterChange()" *ngIf="activeTab === 'sales' || activeTab === 'products' || activeTab === 'kitchens'">
            <option value="">Barcha oshxonalar</option>
            <option *ngFor="let k of kitchens" [value]="k.id">{{ k.name }}</option>
          </select>

          <select class="pos-select" [(ngModel)]="filters.waiterId" (change)="onFilterChange()" *ngIf="activeTab === 'sales' || activeTab === 'waiters'">
            <option value="">Barcha ofitsiantlar</option>
            <option *ngFor="let w of waiters" [value]="w.id">{{ w.firstName }} {{ w.lastName || '' }}</option>
          </select>

          <select class="pos-select" [(ngModel)]="filters.warehouseId" (change)="onFilterChange()" *ngIf="activeTab === 'stock'">
            <option value="">Barcha omborlar</option>
            <option *ngFor="let wh of warehouses" [value]="wh.id">{{ wh.name }}</option>
          </select>
        </div>
      </div>

      <!-- TABS -->
      <div class="sub-nav-tabs">
        <button class="tab-btn" [class.active]="activeTab === 'sales'" (click)="setTab('sales')">
          <span>📊 Savdo (Umumiy)</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'products'" (click)="setTab('products')">
          <span>🍔 Mahsulotlar Savdosi</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'profit'" (click)="setTab('profit')">
          <span>💵 Foyda & Zarar (P&L)</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'cashier'" (click)="setTab('cashier')">
          <span>🏧 Kassa & To'lovlar</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'waiters'" (click)="setTab('waiters')">
          <span>🤵 Ofitsiantlar</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'kitchens'" (click)="setTab('kitchens')">
          <span>🍳 Oshxonalar</span>
        </button>
        <!-- Ombor hozircha disable:
        <button class="tab-btn" [class.active]="activeTab === 'stock'" (click)="setTab('stock')">
          <span>📦 Ombor Harakati</span>
        </button>
        -->
      </div>

      <!-- ======================================================== -->
      <!-- TAB 1: SAVDO (SALES SUMMARY) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'sales'" class="tab-content">
        <!-- KPI METRICS -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--green">💰</div>
            <div class="kpi-info">
              <span class="kpi-label">Jami Savdo</span>
              <h3 class="kpi-value text-success">{{ (salesSummary?.totalSales || 0) | number }} so'm</h3>
              <span class="kpi-sub">Barcha to'langan buyurtmalar</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--blue">🧾</div>
            <div class="kpi-info">
              <span class="kpi-label">Buyurtmalar Soni</span>
              <h3 class="kpi-value">{{ salesSummary?.totalOrders || 0 }} ta</h3>
              <span class="kpi-sub">O'rtacha chek: {{ (salesSummary?.avgCheck || 0) | number }} so'm</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--teal">💵</div>
            <div class="kpi-info">
              <span class="kpi-label">Naqd To'lovlar</span>
              <h3 class="kpi-value">{{ (salesSummary?.cashTotal || 0) | number }} so'm</h3>
              <span class="kpi-sub">Kassa naqd puli</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--purple">💳</div>
            <div class="kpi-info">
              <span class="kpi-label">Karta To'lovlar</span>
              <h3 class="kpi-value">{{ (salesSummary?.cardTotal || 0) | number }} so'm</h3>
              <span class="kpi-sub">Terminal / Humo / Uzcard</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--orange">❌</div>
            <div class="kpi-info">
              <span class="kpi-label">Bekor Qilinganlar</span>
              <h3 class="kpi-value text-warning">{{ salesSummary?.cancelledOrdersCount || 0 }} ta</h3>
              <span class="kpi-sub">Bekor qilingan buyurtmalar</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--red">↩️</div>
            <div class="kpi-info">
              <span class="kpi-label">Qaytarilgan Summa</span>
              <h3 class="kpi-value text-danger">{{ (salesSummary?.refundedAmount || 0) | number }} so'm</h3>
              <span class="kpi-sub">{{ salesSummary?.refundedOrdersCount || 0 }} ta qaytarish</span>
            </div>
          </div>
        </div>

        <!-- CHARTS SECTION -->
        <div class="charts-grid">
          <!-- SOATLIK SAVDO GRAFIKI -->
          <div class="pos-card chart-card">
            <h3 class="chart-title">🕒 Soatlik Savdo Taqsimoti</h3>
            <div class="hourly-chart-container">
              <div class="hourly-bar-col" *ngFor="let h of salesSummary?.hourlySales">
                <div class="bar-wrapper">
                  <div class="bar-fill" [style.height.%]="getHourlyBarHeight(h.revenue)"></div>
                </div>
                <span class="hour-label">{{ h.hour }}</span>
              </div>
            </div>
          </div>

          <!-- TO'LOV TURLARI TAQSIMOTI -->
          <div class="pos-card chart-card">
            <h3 class="chart-title">💳 To'lov Turlari Nisbati</h3>
            <div class="payment-distribution">
              <div class="payment-dist-item">
                <div class="dist-row">
                  <span>💵 Naqd Pul</span>
                  <strong>{{ (salesSummary?.cashTotal || 0) | number }} so'm ({{ getPaymentPercent('cash') }}%)</strong>
                </div>
                <div class="progress-track">
                  <div class="progress-fill fill--teal" [style.width.%]="getPaymentPercent('cash')"></div>
                </div>
              </div>

              <div class="payment-dist-item">
                <div class="dist-row">
                  <span>💳 Bank Kartasi</span>
                  <strong>{{ (salesSummary?.cardTotal || 0) | number }} so'm ({{ getPaymentPercent('card') }}%)</strong>
                </div>
                <div class="progress-track">
                  <div class="progress-fill fill--purple" [style.width.%]="getPaymentPercent('card')"></div>
                </div>
              </div>

              <div class="payment-dist-item">
                <div class="dist-row">
                  <span>🌐 Boshqa / Online</span>
                  <strong>{{ (salesSummary?.otherTotal || 0) | number }} so'm ({{ getPaymentPercent('other') }}%)</strong>
                </div>
                <div class="progress-track">
                  <div class="progress-fill fill--blue" [style.width.%]="getPaymentPercent('other')"></div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 2: MAHSULOTLAR (PRODUCT SALES) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'products'" class="tab-content">
        <!-- TOP 3 DISHES SUMMARY -->
        <div class="top-dishes-banner mb-4" *ngIf="productSales.length > 0">
          <div class="top-dish-card" *ngFor="let top of productSales.slice(0, 3); let i = index">
            <span class="medal-icon">{{ i === 0 ? '🥇' : (i === 1 ? '🥈' : '🥉') }}</span>
            <div>
              <h4>{{ top.productName }}</h4>
              <p>{{ top.quantity | number:'1.0-2' }} dona sotilgan &bull; {{ top.revenue | number }} so'm</p>
            </div>
            <span class="badge badge--success">Foyda: {{ top.profit | number }} so'm</span>
          </div>
        </div>

        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Taom Nomi</th>
                <th>Kategoriya</th>
                <th>Sotilgan Miqdor</th>
                <th>Tushum (Revenue)</th>
                <th>Tannarx (Cost)</th>
                <th>Foyda (Profit)</th>
                <th>Marja %</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of productSales; let idx = index">
                <td>{{ idx + 1 }}</td>
                <td><strong>{{ p.productName }}</strong></td>
                <td><span class="category-pill">{{ p.categoryName || 'Boshqa' }}</span></td>
                <td><strong class="text-primary">{{ p.quantity | number:'1.0-2' }} dona</strong></td>
                <td>{{ p.revenue | number }} so'm</td>
                <td class="text-muted">{{ p.cost | number }} so'm</td>
                <td class="text-success font-bold">{{ p.profit | number }} so'm</td>
                <td>
                  <span class="badge" [class.badge--success]="p.profitMargin >= 50" [class.badge--warning]="p.profitMargin < 50">
                    {{ p.profitMargin | number:'1.1-1' }}%
                  </span>
                </td>
              </tr>
              <tr *ngIf="productSales.length === 0">
                <td colspan="8" class="text-center py-4 text-muted">Ushbu davrda savdo ma'lumotlari mavjud emas</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 3: FOYDA & ZARAR (PROFIT & LOSS) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'profit'" class="tab-content">
        <div class="profit-formula-card pos-card mb-4">
          <h2 class="formula-title">📐 Foyda Formulatsiyasi (P&L)</h2>
          <div class="formula-steps">
            <div class="formula-block">
              <span class="formula-lbl">Jami Tushum (Revenue)</span>
              <h3 class="text-primary">+ {{ (profitLoss?.totalRevenue || 0) | number }} so'm</h3>
            </div>
            <span class="formula-op">-</span>
            <div class="formula-block">
              <span class="formula-lbl">Mahsulot Tannarxi (Cost)</span>
              <h3 class="text-danger">- {{ (profitLoss?.totalProductCost || 0) | number }} so'm</h3>
            </div>
            <span class="formula-op">-</span>
            <div class="formula-block">
              <span class="formula-lbl">Chegirmalar (Discount)</span>
              <h3 class="text-warning">- {{ (profitLoss?.totalDiscounts || 0) | number }} so'm</h3>
            </div>
            <span class="formula-op">-</span>
            <div class="formula-block">
              <span class="formula-lbl">Qaytarishlar (Refund)</span>
              <h3 class="text-danger">- {{ (profitLoss?.totalRefunds || 0) | number }} so'm</h3>
            </div>
            <span class="formula-op">=</span>
            <div class="formula-block highlight">
              <span class="formula-lbl">Sof Yalpi Foyda (Gross Profit)</span>
              <h2 class="text-success">{{ (profitLoss?.grossProfit || 0) | number }} so'm</h2>
              <span class="badge badge--success">Rentabellik: {{ (profitLoss?.profitMargin || 0) | number:'1.1-1' }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 4: KASSA (CASHIER & REGISTER) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'cashier'" class="tab-content">
        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Kassir Ismi</th>
                <th>Buyurtmalar Soni</th>
                <th>Naqd Savdo</th>
                <th>Karta Savdo</th>
                <th>Boshqa Savdo</th>
                <th>Jami Savdo</th>
                <th>Qaytarilgan</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let c of cashierSummaries">
                <td><strong>{{ c.cashierName }}</strong></td>
                <td>{{ c.ordersCount }} ta</td>
                <td>{{ c.cashSales | number }} so'm</td>
                <td>{{ c.cardSales | number }} so'm</td>
                <td>{{ c.otherSales | number }} so'm</td>
                <td><strong class="text-success">{{ c.totalSales | number }} so'm</strong></td>
                <td class="text-danger">{{ c.totalRefunds | number }} so'm</td>
              </tr>
              <tr *ngIf="cashierSummaries.length === 0">
                <td colspan="7" class="text-center py-4 text-muted">Kassa operatsiyalari topilmadi</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 5: OFITSIANTLAR (WAITER PERFORMANCE) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'waiters'" class="tab-content">
        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Ofitsiant</th>
                <th>Buyurtmalar Soni</th>
                <th>Jami Savdo</th>
                <th>O'rtacha Chek</th>
                <th>Yetkazilgan Taomlar</th>
                <th>Bekor Qilingan Taomlar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let w of waiterReports">
                <td><strong>{{ w.waiterName }}</strong></td>
                <td><strong>{{ w.ordersCount }} ta</strong></td>
                <td class="text-success font-bold">{{ w.totalSales | number }} so'm</td>
                <td>{{ w.avgCheck | number }} so'm</td>
                <td><span class="badge badge--success">{{ w.deliveredCount }} ta</span></td>
                <td><span class="badge badge--danger" *ngIf="w.cancelledCount > 0">{{ w.cancelledCount }} ta</span><span *ngIf="w.cancelledCount === 0">0</span></td>
              </tr>
              <tr *ngIf="waiterReports.length === 0">
                <td colspan="6" class="text-center py-4 text-muted">Ofitsiant savdolari topilmadi</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 6: OSHXONALAR (KITCHEN PERFORMANCE) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'kitchens'" class="tab-content">
        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Oshxona Stansiyasi</th>
                <th>Buyurtmalar Soni</th>
                <th>Tayyorlangan Taomlar</th>
                <th>Bekor Qilinganlar</th>
                <th>Oshxona Savdosi (Tushum)</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let k of kitchenReports">
                <td><strong>{{ k.kitchenName }}</strong></td>
                <td><strong>{{ k.ordersCount }} ta</strong></td>
                <td><span class="badge badge--success">{{ k.itemsPrepared | number:'1.0-2' }} dona</span></td>
                <td>
                  <span class="badge badge--danger" *ngIf="k.itemsCancelled > 0">{{ k.itemsCancelled | number:'1.0-2' }} dona</span>
                  <span *ngIf="k.itemsCancelled === 0">0</span>
                </td>
                <td><strong class="text-primary">{{ k.revenue | number }} so'm</strong></td>
              </tr>
              <tr *ngIf="kitchenReports.length === 0">
                <td colspan="5" class="text-center py-4 text-muted">Oshxona ma'lumotlari topilmadi</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 7: OMBOR HARAKATI (STOCK BALANCE) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'stock'" class="tab-content">
        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Mahsulot</th>
                <th>Birlik</th>
                <th>Ombor</th>
                <th>Boshlang'ich</th>
                <th>Kirim</th>
                <th>Chiqim</th>
                <th>Savdo Sarfi</th>
                <th>Isrof</th>
                <th>Tuzatish</th>
                <th>Yakuniy Qoldiq</th>
                <th>Tannarx</th>
                <th>Ombor Qiymati</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let s of stockReports">
                <td><strong>{{ s.itemName }}</strong></td>
                <td>{{ s.unit }}</td>
                <td>{{ s.warehouseName }}</td>
                <td>{{ s.openingStock | number:'1.1-3' }}</td>
                <td class="text-success font-bold">+{{ s.incoming | number:'1.1-3' }}</td>
                <td class="text-danger">-{{ s.outgoing | number:'1.1-3' }}</td>
                <td class="text-warning">-{{ s.salesConsumption | number:'1.1-3' }}</td>
                <td class="text-danger">-{{ s.waste | number:'1.1-3' }}</td>
                <td [class.text-success]="s.adjustment > 0" [class.text-danger]="s.adjustment < 0">
                  {{ s.adjustment > 0 ? '+' : '' }}{{ s.adjustment | number:'1.1-3' }}
                </td>
                <td><strong class="stock-amount">{{ s.closingStock | number:'1.1-3' }} {{ s.unit }}</strong></td>
                <td>{{ s.unitCost | number }} so'm</td>
                <td><strong>{{ s.totalValuation | number }} so'm</strong></td>
              </tr>
              <tr *ngIf="stockReports.length === 0">
                <td colspan="12" class="text-center py-4 text-muted">Zaxira hisoboti topilmadi</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .reports-page {
      padding: 0;
      color: var(--text-primary);
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title {
      font-size: 24px;
      font-weight: 800;
      margin: 0 0 4px 0;
      letter-spacing: -0.5px;
    }
    .page-subtitle {
      font-size: 14px;
      color: var(--text-muted);
      margin: 0;
    }
    .header-actions {
      display: flex;
      gap: 12px;
    }
    .filter-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      padding: 14px 18px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      border-radius: 12px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
    }
    .filter-presets {
      display: flex;
      gap: 6px;
    }
    .preset-btn {
      padding: 6px 14px;
      border: 1px solid var(--border-color);
      background: rgba(255, 255, 255, 0.03);
      color: var(--text-muted);
      font-size: 13px;
      font-weight: 600;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
    }
    .preset-btn:hover {
      color: var(--text-primary);
      background: rgba(255, 255, 255, 0.08);
    }
    .preset-btn.active {
      background: var(--primary);
      border-color: var(--primary);
      color: #fff;
    }
    .filter-inputs {
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
    }
    .date-range-group {
      display: flex;
      align-items: center;
      gap: 6px;
    }
    .range-sep {
      color: var(--text-muted);
    }
    .sub-nav-tabs {
      display: flex;
      gap: 6px;
      background: var(--bg-card);
      padding: 6px;
      border-radius: 12px;
      border: 1px solid var(--border-color);
      margin-bottom: 24px;
      overflow-x: auto;
    }
    .tab-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border: none;
      background: transparent;
      color: var(--text-muted);
      font-size: 14px;
      font-weight: 600;
      border-radius: 8px;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
    }
    .tab-btn:hover {
      color: var(--text-primary);
      background: rgba(255, 255, 255, 0.05);
    }
    .tab-btn.active {
      background: var(--primary);
      color: #fff;
    }
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }
    .kpi-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 14px;
      padding: 18px;
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .kpi-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      background: rgba(255, 255, 255, 0.05);
    }
    .kpi-value {
      font-size: 19px;
      font-weight: 800;
      margin: 4px 0 2px 0;
    }
    .kpi-label {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 700;
    }
    .kpi-sub {
      font-size: 12px;
      color: var(--text-muted);
    }
    .charts-grid {
      display: grid;
      grid-template-columns: 1.5fr 1fr;
      gap: 20px;
    }
    .chart-card {
      padding: 20px;
      border-radius: 14px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
    }
    .chart-title {
      font-size: 16px;
      font-weight: 700;
      margin: 0 0 16px 0;
    }
    .hourly-chart-container {
      display: flex;
      align-items: flex-end;
      height: 180px;
      gap: 4px;
      padding-top: 10px;
      border-bottom: 1px solid var(--border-color);
    }
    .hourly-bar-col {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;
      height: 100%;
    }
    .bar-wrapper {
      flex: 1;
      width: 100%;
      display: flex;
      align-items: flex-end;
      justify-content: center;
    }
    .bar-fill {
      width: 80%;
      background: var(--primary);
      border-radius: 4px 4px 0 0;
      min-height: 2px;
      transition: height 0.3s ease;
    }
    .bar-fill:hover {
      background: #818cf8;
    }
    .hour-label {
      font-size: 10px;
      color: var(--text-muted);
      margin-top: 6px;
    }
    .payment-distribution {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .dist-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      margin-bottom: 6px;
    }
    .progress-track {
      height: 8px;
      background: rgba(255, 255, 255, 0.05);
      border-radius: 4px;
      overflow: hidden;
    }
    .progress-fill {
      height: 100%;
      border-radius: 4px;
    }
    .fill--teal { background: #10b981; }
    .fill--purple { background: #8b5cf6; }
    .fill--blue { background: #3b82f6; }
    .top-dishes-banner {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 16px;
    }
    .top-dish-card {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 16px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: 12px;
    }
    .top-dish-card h4 {
      margin: 0 0 4px 0;
    }
    .top-dish-card p {
      margin: 0;
      font-size: 12px;
      color: var(--text-muted);
    }
    .medal-icon {
      font-size: 28px;
    }
    .profit-formula-card {
      padding: 24px;
      border-radius: 16px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
    }
    .formula-title {
      font-size: 18px;
      font-weight: 800;
      margin: 0 0 20px 0;
    }
    .formula-steps {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }
    .formula-block {
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      padding: 16px 20px;
      border-radius: 12px;
      min-width: 180px;
    }
    .formula-block.highlight {
      background: rgba(16, 185, 129, 0.08);
      border-color: rgba(16, 185, 129, 0.3);
    }
    .formula-lbl {
      font-size: 12px;
      color: var(--text-muted);
      text-transform: uppercase;
      font-weight: 700;
      display: block;
      margin-bottom: 6px;
    }
    .formula-op {
      font-size: 26px;
      font-weight: 800;
      color: var(--text-muted);
    }
    .table-card {
      overflow-x: auto;
      border: 1px solid var(--border-color);
      border-radius: 14px;
      background: var(--bg-card);
    }
    .pos-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
    }
    .pos-table th {
      background: rgba(0, 0, 0, 0.2);
      color: var(--text-muted);
      font-weight: 700;
      text-transform: uppercase;
      font-size: 11px;
      letter-spacing: 0.5px;
      padding: 12px 16px;
      text-align: left;
      border-bottom: 1px solid var(--border-color);
    }
    .pos-table td {
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-color);
      color: var(--text-primary);
    }
    .pos-table tr:last-child td {
      border-bottom: none;
    }
    .text-success { color: #10b981; }
    .text-warning { color: #f59e0b; }
    .text-danger { color: #ef4444; }
    .text-primary { color: var(--primary); }
    .text-muted { color: var(--text-muted); }
    .font-bold { font-weight: 700; }
  `]
})
export class ReportsComponent implements OnInit {
  activeTab: ReportTab = 'sales';
  selectedPreset: 'today' | 'yesterday' | 'week' | 'month' | 'custom' = 'today';

  filters: ReportFilter = {};

  salesSummary: SalesSummary | null = null;
  productSales: ProductSaleItem[] = [];
  profitLoss: ProfitLoss | null = null;
  cashierSummaries: CashierSummary[] = [];
  waiterReports: WaiterPerformance[] = [];
  kitchenReports: KitchenPerformance[] = [];
  stockReports: StockReportItem[] = [];

  kitchens: KitchenStation[] = [];
  waiters: Employee[] = [];
  warehouses: Warehouse[] = [];

  constructor(
    private reportsService: ReportsService,
    private kitchenService: KitchenService,
    private userService: UserService,
    private inventoryService: InventoryService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.setPreset('today');
    this.loadFilterLookups();
  }

  setPreset(preset: 'today' | 'yesterday' | 'week' | 'month' | 'custom'): void {
    this.selectedPreset = preset;
    const now = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    const toDateStr = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

    if (preset === 'today') {
      const todayStr = toDateStr(now);
      this.filters.dateFrom = todayStr;
      this.filters.dateTo = todayStr;
    } else if (preset === 'yesterday') {
      const y = new Date();
      y.setDate(y.getDate() - 1);
      const yStr = toDateStr(y);
      this.filters.dateFrom = yStr;
      this.filters.dateTo = yStr;
    } else if (preset === 'week') {
      const d = new Date();
      const day = d.getDay();
      const diff = d.getDate() - day + (day === 0 ? -6 : 1); // Monday
      const monday = new Date(d.setDate(diff));
      this.filters.dateFrom = toDateStr(monday);
      this.filters.dateTo = toDateStr(now);
    } else if (preset === 'month') {
      const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
      this.filters.dateFrom = toDateStr(firstDay);
      this.filters.dateTo = toDateStr(now);
    }

    this.loadCurrentReport();
  }

  setTab(tab: ReportTab): void {
    this.activeTab = tab;
    this.loadCurrentReport();
  }

  onFilterChange(): void {
    this.loadCurrentReport();
  }

  loadFilterLookups(): void {
    this.kitchenService.getKitchens().subscribe({
      next: res => { this.kitchens = res.data || []; this.cdr.markForCheck(); }
    });
    this.userService.getUsers().subscribe({
      next: res => {
        this.waiters = (res.data || []).filter(u => u.role === 'WAITER');
        this.cdr.markForCheck();
      }
    });
    this.inventoryService.getWarehouses().subscribe({
      next: res => { this.warehouses = res.data || []; this.cdr.markForCheck(); }
    });
  }

  loadCurrentReport(): void {
    if (this.activeTab === 'sales') {
      this.reportsService.getSalesSummary(this.filters).subscribe({
        next: res => { this.salesSummary = res.data; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'products') {
      this.reportsService.getProductSales(this.filters).subscribe({
        next: res => { this.productSales = res.data || []; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'profit') {
      this.reportsService.getProfitLoss(this.filters).subscribe({
        next: res => { this.profitLoss = res.data; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'cashier') {
      this.reportsService.getCashierReport(this.filters).subscribe({
        next: res => { this.cashierSummaries = res.data || []; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'waiters') {
      this.reportsService.getWaiterReport(this.filters).subscribe({
        next: res => { this.waiterReports = res.data || []; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'kitchens') {
      this.reportsService.getKitchenReport(this.filters).subscribe({
        next: res => { this.kitchenReports = res.data || []; this.cdr.markForCheck(); }
      });
    } else if (this.activeTab === 'stock') {
      this.reportsService.getStockReport(this.filters).subscribe({
        next: res => { this.stockReports = res.data || []; this.cdr.markForCheck(); }
      });
    }
  }

  getHourlyBarHeight(rev: number): number {
    if (!this.salesSummary?.hourlySales || this.salesSummary.hourlySales.length === 0) return 2;
    const max = Math.max(...this.salesSummary.hourlySales.map(h => h.revenue), 1);
    const pct = Math.round((rev / max) * 100);
    return Math.max(pct, 2);
  }

  getPaymentPercent(type: 'cash' | 'card' | 'other'): number {
    if (!this.salesSummary || this.salesSummary.totalSales <= 0) return 0;
    const total = this.salesSummary.totalSales;
    let val = 0;
    if (type === 'cash') val = this.salesSummary.cashTotal || 0;
    if (type === 'card') val = this.salesSummary.cardTotal || 0;
    if (type === 'other') val = this.salesSummary.otherTotal || 0;
    return Math.round((val / total) * 100);
  }

  exportReportCsv(): void {
    const typeMap: { [k in ReportTab]: string } = {
      sales: 'SALES',
      products: 'PRODUCTS',
      profit: 'SALES',
      cashier: 'SALES',
      waiters: 'WAITERS',
      kitchens: 'KITCHEN',
      stock: 'STOCK'
    };
    const reportType = typeMap[this.activeTab];

    this.reportsService.downloadCsv(reportType, this.filters).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `hisobot_${reportType.toLowerCase()}_${this.filters.dateFrom || 'today'}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success("Hisobot CSV fayli yuklab olindi!");
      },
      error: () => {
        this.notificationService.error("CSV eksportda xatolik yuz berdi");
      }
    });
  }
}
