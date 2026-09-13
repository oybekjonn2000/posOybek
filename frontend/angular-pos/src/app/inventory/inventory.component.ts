import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import {
  InventoryService,
  InventoryDashboardStats,
  InventoryItem,
  InventoryTransaction,
  PurchaseResponse,
  AuditResponse,
  ProductRecipe,
  Warehouse,
  Supplier
} from '../core/services/inventory.service';
import { ProductService, Product } from '../core/services/product.service';
import { NotificationService } from '../core/services/notification.service';

type InventoryTab = 'dashboard' | 'items' | 'kirim' | 'chiqim' | 'movements' | 'audit' | 'recipes' | 'warehouses';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  template: `
    <div class="inventory-page fade-in">
      <!-- HEADER -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">📦 Ombor & Zaxiralar Tizimi</h1>
          <p class="page-subtitle">Real-time inventory management, kirim-chiqim, retseptlar va inventarizatsiya</p>
        </div>
        <div class="header-actions">
          <button class="pos-btn pos-btn--secondary" (click)="loadAllData()">
            <span>🔄 Yangilash</span>
          </button>
          <button class="pos-btn pos-btn--primary" (click)="openCreateItemModal()">
            <span>➕ Yangi Mahsulot</span>
          </button>
        </div>
      </div>

      <!-- NAVIGATION TABS -->
      <div class="sub-nav-tabs">
        <button class="tab-btn" [class.active]="activeTab === 'dashboard'" (click)="setTab('dashboard')">
          <span>📊 Dashboard</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'items'" (click)="setTab('items')">
          <span>📦 Mahsulotlar</span>
          <span class="badge badge--pill" *ngIf="items.length">{{ items.length }}</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'kirim'" (click)="setTab('kirim')">
          <span>📥 Kirim (Xaridlar)</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'chiqim'" (click)="setTab('chiqim')">
          <span>📤 Chiqim (Chiqindilar)</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'movements'" (click)="setTab('movements')">
          <span>📜 Harakatlar Tarixi</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'audit'" (click)="setTab('audit')">
          <span>📋 Inventarizatsiya</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'recipes'" (click)="setTab('recipes')">
          <span>🍕 Retseptlar (Tannarx)</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab === 'warehouses'" (click)="setTab('warehouses')">
          <span>🏢 Omborlar & Ta'minotchilar</span>
        </button>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 1: DASHBOARD -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'dashboard'" class="tab-content">
        <!-- KPI METRICS -->
        <div class="kpi-grid">
          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--blue">📦</div>
            <div class="kpi-info">
              <span class="kpi-label">Jami Mahsulotlar</span>
              <h3 class="kpi-value">{{ stats?.totalProducts || 0 }} xil</h3>
              <span class="kpi-sub">Umumiy hajm: {{ (stats?.totalStockQuantity || 0) | number:'1.1-2' }} birlik</span>
            </div>
          </div>

          <div class="kpi-card" [class.warning]="(stats?.lowStockCount || 0) > 0">
            <div class="kpi-icon kpi-icon--orange">⚠️</div>
            <div class="kpi-info">
              <span class="kpi-label">Kam Qolganlar</span>
              <h3 class="kpi-value text-warning">{{ stats?.lowStockCount || 0 }} xil</h3>
              <span class="kpi-sub">Minimal me'yordan past</span>
            </div>
          </div>

          <div class="kpi-card" [class.danger]="(stats?.outOfStockCount || 0) > 0">
            <div class="kpi-icon kpi-icon--red">🚫</div>
            <div class="kpi-info">
              <span class="kpi-label">Tugagan Mahsulotlar</span>
              <h3 class="kpi-value text-danger">{{ stats?.outOfStockCount || 0 }} xil</h3>
              <span class="kpi-sub">Qoldiq 0 ga teng</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--green">💰</div>
            <div class="kpi-info">
              <span class="kpi-label">Ombor Umumiy Qiymati</span>
              <h3 class="kpi-value text-success">{{ (stats?.warehouseValuation || 0) | number }} so'm</h3>
              <span class="kpi-sub">Tannarx bo'yicha jami aktiv</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--teal">📥</div>
            <div class="kpi-info">
              <span class="kpi-label">Bugungi Kirim</span>
              <h3 class="kpi-value">{{ (stats?.todayIncomingAmount || 0) | number }} so'm</h3>
              <span class="kpi-sub">Bugun qabul qilingan tovarlar</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--purple">📤</div>
            <div class="kpi-info">
              <span class="kpi-label">Bugungi Chiqim</span>
              <h3 class="kpi-value">{{ (stats?.todayOutgoingAmount || 0) | number }} so'm</h3>
              <span class="kpi-sub">Oshxona va boshqa maqsadlarga</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--indigo">🍽️</div>
            <div class="kpi-info">
              <span class="kpi-label">Bugungi Retsept Sarfi</span>
              <h3 class="kpi-value">{{ (stats?.todaySalesConsumptionAmount || 0) | number }} so'm</h3>
              <span class="kpi-sub">Savdo buyurtmalaridan avtomatik</span>
            </div>
          </div>

          <div class="kpi-card">
            <div class="kpi-icon kpi-icon--gray">⚖️</div>
            <div class="kpi-info">
              <span class="kpi-label">Inventarizatsiya Farqi</span>
              <h3 class="kpi-value" [class.text-danger]="(stats?.recentDiscrepancyAmount || 0) < 0">
                {{ (stats?.recentDiscrepancyAmount || 0) | number }} so'm
              </h3>
              <span class="kpi-sub">Oxirgi sanoq natijasi</span>
            </div>
          </div>
        </div>

        <!-- QUICK ACTIONS & LOW STOCK ALERTS -->
        <div class="dashboard-grid">
          <div class="pos-card quick-actions-panel">
            <h3 class="panel-title">⚡ Tezkor Operatsiyalar</h3>
            <div class="actions-buttons-grid">
              <button class="action-tile" (click)="openPurchaseModal()">
                <span class="tile-icon">📥</span>
                <span class="tile-title">Yangi Kirim Qilish</span>
                <span class="tile-desc">Yetkazib beruvchidan tovar qabul qilish</span>
              </button>
              <button class="action-tile" (click)="openOutboundModal()">
                <span class="tile-icon">📤</span>
                <span class="tile-title">Chiqim / Isrof</span>
                <span class="tile-desc">Oshxonaga berish yoki chiqit qilish</span>
              </button>
              <button class="action-tile" (click)="setTab('audit')">
                <span class="tile-icon">📋</span>
                <span class="tile-title">Inventarizatsiya</span>
                <span class="tile-desc">Haqiqiy va tizim qoldiqlarini solishtirish</span>
              </button>
              <button class="action-tile" (click)="setTab('recipes')">
                <span class="tile-icon">🍕</span>
                <span class="tile-title">Taom Retseptlari</span>
                <span class="tile-desc">Ingredientlar va tannarx kalkulyatsiyasi</span>
              </button>
            </div>
          </div>

          <div class="pos-card alert-panel">
            <div class="panel-header-row">
              <h3 class="panel-title">⚠️ Diqqat Talab Mahsulotlar</h3>
              <span class="badge badge--warning" *ngIf="lowStockItems.length">{{ lowStockItems.length }} ta</span>
            </div>

            <div class="alert-list" *ngIf="lowStockItems.length > 0; else allStockOk">
              <div class="alert-item" *ngFor="let item of lowStockItems">
                <div class="alert-item-info">
                  <strong>{{ item.name }}</strong>
                  <span class="alert-sub">{{ item.category || 'Umumiy' }} &bull; Ombor: {{ item.warehouseName }}</span>
                </div>
                <div class="alert-item-stock">
                  <span class="stock-badge" [class.danger]="item.quantity <= 0" [class.warning]="item.quantity > 0">
                    {{ item.quantity }} {{ item.unit }}
                  </span>
                  <small class="min-label">Min: {{ item.minQuantity }} {{ item.unit }}</small>
                </div>
                <button class="pos-btn pos-btn--sm pos-btn--primary" (click)="quickKirim(item)">+ Kirim</button>
              </div>
            </div>
            <ng-template #allStockOk>
              <div class="empty-state-mini">
                <span>✅ Barcha mahsulotlar yetarli miqdorda mavjud</span>
              </div>
            </ng-template>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 2: MAHSULOTLAR (ITEMS CATALOG) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'items'" class="tab-content">
        <div class="table-toolbar">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input type="text" class="pos-input" placeholder="Mahsulot nomi yoki SKU bo'yicha qidirish..." [(ngModel)]="itemSearchQuery" (ngModelChange)="itemPageIndex = 0" />
          </div>

          <div class="filter-controls">
            <select class="pos-select" [(ngModel)]="selectedWarehouseFilter" (change)="loadItems(); itemPageIndex = 0">
              <option value="">Barcha omborlar</option>
              <option *ngFor="let w of warehouses" [value]="w.id">{{ w.name }}</option>
            </select>

            <select class="pos-select" [(ngModel)]="selectedCategoryFilter" (change)="loadItems(); itemPageIndex = 0">
              <option value="">Barcha kategoriyalar</option>
              <option *ngFor="let cat of itemCategories" [value]="cat">{{ cat }}</option>
            </select>

            <label class="checkbox-toggle">
              <input type="checkbox" [(ngModel)]="onlyLowStockFilter" (change)="loadItems(); itemPageIndex = 0" />
              <span>Faqat kam qolganlar</span>
            </label>
          </div>
        </div>

        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>SKU / Kod</th>
                <th>Mahsulot Nomi</th>
                <th>Kategoriya</th>
                <th>Ombor</th>
                <th>O'lchov Birligi</th>
                <th>Hozirgi Qoldiq</th>
                <th>Min / Max</th>
                <th>Sotib Olish Narxi</th>
                <th>Status</th>
                <th style="text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of pagedItems">
                <td><span class="sku-tag">{{ item.sku || '---' }}</span></td>
                <td><strong>{{ item.name }}</strong></td>
                <td><span class="category-pill">{{ item.category || 'Umumiy' }}</span></td>
                <td>{{ item.warehouseName || 'Asosiy ombor' }}</td>
                <td><strong>{{ item.unit }}</strong></td>
                <td>
                  <span class="stock-amount" [class.text-danger]="item.quantity <= 0" [class.text-warning]="item.quantity > 0 && item.quantity <= item.minQuantity">
                    {{ item.quantity | number:'1.1-3' }} {{ item.unit }}
                  </span>
                </td>
                <td>
                  <small>{{ item.minQuantity }} / {{ item.maxQuantity || '∞' }}</small>
                </td>
                <td>{{ (item.costPrice || 0) | number }} so'm</td>
                <td>
                  <span class="badge badge--danger" *ngIf="item.quantity <= 0">Tugagan</span>
                  <span class="badge badge--warning" *ngIf="item.quantity > 0 && item.quantity <= item.minQuantity">Kam qoldi</span>
                  <span class="badge badge--success" *ngIf="item.quantity > item.minQuantity">Yetarli</span>
                </td>
                <td style="text-align: right;">
                  <button class="pos-btn pos-btn--sm pos-btn--secondary" (click)="openAdjustModal(item)" title="Qo'lda tuzatish">⚙️</button>
                  <button class="pos-btn pos-btn--sm pos-btn--secondary" (click)="openEditItemModal(item)" title="Tahrirlash">✏️</button>
                  <button class="pos-btn pos-btn--sm pos-btn--danger" (click)="deleteItem(item)" title="O'chirish">🗑️</button>
                </td>
              </tr>
              <tr *ngIf="filteredItems.length === 0">
                <td colspan="10" class="text-center py-4 text-muted">Mahsulot topilmadi</td>
              </tr>
            </tbody>
          </table>

          <mat-paginator
            *ngIf="filteredItems.length > 0"
            [length]="filteredItems.length"
            [pageSize]="itemPageSize"
            [pageIndex]="itemPageIndex"
            [pageSizeOptions]="pageSizeOptions"
            [showFirstLastButtons]="true"
            (page)="onItemPageChange($event)">
          </mat-paginator>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 3: KIRIM (PURCHASES) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'kirim'" class="tab-content">
        <div class="section-actions-row">
          <div>
            <h2 class="section-title">📥 Omborga Mahsulot Kirimi (Xaridlar)</h2>
            <p class="section-subtitle">Yetkazib beruvchilardan kelgan tovarlarni qabul qilish va zaxiraga qo'shish</p>
          </div>
          <button class="pos-btn pos-btn--primary" (click)="openPurchaseModal()">
            <span>➕ Yangi Kirim Hujjati</span>
          </button>
        </div>

        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Hujjat №</th>
                <th>Invoys / Chek</th>
                <th>Sana</th>
                <th>Yetkazib Beruvchi</th>
                <th>Ombor</th>
                <th>Jami Summa</th>
                <th>To'langan</th>
                <th>Qarzdorlik</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of pagedPurchases">
                <td><strong>{{ p.purchaseNumber }}</strong></td>
                <td>{{ p.invoiceNumber || '---' }}</td>
                <td>{{ p.purchaseDate || (p.createdAt | date:'yyyy-MM-dd') }}</td>
                <td><strong>{{ p.supplierName }}</strong></td>
                <td>{{ p.warehouseName }}</td>
                <td><strong>{{ p.totalAmount | number }} so'm</strong></td>
                <td class="text-success">{{ p.paidAmount | number }} so'm</td>
                <td [class.text-danger]="p.balanceDue > 0">{{ p.balanceDue | number }} so'm</td>
                <td>
                  <span class="badge badge--success">TASDIQLANGAN</span>
                </td>
              </tr>
              <tr *ngIf="purchases.length === 0">
                <td colspan="9" class="text-center py-4 text-muted">Hozircha kirim hujjatlari mavjud emas</td>
              </tr>
            </tbody>
          </table>

          <mat-paginator
            *ngIf="purchases.length > 0"
            [length]="purchases.length"
            [pageSize]="kirimPageSize"
            [pageIndex]="kirimPageIndex"
            [pageSizeOptions]="pageSizeOptions"
            [showFirstLastButtons]="true"
            (page)="onKirimPageChange($event)">
          </mat-paginator>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 4: CHIQIM (OUTBOUND) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'chiqim'" class="tab-content">
        <div class="section-actions-row">
          <div>
            <h2 class="section-title">📤 Ombordan Chiqim & Isrof Qilish</h2>
            <p class="section-subtitle">Oshxona ehtiyojlari, muddati o'tgan yoki yaroqsiz mahsulotlarni ro'yxatdan o'chirish</p>
          </div>
          <button class="pos-btn pos-btn--primary" (click)="openOutboundModal()">
            <span>➖ Chiqim Qilish</span>
          </button>
        </div>

        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Sana / Vaqt</th>
                <th>Mahsulot</th>
                <th>Chiqim Turi</th>
                <th>Miqdor</th>
                <th>Tannarx</th>
                <th>Jami Qiymat</th>
                <th>Sabab / Izoh</th>
                <th>Mas'ul Shaxs</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let m of pagedOutbound">
                <td>{{ m.createdAt | date:'yyyy-MM-dd HH:mm' }}</td>
                <td><strong>{{ m.itemName }}</strong></td>
                <td>
                  <span class="badge" [class.badge--warning]="m.type === 'OUT'" [class.badge--danger]="m.type === 'WASTE'">
                    {{ m.type === 'WASTE' ? 'ISROF' : 'CHIQIM' }}
                  </span>
                </td>
                <td class="text-danger font-bold">{{ m.quantity | number:'1.1-3' }} {{ m.unit }}</td>
                <td>{{ (m.unitCost || 0) | number }} so'm</td>
                <td><strong>{{ (m.totalCost || 0) | number }} so'm</strong></td>
                <td>{{ m.notes || 'Sabab ko\'rsatilmagan' }}</td>
                <td>{{ m.userName || 'Admin' }}</td>
              </tr>
              <tr *ngIf="outboundTransactions.length === 0">
                <td colspan="8" class="text-center py-4 text-muted">Chiqim operatsiyalari topilmadi</td>
              </tr>
            </tbody>
          </table>

          <mat-paginator
            *ngIf="outboundTransactions.length > 0"
            [length]="outboundTransactions.length"
            [pageSize]="chiqimPageSize"
            [pageIndex]="chiqimPageIndex"
            [pageSizeOptions]="pageSizeOptions"
            [showFirstLastButtons]="true"
            (page)="onChiqimPageChange($event)">
          </mat-paginator>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 5: HARAKATLAR (MOVEMENTS LEDGER) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'movements'" class="tab-content">
        <div class="table-toolbar">
          <div class="filter-controls">
            <select class="pos-select" [(ngModel)]="movementTypeFilter" (change)="loadTransactions(); movementsPageIndex = 0">
              <option value="ALL">Barcha harakat turlari</option>
              <option value="PURCHASE">Kirim (PURCHASE)</option>
              <option value="OUT">Chiqim (OUT)</option>
              <option value="SALE">Savdo sarfi (SALE)</option>
              <option value="WASTE">Isrof (WASTE)</option>
              <option value="ADJUSTMENT">Tuzatish (ADJUSTMENT)</option>
            </select>

            <select class="pos-select" [(ngModel)]="movementItemFilter" (change)="loadTransactions(); movementsPageIndex = 0">
              <option value="">Barcha mahsulotlar</option>
              <option *ngFor="let it of items" [value]="it.id">{{ it.name }}</option>
            </select>

            <input type="date" class="pos-input" [(ngModel)]="movementDateFrom" (change)="loadTransactions(); movementsPageIndex = 0" />
            <input type="date" class="pos-input" [(ngModel)]="movementDateTo" (change)="loadTransactions(); movementsPageIndex = 0" />
          </div>
        </div>

        <div class="pos-card table-card">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Sana / Vaqt</th>
                <th>Mahsulot</th>
                <th>Harakat Turi</th>
                <th>Oldingi Qoldiq</th>
                <th>O'zgarish</th>
                <th>Yangi Qoldiq</th>
                <th>Tannarx</th>
                <th>Jami Summa</th>
                <th>Hujjat / Izoh</th>
                <th>Mas'ul</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let tx of pagedTransactions">
                <td>{{ tx.createdAt | date:'yyyy-MM-dd HH:mm' }}</td>
                <td><strong>{{ tx.itemName }}</strong></td>
                <td>
                  <span class="badge" [ngClass]="getTxTypeClass(tx.type)">
                    {{ getTxTypeLabel(tx.type) }}
                  </span>
                </td>
                <td>{{ tx.quantityBefore | number:'1.1-3' }} {{ tx.unit }}</td>
                <td>
                  <span [class.text-success]="tx.quantity > 0" [class.text-danger]="tx.quantity < 0" class="font-bold">
                    {{ tx.quantity > 0 ? '+' : '' }}{{ tx.quantity | number:'1.1-3' }} {{ tx.unit }}
                  </span>
                </td>
                <td><strong>{{ tx.quantityAfter | number:'1.1-3' }} {{ tx.unit }}</strong></td>
                <td>{{ (tx.unitCost || 0) | number }} so'm</td>
                <td>{{ (tx.totalCost || 0) | number }} so'm</td>
                <td>
                  <span class="doc-pill" *ngIf="tx.referenceNumber">{{ tx.referenceNumber }}</span>
                  <small>{{ tx.notes }}</small>
                </td>
                <td>{{ tx.userName || 'Tizim' }}</td>
              </tr>
              <tr *ngIf="transactions.length === 0">
                <td colspan="10" class="text-center py-4 text-muted">Harakatlar tarixi topilmadi</td>
              </tr>
            </tbody>
          </table>

          <mat-paginator
            *ngIf="transactions.length > 0"
            [length]="transactions.length"
            [pageSize]="movementsPageSize"
            [pageIndex]="movementsPageIndex"
            [pageSizeOptions]="pageSizeOptions"
            [showFirstLastButtons]="true"
            (page)="onMovementsPageChange($event)">
          </mat-paginator>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 6: INVENTARIZATSIYA (AUDIT) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'audit'" class="tab-content">
        <!-- AGAR AKTIV AUDIT BO'LSA SANOQ JADVALI -->
        <div *ngIf="currentAudit; else auditListBlock" class="audit-session pos-card">
          <div class="audit-header">
            <div>
              <span class="badge badge--warning">SANASH JARAYONIDA</span>
              <h2 class="audit-title">{{ currentAudit.title }}</h2>
              <p class="audit-meta">Sana: {{ currentAudit.startedAt | date:'yyyy-MM-dd HH:mm' }} &bull; Ombor: {{ currentAudit.warehouseName }}</p>
            </div>
            <div class="audit-actions">
              <button class="pos-btn pos-btn--secondary" (click)="cancelCurrentAudit()">Bekor qilish</button>
              <button class="pos-btn pos-btn--primary" (click)="submitAuditCount()">✅ Sanoqni Yakunlash & Tasdiqlash</button>
            </div>
          </div>

          <table class="pos-table audit-table">
            <thead>
              <tr>
                <th>Mahsulot</th>
                <th>Birlik</th>
                <th>Tizimdagi Qoldiq</th>
                <th style="width: 180px;">Haqiqiy Qoldiq (Sanoq)</th>
                <th>Farq</th>
                <th>Tannarx</th>
                <th>Farq Qiymati</th>
                <th>Izoh</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let ai of currentAudit.items">
                <td><strong>{{ ai.itemName }}</strong></td>
                <td>{{ ai.unit }}</td>
                <td>{{ ai.systemQuantity | number:'1.1-3' }}</td>
                <td>
                  <input type="number" class="pos-input audit-input" [(ngModel)]="ai.actualQuantity" (ngModelChange)="recalcAuditDiff(ai)" />
                </td>
                <td>
                  <span [class.text-danger]="ai.difference < 0" [class.text-success]="ai.difference > 0" class="font-bold">
                    {{ ai.difference > 0 ? '+' : '' }}{{ ai.difference | number:'1.1-3' }}
                  </span>
                </td>
                <td>{{ (ai.unitCost || 0) | number }} so'm</td>
                <td>
                  <span [class.text-danger]="ai.totalDifferenceCost < 0" [class.text-success]="ai.totalDifferenceCost > 0" class="font-bold">
                    {{ ai.totalDifferenceCost | number }} so'm
                  </span>
                </td>
                <td>
                  <input type="text" class="pos-input" placeholder="Izoh..." [(ngModel)]="ai.notes" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <ng-template #auditListBlock>
          <div class="section-actions-row">
            <div>
              <h2 class="section-title">📋 Inventarizatsiya (Sanoq & Nazorat)</h2>
              <p class="section-subtitle">Haqiqiy qoldiqni tizim bilan solishtirish va farqlarni ADJUSTMENT sifatida hisobga olish</p>
            </div>
            <button class="pos-btn pos-btn--primary" (click)="startNewAudit()">
              <span>➕ Yangi Inventarizatsiya Boshlash</span>
            </button>
          </div>

          <div class="pos-card table-card">
            <table class="pos-table">
              <thead>
                <tr>
                  <th>Audit №</th>
                  <th>Nomi</th>
                  <th>Boshlangan</th>
                  <th>Yakunlangan</th>
                  <th>Ombor</th>
                  <th>Mas'ul Shaxs</th>
                  <th>Farq Qiymati</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let a of audits">
                  <td><strong>{{ a.auditNumber }}</strong></td>
                  <td>{{ a.title }}</td>
                  <td>{{ a.startedAt | date:'yyyy-MM-dd HH:mm' }}</td>
                  <td>{{ a.completedAt ? (a.completedAt | date:'yyyy-MM-dd HH:mm') : '---' }}</td>
                  <td>{{ a.warehouseName }}</td>
                  <td>{{ a.conductedByName || 'Admin' }}</td>
                  <td [class.text-danger]="a.totalDiscrepancyCost < 0" [class.text-success]="a.totalDiscrepancyCost > 0" class="font-bold">
                    {{ a.totalDiscrepancyCost | number }} so'm
                  </td>
                  <td>
                    <span class="badge" [class.badge--success]="a.status === 'COMPLETED'" [class.badge--warning]="a.status === 'IN_PROGRESS'">
                      {{ a.status === 'COMPLETED' ? 'YAKUNLANGAN' : 'JARAYONDA' }}
                    </span>
                  </td>
                </tr>
                <tr *ngIf="audits.length === 0">
                  <td colspan="8" class="text-center py-4 text-muted">Inventarizatsiyalar tarixi mavjud emas</td>
                </tr>
              </tbody>
            </table>
          </div>
        </ng-template>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 7: RETSEPTLAR (RECIPES) -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'recipes'" class="tab-content">
        <div class="recipes-layout">
          <!-- CHAP PANEL: TAOMLLAR RO'YXATI -->
          <div class="pos-card products-list-panel">
            <h3 class="panel-title">🍽️ Taom Tanlang</h3>
            <div class="search-box">
              <input type="text" class="pos-input" placeholder="Taom qidirish..." [(ngModel)]="recipeSearchQuery" />
            </div>
            <div class="recipe-dishes-list">
              <div
                *ngFor="let r of filteredRecipeDishes"
                class="dish-item"
                [class.active]="selectedRecipeProduct?.productId === r.productId"
                (click)="selectRecipeDish(r)">
                <div>
                  <strong>{{ r.productName }}</strong>
                  <span class="dish-sub">{{ r.categoryName }} &bull; Narxi: {{ (r.price || 0) | number }} so'm</span>
                </div>
                <span class="badge badge--pill" [class.badge--success]="r.recipeItems.length > 0" [class.badge--secondary]="r.recipeItems.length === 0">
                  {{ r.recipeItems.length }} ta xomashyo
                </span>
              </div>
            </div>
          </div>

          <!-- O'NG PANEL: RETSEPT TAHRIRLASH -->
          <div class="pos-card recipe-editor-panel" *ngIf="selectedRecipeProduct; else noRecipeSelected">
            <div class="panel-header-row">
              <div>
                <h2 class="editor-title">{{ selectedRecipeProduct.productName }} retsepti</h2>
                <p class="editor-sub">Taom sotilganda (Order PAID) quyidagi xomashyolar ombordan avtomatik yechiladi</p>
              </div>
              <button class="pos-btn pos-btn--primary" (click)="saveCurrentRecipe()">💾 Retseptni Saqlash</button>
            </div>

            <!-- INGREDIENTLAR JADVALI -->
            <table class="pos-table mb-4">
              <thead>
                <tr>
                  <th>Xomashyo Mahsulot</th>
                  <th>Miqdor</th>
                  <th>Birlik</th>
                  <th>Tannarx</th>
                  <th>Jami Tannarx</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let ing of currentRecipeIngredients; let idx = index">
                  <td>
                    <select class="pos-select" [(ngModel)]="ing.inventoryItemId" (ngModelChange)="onIngredientItemChange(ing)">
                      <option *ngFor="let it of items" [value]="it.id">{{ it.name }} ({{ it.unit }})</option>
                    </select>
                  </td>
                  <td>
                    <input type="number" step="0.001" class="pos-input" style="width: 110px;" [(ngModel)]="ing.quantity" (ngModelChange)="recalcRecipeItemCost(ing)" />
                  </td>
                  <td><strong>{{ ing.unit }}</strong></td>
                  <td>{{ (ing.costPrice || 0) | number }} so'm</td>
                  <td><strong>{{ (ing.totalCost || 0) | number }} so'm</strong></td>
                  <td>
                    <button class="pos-btn pos-btn--sm pos-btn--danger" (click)="removeRecipeIngredient(idx)">✕</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="recipe-summary-bar">
              <button class="pos-btn pos-btn--secondary" (click)="addRecipeIngredientRow()">+ Ingredient Qo'shish</button>
              <div class="cost-breakdown">
                <span>Jami Tannarx: <strong>{{ currentRecipeTotalCost | number }} so'm</strong></span>
                <span>Sotuv Narxi: <strong>{{ (selectedRecipeProduct.price || 0) | number }} so'm</strong></span>
                <span class="margin-badge">Kutilgan Foyda: <strong>{{ ((selectedRecipeProduct.price || 0) - currentRecipeTotalCost) | number }} so'm</strong></span>
              </div>
            </div>
          </div>

          <ng-template #noRecipeSelected>
            <div class="pos-card empty-recipe-card">
              <span>👈 Chapdagi ro'yxatdan taom tanlang</span>
            </div>
          </ng-template>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- TAB 8: OMBORLAR & SUPPLIERS -->
      <!-- ======================================================== -->
      <div *ngIf="activeTab === 'warehouses'" class="tab-content">
        <div class="split-columns">
          <!-- WAREHOUSES -->
          <div class="pos-card column-panel">
            <div class="panel-header-row">
              <h3 class="panel-title">🏢 Omborlar Ro'yxati</h3>
              <button class="pos-btn pos-btn--sm pos-btn--primary" (click)="openCreateWarehouseModal()">+ Yangi Ombor</button>
            </div>
            <div class="warehouses-grid">
              <div class="warehouse-box" *ngFor="let w of warehouses">
                <div class="wh-icon">🏢</div>
                <div class="wh-details">
                  <h4>{{ w.name }}</h4>
                  <p>{{ w.description || 'Asosiy saqlash joyi' }}</p>
                  <span class="badge badge--success" *ngIf="w.active">FAOL</span>
                </div>
              </div>
            </div>
          </div>

          <!-- SUPPLIERS -->
          <div class="pos-card column-panel">
            <div class="panel-header-row">
              <h3 class="panel-title">🤝 Yetkazib Beruvchilar</h3>
              <button class="pos-btn pos-btn--sm pos-btn--primary" (click)="openCreateSupplierModal()">+ Yangi Ta'minotchi</button>
            </div>
            <table class="pos-table">
              <thead>
                <tr>
                  <th>Nomi</th>
                  <th>Kontakt Shaxs</th>
                  <th>Telefon</th>
                  <th>Qarzdorlik</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let s of suppliers">
                  <td><strong>{{ s.name }}</strong></td>
                  <td>{{ s.contactPerson || '---' }}</td>
                  <td>{{ s.phone || '---' }}</td>
                  <td [class.text-danger]="(s.balanceDue || 0) > 0">
                    {{ (s.balanceDue || 0) | number }} so'm
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- ======================================================== -->
      <!-- MODALS -->
      <!-- ======================================================== -->
      <!-- 1. YANGI / TAHRIRLASH MAHSULOT MODAL -->
      <div class="modal-backdrop" *ngIf="showItemModal">
        <div class="pos-modal">
          <div class="modal-header">
            <h3>{{ editingItemId ? '✏️ Mahsulotni Tahrirlash' : '➕ Yangi Ombor Mahsuloti' }}</h3>
            <button class="close-btn" (click)="showItemModal = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <div class="form-group full-width">
                <label>Mahsulot Nomi *</label>
                <input type="text" class="pos-input" [(ngModel)]="itemForm.name" placeholder="Masalan: Oliy navli un" />
              </div>
              <div class="form-group">
                <label>SKU / Shtrixkod</label>
                <input type="text" class="pos-input" [(ngModel)]="itemForm.sku" placeholder="UN-001" />
              </div>
              <div class="form-group">
                <label>O'lchov Birligi *</label>
                <select class="pos-select" [(ngModel)]="itemForm.unit">
                  <option value="kg">kg (Kilogramm)</option>
                  <option value="g">g (Gramm)</option>
                  <option value="litr">litr (Litr)</option>
                  <option value="ml">ml (Millilitr)</option>
                  <option value="dona">dona (Dona/Pcs)</option>
                  <option value="quti">quti (Quti)</option>
                  <option value="pachka">pachka (Pachka)</option>
                  <option value="metr">metr (Metr)</option>
                </select>
              </div>
              <div class="form-group">
                <label>Boshlang'ich Qoldiq</label>
                <input type="number" step="0.01" class="pos-input" [(ngModel)]="itemForm.quantity" [disabled]="!!editingItemId" />
              </div>
              <div class="form-group">
                <label>Minimal Qoldiq (Alert)</label>
                <input type="number" step="0.01" class="pos-input" [(ngModel)]="itemForm.minQuantity" />
              </div>
              <div class="form-group">
                <label>Sotib Olish Narxi (Tannarx)</label>
                <input type="number" class="pos-input" [(ngModel)]="itemForm.costPrice" placeholder="so'mda" />
              </div>
              <div class="form-group">
                <label>Ombor</label>
                <select class="pos-select" [(ngModel)]="itemForm.warehouseId">
                  <option *ngFor="let w of warehouses" [value]="w.id">{{ w.name }}</option>
                </select>
              </div>
              <div class="form-group full-width">
                <label>Kategoriya</label>
                <input type="text" class="pos-input" [(ngModel)]="itemForm.category" placeholder="Masalan: Go'sht, Don, Sabzavot" />
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showItemModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="saveItem()">Saqlash</button>
          </div>
        </div>
      </div>

      <!-- 2. KIRIM MODAL (MULTI-ITEM PURCHASE) -->
      <div class="modal-backdrop" *ngIf="showPurchaseModal">
        <div class="pos-modal modal-lg">
          <div class="modal-header">
            <h3>📥 Yangi Tovar Kirimi</h3>
            <button class="close-btn" (click)="showPurchaseModal = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-grid mb-4">
              <div class="form-group">
                <label>Yetkazib Beruvchi *</label>
                <select class="pos-select" [(ngModel)]="purchaseForm.supplierId">
                  <option *ngFor="let s of suppliers" [value]="s.id">{{ s.name }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>Qabul Qiluvchi Ombor</label>
                <select class="pos-select" [(ngModel)]="purchaseForm.warehouseId">
                  <option *ngFor="let w of warehouses" [value]="w.id">{{ w.name }}</option>
                </select>
              </div>
              <div class="form-group">
                <label>Hujjat / Invoys Raqami</label>
                <input type="text" class="pos-input" [(ngModel)]="purchaseForm.invoiceNumber" placeholder="INV-00123" />
              </div>
              <div class="form-group">
                <label>To'langan Summa (so'm)</label>
                <input type="number" class="pos-input" [(ngModel)]="purchaseForm.paidAmount" placeholder="0" />
              </div>
            </div>

            <h4 class="mb-2">Mahsulotlar Ro'yxati</h4>
            <table class="pos-table mb-3">
              <thead>
                <tr>
                  <th>Mahsulot</th>
                  <th style="width: 120px;">Miqdor</th>
                  <th style="width: 150px;">Birlik Narxi</th>
                  <th style="width: 150px;">Jami</th>
                  <th style="width: 40px;"></th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let row of purchaseForm.items; let i = index">
                  <td>
                    <select class="pos-select" [(ngModel)]="row.itemId" (ngModelChange)="onPurchaseItemSelect(row)">
                      <option *ngFor="let it of items" [value]="it.id">{{ it.name }} ({{ it.unit }})</option>
                    </select>
                  </td>
                  <td>
                    <input type="number" step="0.01" class="pos-input" [(ngModel)]="row.quantity" />
                  </td>
                  <td>
                    <input type="number" class="pos-input" [(ngModel)]="row.unitCost" />
                  </td>
                  <td>
                    <strong>{{ (row.quantity * row.unitCost) | number }} so'm</strong>
                  </td>
                  <td>
                    <button class="pos-btn pos-btn--sm pos-btn--danger" (click)="removePurchaseRow(i)">✕</button>
                  </td>
                </tr>
              </tbody>
            </table>
            <button class="pos-btn pos-btn--secondary pos-btn--sm" (click)="addPurchaseRow()">+ Mahsulot Qo'shish</button>

            <div class="purchase-total-banner mt-4">
              <span>Kirim Umumiy Summasi: <strong>{{ calculatePurchaseTotal() | number }} so'm</strong></span>
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showPurchaseModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="submitPurchase()">✅ Kirimni Tasdiqlash</button>
          </div>
        </div>
      </div>

      <!-- 3. CHIQIM MODAL -->
      <div class="modal-backdrop" *ngIf="showOutboundModal">
        <div class="pos-modal">
          <div class="modal-header">
            <h3>📤 Ombordan Chiqim Qilish</h3>
            <button class="close-btn" (click)="showOutboundModal = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group mb-3">
              <label>Mahsulotni Tanlang *</label>
              <select class="pos-select" [(ngModel)]="outboundForm.itemId">
                <option *ngFor="let it of items" [value]="it.id">{{ it.name }} (Mavjud: {{ it.quantity }} {{ it.unit }})</option>
              </select>
            </div>
            <div class="form-group mb-3">
              <label>Chiqim Miqdori *</label>
              <input type="number" step="0.01" class="pos-input" [(ngModel)]="outboundForm.quantity" placeholder="Miqdor" />
            </div>
            <div class="form-group mb-3">
              <label>Chiqim Sababi *</label>
              <select class="pos-select" [(ngModel)]="outboundForm.reason">
                <option value="Oshxona uchun">Oshxona uchun</option>
                <option value="Buzilgan mahsulot">Buzilgan mahsulot</option>
                <option value="Yaroqlilik muddati tugagan">Yaroqlilik muddati tugagan</option>
                <option value="Ichki foydalanish">Ichki foydalanish</option>
                <option value="Manual chiqim">Manual chiqim</option>
                <option value="Boshqa sabab">Boshqa sabab</option>
              </select>
            </div>
            <div class="form-group mb-3">
              <label>Chiqim Turi</label>
              <select class="pos-select" [(ngModel)]="outboundForm.type">
                <option value="OUT">Oddiy Chiqim (OUT)</option>
                <option value="WASTE">Isrof / Yaroqsiz (WASTE)</option>
              </select>
            </div>
            <div class="form-group">
              <label>Qo'shimcha Izoh</label>
              <input type="text" class="pos-input" [(ngModel)]="outboundForm.notes" placeholder="Izoh..." />
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showOutboundModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="submitOutbound()">Chiqimni Tasdiqlash</button>
          </div>
        </div>
      </div>

      <!-- 4. ADJUST MODAL -->
      <div class="modal-backdrop" *ngIf="showAdjustModal">
        <div class="pos-modal">
          <div class="modal-header">
            <h3>⚙️ Zaxirani Qo'lda Tuzatish: {{ targetAdjustItem?.name }}</h3>
            <button class="close-btn" (click)="showAdjustModal = false">✕</button>
          </div>
          <div class="modal-body">
            <p class="text-muted mb-3">Hozirgi tizim qoldig'i: <strong>{{ targetAdjustItem?.quantity }} {{ targetAdjustItem?.unit }}</strong></p>
            <div class="form-group mb-3">
              <label>O'zgarish Miqdori (musbat = qo'shish, manfiy = ayirish) *</label>
              <input type="number" step="0.01" class="pos-input" [(ngModel)]="adjustQuantity" placeholder="+5 yoki -2" />
            </div>
            <div class="form-group mb-3">
              <label>Izoh / Sabab *</label>
              <input type="text" class="pos-input" [(ngModel)]="adjustNotes" placeholder="Qayta sanash sababi..." />
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showAdjustModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="submitAdjust()">Tuzatishni Saqlash</button>
          </div>
        </div>
      </div>

      <!-- 5. YANGI OMBOR MODAL -->
      <div class="modal-backdrop" *ngIf="showWarehouseModal">
        <div class="pos-modal">
          <div class="modal-header">
            <h3>🏢 Yangi Ombor Qo'shish</h3>
            <button class="close-btn" (click)="showWarehouseModal = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group mb-3">
              <label>Ombor Nomi *</label>
              <input type="text" class="pos-input" [(ngModel)]="newWarehouse.name" placeholder="Masalan: Bar ombori" />
            </div>
            <div class="form-group">
              <label>Tavsif</label>
              <input type="text" class="pos-input" [(ngModel)]="newWarehouse.description" placeholder="Joylashuvi yoki vazifasi..." />
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showWarehouseModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="submitNewWarehouse()">Saqlash</button>
          </div>
        </div>
      </div>

      <!-- 6. YANGI SUPPLIER MODAL -->
      <div class="modal-backdrop" *ngIf="showSupplierModal">
        <div class="pos-modal">
          <div class="modal-header">
            <h3>🤝 Yangi Yetkazib Beruvchi</h3>
            <button class="close-btn" (click)="showSupplierModal = false">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group mb-3">
              <label>Kompaniya / Yetkazib Beruvchi Nomi *</label>
              <input type="text" class="pos-input" [(ngModel)]="newSupplier.name" placeholder="Masalan: Oltin Don MCHJ" />
            </div>
            <div class="form-group mb-3">
              <label>Mas'ul Shaxs</label>
              <input type="text" class="pos-input" [(ngModel)]="newSupplier.contactPerson" placeholder="Ism Familiya" />
            </div>
            <div class="form-group mb-3">
              <label>Telefon</label>
              <input type="text" class="pos-input" [(ngModel)]="newSupplier.phone" placeholder="+998 90 123 45 67" />
            </div>
            <div class="form-group">
              <label>Manzil</label>
              <input type="text" class="pos-input" [(ngModel)]="newSupplier.address" placeholder="Shahar, ko'cha" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showSupplierModal = false">Bekor qilish</button>
            <button class="pos-btn pos-btn--primary" (click)="submitNewSupplier()">Saqlash</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .inventory-page {
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
      letter-spacing: -0.5px;
      margin: 0 0 4px 0;
      color: var(--text-primary);
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
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
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
      transition: transform 0.2s, box-shadow 0.2s;
    }
    .kpi-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }
    .kpi-card.warning {
      border-color: #f59e0b;
    }
    .kpi-card.danger {
      border-color: #ef4444;
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
      font-size: 20px;
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
    .dashboard-grid {
      display: grid;
      grid-template-columns: 1.2fr 1fr;
      gap: 20px;
    }
    .actions-buttons-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
      margin-top: 14px;
    }
    .action-tile {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-color);
      border-radius: 12px;
      padding: 16px;
      display: flex;
      flex-direction: column;
      text-align: left;
      cursor: pointer;
      transition: all 0.2s;
    }
    .action-tile:hover {
      background: rgba(255, 255, 255, 0.07);
      border-color: var(--primary);
    }
    .tile-icon {
      font-size: 26px;
      margin-bottom: 8px;
    }
    .tile-title {
      font-size: 15px;
      font-weight: 700;
      color: var(--text-primary);
    }
    .tile-desc {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: 4px;
    }
    .alert-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-top: 14px;
      max-height: 280px;
      overflow-y: auto;
    }
    .alert-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 14px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: 10px;
    }
    .alert-item-info strong {
      display: block;
      font-size: 14px;
    }
    .alert-sub {
      font-size: 12px;
      color: var(--text-muted);
    }
    .stock-badge {
      font-weight: 800;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 13px;
    }
    .stock-badge.warning {
      background: rgba(245, 158, 11, 0.15);
      color: #f59e0b;
    }
    .stock-badge.danger {
      background: rgba(239, 68, 68, 0.15);
      color: #ef4444;
    }
    .min-label {
      display: block;
      font-size: 11px;
      color: var(--text-muted);
    }
    .table-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .filter-controls {
      display: flex;
      gap: 10px;
      align-items: center;
      flex-wrap: wrap;
    }
    .checkbox-toggle {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--text-muted);
      cursor: pointer;
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
    .sku-tag {
      font-family: monospace;
      background: rgba(255, 255, 255, 0.06);
      padding: 2px 6px;
      border-radius: 4px;
      font-size: 12px;
    }
    .category-pill {
      background: rgba(99, 102, 241, 0.1);
      color: #818cf8;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
    }
    .section-actions-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .section-title {
      font-size: 18px;
      font-weight: 800;
      margin: 0 0 4px 0;
    }
    .section-subtitle {
      font-size: 13px;
      color: var(--text-muted);
      margin: 0;
    }
    .recipes-layout {
      display: grid;
      grid-template-columns: 360px 1fr;
      gap: 20px;
    }
    .recipe-dishes-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 600px;
      overflow-y: auto;
      margin-top: 12px;
    }
    .dish-item {
      padding: 12px 14px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: 10px;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      transition: all 0.2s;
    }
    .dish-item:hover {
      background: rgba(255, 255, 255, 0.06);
    }
    .dish-item.active {
      border-color: var(--primary);
      background: rgba(99, 102, 241, 0.12);
    }
    .dish-sub {
      display: block;
      font-size: 12px;
      color: var(--text-muted);
    }
    .recipe-summary-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: rgba(255, 255, 255, 0.03);
      border-radius: 10px;
      border: 1px solid var(--border-color);
    }
    .cost-breakdown {
      display: flex;
      gap: 20px;
      align-items: center;
    }
    .split-columns {
      display: grid;
      grid-template-columns: 1fr 1.2fr;
      gap: 20px;
    }
    .warehouses-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 14px;
    }
    .warehouse-box {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 14px;
      border: 1px solid var(--border-color);
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.02);
    }
    .wh-icon {
      font-size: 28px;
    }
    .wh-details h4 {
      margin: 0 0 4px 0;
    }
    .wh-details p {
      margin: 0 0 6px 0;
      font-size: 12px;
      color: var(--text-muted);
    }
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.75);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      backdrop-filter: blur(4px);
    }
    .pos-modal {
      background: #1e222d;
      border: 1px solid var(--border-color);
      border-radius: 16px;
      width: 520px;
      max-width: 95vw;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
    }
    .pos-modal.modal-lg {
      width: 780px;
    }
    .modal-header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-color);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .modal-header h3 {
      margin: 0;
      font-size: 17px;
    }
    .close-btn {
      background: none;
      border: none;
      color: var(--text-muted);
      font-size: 18px;
      cursor: pointer;
    }
    .modal-body {
      padding: 20px;
      overflow-y: auto;
    }
    .modal-footer {
      padding: 16px 20px;
      border-top: 1px solid var(--border-color);
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 14px;
    }
    .form-group.full-width {
      grid-column: span 2;
    }
    .form-group label {
      display: block;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      margin-bottom: 6px;
    }
    .purchase-total-banner {
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.3);
      padding: 14px;
      border-radius: 10px;
      text-align: right;
      font-size: 16px;
      color: #10b981;
    }
    .text-success { color: #10b981; }
    .text-warning { color: #f59e0b; }
    .text-danger { color: #ef4444; }
    .font-bold { font-weight: 700; }
  `]
})
export class InventoryComponent implements OnInit {
  activeTab: InventoryTab = 'dashboard';

  stats: InventoryDashboardStats | null = null;
  items: InventoryItem[] = [];
  lowStockItems: InventoryItem[] = [];
  warehouses: Warehouse[] = [];
  suppliers: Supplier[] = [];
  purchases: PurchaseResponse[] = [];
  transactions: InventoryTransaction[] = [];
  audits: AuditResponse[] = [];
  recipes: ProductRecipe[] = [];

  // Filter properties
  itemSearchQuery = '';
  selectedWarehouseFilter = '';
  selectedCategoryFilter = '';
  onlyLowStockFilter = false;
  itemCategories: string[] = [];

  movementTypeFilter = 'ALL';
  movementItemFilter = '';
  movementDateFrom = '';
  movementDateTo = '';

  // Pagination
  pageSizeOptions = [10, 25, 50, 100];
  itemPageIndex = 0;
  itemPageSize = 10;
  kirimPageIndex = 0;
  kirimPageSize = 10;
  chiqimPageIndex = 0;
  chiqimPageSize = 10;
  movementsPageIndex = 0;
  movementsPageSize = 10;

  onItemPageChange(event: PageEvent): void {
    this.itemPageIndex = event.pageIndex;
    this.itemPageSize = event.pageSize;
  }

  get pagedItems(): InventoryItem[] {
    const list = this.filteredItems;
    if (this.itemPageIndex * this.itemPageSize >= list.length && list.length > 0) {
      this.itemPageIndex = Math.max(0, Math.ceil(list.length / this.itemPageSize) - 1);
    }
    const start = this.itemPageIndex * this.itemPageSize;
    return list.slice(start, start + this.itemPageSize);
  }

  onKirimPageChange(event: PageEvent): void {
    this.kirimPageIndex = event.pageIndex;
    this.kirimPageSize = event.pageSize;
  }

  get pagedPurchases(): PurchaseResponse[] {
    const list = this.purchases;
    if (this.kirimPageIndex * this.kirimPageSize >= list.length && list.length > 0) {
      this.kirimPageIndex = Math.max(0, Math.ceil(list.length / this.kirimPageSize) - 1);
    }
    const start = this.kirimPageIndex * this.kirimPageSize;
    return list.slice(start, start + this.kirimPageSize);
  }

  onChiqimPageChange(event: PageEvent): void {
    this.chiqimPageIndex = event.pageIndex;
    this.chiqimPageSize = event.pageSize;
  }

  get pagedOutbound(): InventoryTransaction[] {
    const list = this.outboundTransactions;
    if (this.chiqimPageIndex * this.chiqimPageSize >= list.length && list.length > 0) {
      this.chiqimPageIndex = Math.max(0, Math.ceil(list.length / this.chiqimPageSize) - 1);
    }
    const start = this.chiqimPageIndex * this.chiqimPageSize;
    return list.slice(start, start + this.chiqimPageSize);
  }

  onMovementsPageChange(event: PageEvent): void {
    this.movementsPageIndex = event.pageIndex;
    this.movementsPageSize = event.pageSize;
  }

  get pagedTransactions(): InventoryTransaction[] {
    const list = this.transactions;
    if (this.movementsPageIndex * this.movementsPageSize >= list.length && list.length > 0) {
      this.movementsPageIndex = Math.max(0, Math.ceil(list.length / this.movementsPageSize) - 1);
    }
    const start = this.movementsPageIndex * this.movementsPageSize;
    return list.slice(start, start + this.movementsPageSize);
  }

  recipeSearchQuery = '';
  selectedRecipeProduct: ProductRecipe | null = null;
  currentRecipeIngredients: { inventoryItemId: string; quantity: number; unit: string; costPrice: number; totalCost: number }[] = [];
  currentRecipeTotalCost = 0;

  // Active Audit
  currentAudit: AuditResponse | null = null;

  // Modals state
  showItemModal = false;
  editingItemId: string | null = null;
  itemForm: any = { unit: 'kg', minQuantity: 5 };

  showPurchaseModal = false;
  purchaseForm: any = { items: [] };

  showOutboundModal = false;
  outboundForm: any = { reason: 'Oshxona uchun', type: 'OUT' };

  showAdjustModal = false;
  targetAdjustItem: InventoryItem | null = null;
  adjustQuantity = 0;
  adjustNotes = '';

  showWarehouseModal = false;
  newWarehouse: any = {};

  showSupplierModal = false;
  newSupplier: any = {};

  constructor(
    private inventoryService: InventoryService,
    private productService: ProductService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAllData();
  }

  setTab(tab: InventoryTab): void {
    this.activeTab = tab;
    if (tab === 'dashboard') this.loadDashboard();
    if (tab === 'items') { this.itemPageIndex = 0; this.loadItems(); }
    if (tab === 'kirim') { this.kirimPageIndex = 0; this.loadPurchases(); }
    if (tab === 'chiqim') { this.chiqimPageIndex = 0; this.loadTransactions(); }
    if (tab === 'movements') { this.movementsPageIndex = 0; this.loadTransactions(); }
    if (tab === 'audit') this.loadAudits();
    if (tab === 'recipes') this.loadRecipes();
    if (tab === 'warehouses') { this.loadWarehouses(); this.loadSuppliers(); }
  }

  loadAllData(): void {
    this.loadDashboard();
    this.loadItems();
    this.loadWarehouses();
    this.loadSuppliers();
    this.loadPurchases();
    this.loadTransactions();
    this.loadAudits();
    this.loadRecipes();
  }

  loadDashboard(): void {
    this.inventoryService.getDashboard().subscribe({
      next: res => {
        this.stats = res.data;
        this.cdr.markForCheck();
      }
    });
  }

  loadItems(): void {
    this.inventoryService.getItems(
      this.selectedWarehouseFilter || undefined,
      this.selectedCategoryFilter || undefined,
      this.onlyLowStockFilter ? true : undefined
    ).subscribe({
      next: res => {
        this.items = res.data || [];
        this.lowStockItems = this.items.filter(i => i.lowStock || i.outOfStock);
        const catSet = new Set<string>();
        this.items.forEach(i => { if (i.category) catSet.add(i.category); });
        this.itemCategories = Array.from(catSet);
        this.cdr.markForCheck();
      }
    });
  }

  get filteredItems(): InventoryItem[] {
    if (!this.itemSearchQuery.trim()) return this.items;
    const q = this.itemSearchQuery.toLowerCase();
    return this.items.filter(i =>
      i.name.toLowerCase().includes(q) || (i.sku && i.sku.toLowerCase().includes(q))
    );
  }

  loadWarehouses(): void {
    this.inventoryService.getWarehouses().subscribe({
      next: res => {
        this.warehouses = res.data || [];
        this.cdr.markForCheck();
      }
    });
  }

  loadSuppliers(): void {
    this.inventoryService.getSuppliers().subscribe({
      next: res => {
        this.suppliers = res.data || [];
        this.cdr.markForCheck();
      }
    });
  }

  loadPurchases(): void {
    this.inventoryService.getPurchases().subscribe({
      next: res => {
        this.purchases = res.data || [];
        this.cdr.markForCheck();
      }
    });
  }

  loadTransactions(): void {
    this.inventoryService.getTransactions({
      itemId: this.movementItemFilter || undefined,
      type: this.movementTypeFilter !== 'ALL' ? this.movementTypeFilter : undefined,
      dateFrom: this.movementDateFrom || undefined,
      dateTo: this.movementDateTo || undefined
    }).subscribe({
      next: res => {
        this.transactions = res.data || [];
        this.cdr.markForCheck();
      }
    });
  }

  get outboundTransactions(): InventoryTransaction[] {
    return this.transactions.filter(t => t.type === 'OUT' || t.type === 'WASTE');
  }

  loadAudits(): void {
    this.inventoryService.getAudits().subscribe({
      next: res => {
        this.audits = res.data || [];
        this.cdr.markForCheck();
      }
    });
  }

  loadRecipes(): void {
    this.inventoryService.getRecipes().subscribe({
      next: res => {
        this.recipes = res.data || [];
        if (!this.selectedRecipeProduct && this.recipes.length > 0) {
          this.selectRecipeDish(this.recipes[0]);
        }
        this.cdr.markForCheck();
      }
    });
  }

  get filteredRecipeDishes(): ProductRecipe[] {
    if (!this.recipeSearchQuery.trim()) return this.recipes;
    const q = this.recipeSearchQuery.toLowerCase();
    return this.recipes.filter(r => r.productName.toLowerCase().includes(q));
  }

  selectRecipeDish(r: ProductRecipe): void {
    this.selectedRecipeProduct = r;
    this.currentRecipeIngredients = (r.recipeItems || []).map(i => ({
      inventoryItemId: i.inventoryItemId,
      quantity: i.quantity,
      unit: i.unit,
      costPrice: i.costPrice || 0,
      totalCost: (i.costPrice || 0) * i.quantity
    }));
    this.recalcRecipeTotal();
  }

  addRecipeIngredientRow(): void {
    if (this.items.length === 0) return;
    const first = this.items[0];
    this.currentRecipeIngredients.push({
      inventoryItemId: first.id,
      quantity: 0.1,
      unit: first.unit,
      costPrice: first.costPrice || 0,
      totalCost: (first.costPrice || 0) * 0.1
    });
    this.recalcRecipeTotal();
  }

  removeRecipeIngredient(idx: number): void {
    this.currentRecipeIngredients.splice(idx, 1);
    this.recalcRecipeTotal();
  }

  onIngredientItemChange(ing: any): void {
    const item = this.items.find(i => i.id === ing.inventoryItemId);
    if (item) {
      ing.unit = item.unit;
      ing.costPrice = item.costPrice || 0;
      ing.totalCost = (ing.costPrice || 0) * (ing.quantity || 0);
    }
    this.recalcRecipeTotal();
  }

  recalcRecipeItemCost(ing: any): void {
    ing.totalCost = (ing.costPrice || 0) * (ing.quantity || 0);
    this.recalcRecipeTotal();
  }

  recalcRecipeTotal(): void {
    this.currentRecipeTotalCost = this.currentRecipeIngredients.reduce((sum, i) => sum + (i.totalCost || 0), 0);
  }

  saveCurrentRecipe(): void {
    if (!this.selectedRecipeProduct) return;
    this.inventoryService.saveRecipe({
      productId: this.selectedRecipeProduct.productId,
      items: this.currentRecipeIngredients.map(i => ({
        inventoryItemId: i.inventoryItemId,
        quantity: i.quantity,
        unit: i.unit
      }))
    }).subscribe({
      next: res => {
        this.notificationService.success("Retsept muvaffaqiyatli saqlandi!");
        this.loadRecipes();
      }
    });
  }

  // --- ITEM CRUD MODAL ---
  openCreateItemModal(): void {
    this.editingItemId = null;
    this.itemForm = {
      name: '',
      sku: '',
      unit: 'kg',
      quantity: 0,
      minQuantity: 5,
      costPrice: 0,
      category: '',
      warehouseId: this.warehouses.length ? this.warehouses[0].id : undefined
    };
    this.showItemModal = true;
  }

  openEditItemModal(item: InventoryItem): void {
    this.editingItemId = item.id;
    this.itemForm = {
      name: item.name,
      sku: item.sku,
      unit: item.unit,
      minQuantity: item.minQuantity,
      maxQuantity: item.maxQuantity,
      costPrice: item.costPrice,
      sellingPrice: item.sellingPrice,
      category: item.category,
      warehouseId: item.warehouseId
    };
    this.showItemModal = true;
  }

  saveItem(): void {
    if (!this.itemForm.name) {
      this.notificationService.error("Mahsulot nomini kiriting");
      return;
    }

    if (this.editingItemId) {
      this.inventoryService.updateItem(this.editingItemId, this.itemForm).subscribe({
        next: () => {
          this.notificationService.success("Mahsulot yangilandi!");
          this.showItemModal = false;
          this.loadItems();
          this.loadDashboard();
        }
      });
    } else {
      this.inventoryService.createItem(this.itemForm).subscribe({
        next: () => {
          this.notificationService.success("Yangi mahsulot qo'shildi!");
          this.showItemModal = false;
          this.loadItems();
          this.loadDashboard();
        }
      });
    }
  }

  deleteItem(item: InventoryItem): void {
    if (!confirm(`Haqiqatdan ham "${item.name}" mahsulotini o'chirmoqchimisiz?`)) return;
    this.inventoryService.deleteItem(item.id).subscribe({
      next: () => {
        this.notificationService.success("Mahsulot o'chirildi");
        this.loadItems();
        this.loadDashboard();
      }
    });
  }

  // --- ADJUST MODAL ---
  openAdjustModal(item: InventoryItem): void {
    this.targetAdjustItem = item;
    this.adjustQuantity = 0;
    this.adjustNotes = '';
    this.showAdjustModal = true;
  }

  submitAdjust(): void {
    if (!this.targetAdjustItem) return;
    this.inventoryService.adjustStock(this.targetAdjustItem.id, {
      quantity: this.adjustQuantity,
      type: 'ADJUSTMENT',
      notes: this.adjustNotes
    }).subscribe({
      next: () => {
        this.notificationService.success("Qoldiq muvaffaqiyatli tuzatildi!");
        this.showAdjustModal = false;
        this.loadItems();
        this.loadDashboard();
      }
    });
  }

  // --- PURCHASE MODAL ---
  openPurchaseModal(): void {
    this.purchaseForm = {
      supplierId: this.suppliers.length ? this.suppliers[0].id : '',
      warehouseId: this.warehouses.length ? this.warehouses[0].id : '',
      invoiceNumber: '',
      paidAmount: 0,
      items: [
        { itemId: this.items.length ? this.items[0].id : '', quantity: 10, unitCost: this.items.length ? (this.items[0].costPrice || 5000) : 5000 }
      ]
    };
    this.showPurchaseModal = true;
  }

  quickKirim(item: InventoryItem): void {
    this.openPurchaseModal();
    this.purchaseForm.items = [
      { itemId: item.id, quantity: item.minQuantity * 2, unitCost: item.costPrice || 5000 }
    ];
  }

  addPurchaseRow(): void {
    if (this.items.length === 0) return;
    const first = this.items[0];
    this.purchaseForm.items.push({
      itemId: first.id,
      quantity: 1,
      unitCost: first.costPrice || 5000
    });
  }

  removePurchaseRow(idx: number): void {
    this.purchaseForm.items.splice(idx, 1);
  }

  onPurchaseItemSelect(row: any): void {
    const item = this.items.find(i => i.id === row.itemId);
    if (item) {
      row.unitCost = item.costPrice || 0;
    }
  }

  calculatePurchaseTotal(): number {
    return (this.purchaseForm.items || []).reduce((sum: number, i: any) => sum + ((i.quantity || 0) * (i.unitCost || 0)), 0);
  }

  submitPurchase(): void {
    if (!this.purchaseForm.supplierId) {
      this.notificationService.error("Yetkazib beruvchini tanlang");
      return;
    }
    this.inventoryService.createPurchase(this.purchaseForm).subscribe({
      next: () => {
        this.notificationService.success("Tovar kirimi tasdiqlandi va omborga qo'shildi!");
        this.showPurchaseModal = false;
        this.loadPurchases();
        this.loadItems();
        this.loadDashboard();
      }
    });
  }

  // --- OUTBOUND MODAL ---
  openOutboundModal(): void {
    this.outboundForm = {
      itemId: this.items.length ? this.items[0].id : '',
      warehouseId: this.warehouses.length ? this.warehouses[0].id : '',
      quantity: 1,
      reason: 'Oshxona uchun',
      type: 'OUT',
      notes: ''
    };
    this.showOutboundModal = true;
  }

  submitOutbound(): void {
    if (!this.outboundForm.itemId || !this.outboundForm.quantity) {
      this.notificationService.error("Mahsulot va miqdorni kiriting");
      return;
    }
    this.inventoryService.recordOutbound(this.outboundForm).subscribe({
      next: () => {
        this.notificationService.success("Chiqim qayd etildi!");
        this.showOutboundModal = false;
        this.loadItems();
        this.loadDashboard();
        this.loadTransactions();
      }
    });
  }

  // --- AUDIT ACTIONS ---
  startNewAudit(): void {
    this.inventoryService.startAudit({
      warehouseId: this.warehouses.length ? this.warehouses[0].id : undefined,
      title: 'Inventarizatsiya - ' + new Date().toLocaleDateString('uz-UZ')
    }).subscribe({
      next: res => {
        this.currentAudit = res.data;
        this.notificationService.info("Sanoq boshlandi! Haqiqiy qoldiqlarni kiriting.");
      }
    });
  }

  cancelCurrentAudit(): void {
    this.currentAudit = null;
  }

  recalcAuditDiff(item: any): void {
    item.difference = (item.actualQuantity || 0) - (item.systemQuantity || 0);
    item.totalDifferenceCost = item.difference * (item.unitCost || 0);
  }

  submitAuditCount(): void {
    if (!this.currentAudit) return;
    this.inventoryService.submitAudit(this.currentAudit.id, {
      items: (this.currentAudit.items || []).map(i => ({
        itemId: i.itemId,
        actualQuantity: i.actualQuantity,
        notes: i.notes
      })),
      notes: 'Sanoq tasdiqlandi'
    }).subscribe({
      next: res => {
        this.notificationService.success("Inventarizatsiya yakunlandi! Ombor qoldiqlari yangilandi.");
        this.currentAudit = null;
        this.loadAudits();
        this.loadItems();
        this.loadDashboard();
      }
    });
  }

  // --- WAREHOUSE & SUPPLIER MODALS ---
  openCreateWarehouseModal(): void {
    this.newWarehouse = { name: '', description: '' };
    this.showWarehouseModal = true;
  }

  submitNewWarehouse(): void {
    if (!this.newWarehouse.name) return;
    this.inventoryService.createWarehouse(this.newWarehouse).subscribe({
      next: () => {
        this.notificationService.success("Yangi ombor qo'shildi!");
        this.showWarehouseModal = false;
        this.loadWarehouses();
      }
    });
  }

  openCreateSupplierModal(): void {
    this.newSupplier = { name: '', contactPerson: '', phone: '', email: '', address: '' };
    this.showSupplierModal = true;
  }

  submitNewSupplier(): void {
    if (!this.newSupplier.name) return;
    this.inventoryService.createSupplier(this.newSupplier).subscribe({
      next: () => {
        this.notificationService.success("Yangi yetkazib beruvchi qo'shildi!");
        this.showSupplierModal = false;
        this.loadSuppliers();
      }
    });
  }

  getTxTypeLabel(type: string): string {
    switch (type) {
      case 'PURCHASE': return 'KIRIM';
      case 'IN': return 'BOSHLANG\'ICH';
      case 'OUT': return 'CHIQIM';
      case 'SALE': return 'SAVDO SARFI';
      case 'WASTE': return 'ISROF';
      case 'ADJUSTMENT': return 'TUZATISH';
      case 'TRANSFER': return 'KO\'CHIRISH';
      default: return type;
    }
  }

  getTxTypeClass(type: string): string {
    switch (type) {
      case 'PURCHASE': case 'IN': return 'badge--success';
      case 'OUT': return 'badge--warning';
      case 'SALE': return 'badge--info';
      case 'WASTE': return 'badge--danger';
      case 'ADJUSTMENT': return 'badge--purple';
      default: return 'badge--secondary';
    }
  }
}
