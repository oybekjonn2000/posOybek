import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';
import { WebsocketService } from '../core/services/websocket.service';
import { Order, OrderItem } from '../core/services/order.service';

@Component({
  selector: 'app-kitchen',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  template: `
    <div class="kds-container fade-in">
      <!-- Top Bar -->
      <div class="kds-header">
        <div class="kds-title-area">
          <div class="title-with-badge">
            <h1 class="kds-title">👨‍🍳 Oshxona Ekrani (KDS)</h1>
            <span class="pulse-indicator" *ngIf="activeOrders.length > 0">
              <span class="pulse-dot"></span>
              {{ activeOrders.length }} ta faol buyurtma
            </span>

            <!-- Real-time Connection Status Indicator -->
            <div
              class="realtime-status-pill"
              [class.connected]="wsService.connected()"
              [class.disconnected]="!wsService.connected()">
              <span class="status-dot"></span>
              <span class="status-text">
                {{ wsService.connected() ? '● REAL-TIME ULANGAN' : '○ REAL-TIME UZILGAN' }}
              </span>
            </div>
          </div>
          <p class="kds-subtitle">
            <span *ngIf="selectedKitchen">Stansiya: <strong>{{ selectedKitchen.name }} ({{ selectedKitchen.code }})</strong> • </span>
            Har bir stansiya faqat o'z taomlarini ko'radi • Hech qanday sahifani yangilash (refresh) shart emas
          </p>
        </div>

        <div class="kds-actions">
          <div class="kds-filters">
            <button
              class="filter-tab"
              [class.active]="currentFilter === 'ALL'"
              (click)="currentFilter = 'ALL'; pageIndex = 0">
              Barchasi ({{ orders.length }})
            </button>
            <button
              class="filter-tab"
              [class.active]="currentFilter === 'NEW'"
              (click)="currentFilter = 'NEW'; pageIndex = 0">
              Yangi ({{ countByStatus('NEW') }})
            </button>
            <button
              class="filter-tab"
              [class.active]="currentFilter === 'COOKING'"
              (click)="currentFilter = 'COOKING'; pageIndex = 0">
              Tayyorlanmoqda ({{ countByStatus('COOKING') }})
            </button>
            <button
              class="filter-tab"
              [class.active]="currentFilter === 'READY'"
              (click)="currentFilter = 'READY'; pageIndex = 0">
              Tayyor ({{ countByStatus('READY') }})
            </button>
          </div>
        </div>
      </div>

      <!-- KITCHEN STATIONS SELECTOR BAR -->
      <div class="stations-bar">
        <span class="stations-label">Oshxona Stansiyasi:</span>
        <div class="stations-strip">
          <button
            *ngIf="kitchens.length > 1"
            class="station-tab"
            [class.active]="selectedKitchen === null"
            (click)="selectKitchen(null)">
            <span class="station-icon">🍽️</span>
            <span class="station-name">Barchasi</span>
            <span class="station-code">BARCHA BIRIKTIRILGANLAR</span>
          </button>
          <button
            *ngFor="let k of kitchens"
            class="station-tab"
            [class.active]="selectedKitchen?.id === k.id"
            (click)="selectKitchen(k)">
            <span class="station-icon">{{ getKitchenIcon(k.code) }}</span>
            <span class="station-name">{{ k.name }}</span>
            <span class="station-code">{{ k.code }}</span>
          </button>
        </div>
      </div>

      <!-- REAL-TIME CANCELLATION NOTIFICATION BANNER -->
      <div class="kds-cancel-alert-banner" *ngIf="cancellationAlert">
        <div class="alert-content">
          <span class="alert-icon-anim">⚠️</span>
          <div class="alert-text">
            <div class="alert-headline">
              <strong>DIQQAT: BUYURTMA O'ZGARTIRILDI / BEKOR QILINDI!</strong>
              <span class="alert-time">{{ formatTime(cancellationAlert.timestamp.toISOString()) }}</span>
            </div>
            <div class="alert-body">
              Stol: <strong>{{ cancellationAlert.tableName }}</strong> • Buyurtma: <strong>#{{ cancellationAlert.orderNumber }}</strong>
              • Mahsulot: <span class="alert-highlight">{{ cancellationAlert.itemName }}</span>
              <span *ngIf="cancellationAlert.quantity"> ({{ cancellationAlert.quantity }} ta)</span>
              — <strong>BEKOR QILINDI</strong> • Sabab: <em>{{ cancellationAlert.reason }}</em>
              <span *ngIf="cancellationAlert.cancelledByName"> (Bekor qilgan: <strong>{{ cancellationAlert.cancelledByName }}</strong>)</span>
            </div>
          </div>
        </div>
        <button class="alert-dismiss-btn" (click)="cancellationAlert = null">✕</button>
      </div>

      <!-- Station Banner -->
      <div class="current-station-banner" *ngIf="selectedKitchen">
        <div class="banner-content">
          <span class="station-badge">{{ getKitchenIcon(selectedKitchen.code) }} {{ selectedKitchen.name }}</span>
          <span class="banner-desc">{{ selectedKitchen.description || 'Stansiya faol' }}</span>
        </div>
        <div class="banner-meta">
          <span>Kanal: <code>/topic/kitchen/{{ selectedKitchen.id }}</code></span>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="loading && orders.length === 0" class="kds-loading">
        <div class="spinner"></div>
        <p>Buyurtmalar yuklanmoqda...</p>
      </div>

      <!-- Empty State -->
      <div *ngIf="!loading && filteredOrders.length === 0" class="kds-empty">
        <div class="empty-icon">{{ getKitchenIcon(selectedKitchen?.code) }}</div>
        <h2>{{ selectedKitchen?.name || 'Oshxona' }} uchun faol buyurtmalar yo'q</h2>
        <p>Ofitsiant buyurtma yuborishi bilan bu stansiyaga tegishli taomlar darhol REAL-TIME paydo bo'ladi.</p>
      </div>

      <!-- Orders Grid -->
      <div *ngIf="filteredOrders.length > 0" class="kds-grid">
        <div
          *ngFor="let order of pagedOrders"
          class="kds-card"
          [class.kds-card--urgent]="isUrgent(order)">
          
          <!-- Card Header -->
          <div class="kds-card-header">
            <div class="table-info">
              <span class="delivery-badge" *ngIf="order.orderType === 'DELIVERY'">🚚 DELIVERY</span>
              <span class="table-badge" *ngIf="order.orderType !== 'DELIVERY'">{{ order.tableName || order.tableNumber || 'Stol ?' }}</span>
              <span class="order-num">#{{ order.orderNumber }}</span>
            </div>
            <div class="timer-badge" [class.urgent]="isUrgent(order)">
              ⏱️ {{ getElapsedTime(order) }}
            </div>
          </div>

          <!-- Delivery Customer Meta -->
          <div class="delivery-kds-meta" *ngIf="order.orderType === 'DELIVERY'">
            <span>👤 Mijoz: <strong>{{ order.customerName || 'Yetkazib berish' }}</strong> <span *ngIf="order.customerPhone">({{ order.customerPhone }})</span></span>
            <span *ngIf="order.deliveryAddress" class="delivery-addr">📍 {{ order.deliveryAddress }}</span>
          </div>

          <div class="waiter-meta" *ngIf="order.waiterName && order.orderType !== 'DELIVERY'">
            <span>👤 Ofitsiant: <strong>{{ order.waiterName }}</strong></span>
            <span *ngIf="order.sentToKitchenAt">
              🕒 {{ formatTime(order.sentToKitchenAt) }}
            </span>
          </div>

          <!-- Notes -->
          <div *ngIf="order.notes || order.kitchenNotes" class="kds-order-note">
            💬 {{ order.kitchenNotes || order.notes }}
          </div>

          <!-- Items List: Strictly filtered for this kitchen station -->
          <div class="kds-items-list">
            <div
              *ngFor="let item of order.items"
              class="kds-item-row"
              [class.item-ready]="item.kitchenStatus === 'READY'"
              [class.item-cooking]="item.kitchenStatus === 'COOKING' || item.kitchenStatus === 'PREPARING'"
              [class.item-cancelled]="item.kitchenStatus === 'CANCELLED' || item.voided">
              
              <div class="item-details">
                <span class="item-qty" [class.strikethrough]="item.kitchenStatus === 'CANCELLED' || item.voided">{{ item.quantity }}x</span>
                <span class="item-name" [class.strikethrough]="item.kitchenStatus === 'CANCELLED' || item.voided">{{ item.productName }}</span>
                <span class="item-status-pill" [class]="'pill--' + (item.kitchenStatus || 'NEW').toLowerCase()">
                  {{ item.kitchenStatus === 'CANCELLED' || item.voided ? '🚫 BEKOR QILINDI' : getStatusText(item.kitchenStatus) }}
                </span>
              </div>

              <div *ngIf="item.notes" class="item-note">
                ⚠️ {{ item.notes }}
              </div>

              <!-- Cancellation Audit details for kitchen operator -->
              <div *ngIf="item.kitchenStatus === 'CANCELLED' || item.voided" class="item-void-meta">
                <div class="void-reason">
                  ⚠️ Sabab: <strong>{{ item.voidReason || 'Mijoz rad etdi' }}</strong>
                </div>
                <div class="void-staff" *ngIf="item.voidedByName || item.voidedAt">
                  <span *ngIf="item.voidedByName">👤 Bekor qilgan: <strong>{{ item.voidedByName }}</strong></span>
                  <span *ngIf="item.voidedAt"> • 🕒 {{ formatTime(item.voidedAt) }}</span>
                </div>
              </div>

              <!-- Item Actions: only visible for active non-voided items -->
              <div class="item-actions" *ngIf="!item.voided && item.kitchenStatus !== 'CANCELLED'">
                <button
                  *ngIf="item.kitchenStatus === 'NEW' || item.kitchenStatus === 'SENT_TO_KITCHEN' || !item.kitchenStatus"
                  class="action-btn action-btn--cook"
                  (click)="setItemStatus(item, 'COOKING')">
                  🔥 Tayyorlash
                </button>

                <button
                  *ngIf="item.kitchenStatus === 'COOKING' || item.kitchenStatus === 'PREPARING'"
                  class="action-btn action-btn--ready"
                  (click)="setItemStatus(item, 'READY')">
                  ✅ Tayyor bo'ldi
                </button>

                <button
                  *ngIf="item.kitchenStatus === 'READY'"
                  class="action-btn action-btn--served"
                  (click)="setItemStatus(item, 'SERVED')">
                  🍽️ Tarqatildi
                </button>

                <span *ngIf="item.kitchenStatus === 'SERVED' || item.kitchenStatus === 'DELIVERED'" class="badge-served">
                  ✓ Yetkazildi
                </span>
              </div>
            </div>
          </div>

          <!-- Card Bulk Actions -->
          <div class="kds-card-footer">
            <button
              class="pos-btn pos-btn--secondary pos-btn--sm"
              (click)="setAllStatus(order, 'COOKING')"
              [disabled]="allInStatus(order, 'COOKING')">
              🔥 Barchasi olovda
            </button>
            <button
              class="pos-btn pos-btn--success pos-btn--sm"
              (click)="setAllStatus(order, 'READY')"
              [disabled]="allInStatus(order, 'READY')">
              ✅ Barchasi tayyor!
            </button>
          </div>
        </div>
      </div>

      <!-- Material Paginator -->
      <mat-paginator
        *ngIf="filteredOrders.length > 0"
        [length]="filteredOrders.length"
        [pageSize]="pageSize"
        [pageIndex]="pageIndex"
        [pageSizeOptions]="pageSizeOptions"
        [showFirstLastButtons]="true"
        (page)="onPageChange($event)">
      </mat-paginator>
    </div>
  `,
  styles: [`
    .kds-container {
      padding: 0;
      height: 100%;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .kds-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px 20px;
    }

    .title-with-badge {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .kds-title {
      font-size: 22px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }

    .pulse-indicator {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      background: rgba(16, 185, 129, 0.15);
      color: var(--success);
      padding: 4px 10px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
    }

    .pulse-dot {
      width: 8px;
      height: 8px;
      background: var(--success);
      border-radius: 50%;
      animation: pulse 1.5s infinite;
    }

    @keyframes pulse {
      0% { transform: scale(0.9); opacity: 1; }
      50% { transform: scale(1.4); opacity: 0.5; }
      100% { transform: scale(0.9); opacity: 1; }
    }

    /* Real-time Status Badge */
    .realtime-status-pill {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.5px;
      transition: all 0.3s ease;

      &.connected {
        background: rgba(16, 185, 129, 0.15);
        color: #10b981;
        border: 1px solid rgba(16, 185, 129, 0.3);

        .status-dot {
          width: 8px;
          height: 8px;
          background: #10b981;
          border-radius: 50%;
          box-shadow: 0 0 8px #10b981;
          animation: pulse 2s infinite;
        }
      }

      &.disconnected {
        background: rgba(239, 68, 68, 0.15);
        color: #ef4444;
        border: 1px solid rgba(239, 68, 68, 0.3);

        .status-dot {
          width: 8px;
          height: 8px;
          background: #ef4444;
          border-radius: 50%;
        }
      }
    }

    .kds-subtitle {
      color: var(--text-muted);
      font-size: 13px;
      margin: 6px 0 0 0;
    }

    .kds-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .kds-filters {
      display: flex;
      background: var(--bg-tertiary);
      border-radius: var(--radius-sm);
      padding: 3px;
      gap: 2px;
    }

    .filter-tab {
      background: transparent;
      border: none;
      color: var(--text-secondary);
      padding: 6px 14px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        color: var(--text-primary);
      }

      &.active {
        background: var(--primary);
        color: white;
      }
    }

    /* STATIONS SELECTOR BAR */
    .stations-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 10px 16px;
      overflow-x: auto;
    }

    .stations-label {
      font-size: 13px;
      font-weight: 700;
      color: var(--text-muted);
      white-space: nowrap;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .stations-strip {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .station-tab {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      color: var(--text-secondary);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        border-color: var(--primary);
        color: var(--text-primary);
        transform: translateY(-1px);
      }

      &.active {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
        box-shadow: 0 2px 8px rgba(99, 102, 241, 0.35);

        .station-code {
          background: rgba(255, 255, 255, 0.25);
          color: white;
        }
      }
    }

    .station-icon {
      font-size: 16px;
    }

    .station-code {
      font-size: 11px;
      font-weight: 700;
      background: var(--bg-card);
      color: var(--text-muted);
      padding: 2px 6px;
      border-radius: 4px;
    }

    /* Station banner */
    .current-station-banner {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 18px;
      background: linear-gradient(90deg, rgba(99, 102, 241, 0.08) 0%, rgba(99, 102, 241, 0.02) 100%);
      border-left: 4px solid var(--primary);
      border-radius: var(--radius-sm);
      font-size: 13px;
    }

    .station-badge {
      font-weight: 700;
      color: var(--primary-light, #818cf8);
      margin-right: 12px;
      font-size: 14px;
    }

    .banner-desc {
      color: var(--text-secondary);
    }

    .banner-meta {
      font-family: var(--font-mono);
      font-size: 11px;
      color: var(--text-muted);
      code {
        color: var(--accent-light, #38bdf8);
        background: rgba(0, 0, 0, 0.2);
        padding: 2px 6px;
        border-radius: 4px;
      }
    }

    /* Loading & Empty */
    .kds-loading, .kds-empty {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 60px 20px;
      text-align: center;
      color: var(--text-secondary);
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid var(--border);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 16px auto;
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }

    .empty-icon {
      font-size: 56px;
      margin-bottom: 12px;
    }

    /* Grid */
    .kds-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;
      align-items: start;
    }

    .kds-card {
      background: var(--bg-card);
      border: 2px solid var(--border);
      border-radius: var(--radius-md);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      transition: all var(--transition);
      box-shadow: var(--shadow-sm);
      animation: slideDown 0.3s ease;

      &:hover {
        border-color: var(--border-light);
        box-shadow: var(--shadow-md);
      }

      &--urgent {
        border-color: #ef4444;
        box-shadow: 0 0 14px rgba(239, 68, 68, 0.3);
      }
    }

    @keyframes slideDown {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .kds-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: var(--bg-tertiary);
      padding: 12px 16px;
      border-bottom: 1px solid var(--border);
    }

    .table-info {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .table-badge {
      background: var(--primary);
      color: white;
      font-weight: 700;
      font-size: 14px;
      padding: 4px 10px;
      border-radius: var(--radius-sm);
    }

    .delivery-badge {
      background: rgba(245, 158, 11, 0.25);
      color: #fbbf24;
      border: 1px solid #f59e0b;
      font-size: 12px;
      font-weight: 800;
      padding: 4px 10px;
      border-radius: var(--radius-sm);
      letter-spacing: 0.5px;
    }

    .delivery-kds-meta {
      display: flex;
      flex-direction: column;
      gap: 2px;
      padding: 6px 12px;
      background: rgba(245, 158, 11, 0.08);
      border-left: 3px solid #f59e0b;
      font-size: 12px;
      color: var(--text-secondary);

      strong { color: var(--text-primary); }
      .delivery-addr { font-size: 11px; color: var(--text-muted); }
    }

    .order-num {
      color: var(--text-muted);
      font-size: 13px;
      font-family: var(--font-mono);
      font-weight: 600;
    }

    .timer-badge {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      background: var(--bg-card);
      padding: 4px 8px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border);

      &.urgent {
        background: rgba(239, 68, 68, 0.2);
        color: #ef4444;
        border-color: #ef4444;
        font-weight: 700;
      }
    }

    .waiter-meta {
      display: flex;
      justify-content: space-between;
      padding: 8px 16px;
      font-size: 12px;
      color: var(--text-muted);
      border-bottom: 1px solid var(--border);
      background: rgba(255, 255, 255, 0.02);
    }

    .kds-order-note {
      background: rgba(245, 158, 11, 0.15);
      color: #fbbf24;
      padding: 8px 16px;
      font-size: 13px;
      border-bottom: 1px solid var(--border);
    }

    .kds-items-list {
      padding: 8px 0;
      display: flex;
      flex-direction: column;
    }

    .kds-item-row {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 10px 16px;
      border-bottom: 1px solid var(--border);
      transition: background var(--transition);

      &:last-child {
        border-bottom: none;
      }

      &.item-cooking {
        background: rgba(245, 158, 11, 0.08);
      }

      &.item-ready {
        background: rgba(16, 185, 129, 0.08);
      }
    }

    .item-details {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .item-qty {
      font-size: 16px;
      font-weight: 800;
      color: var(--accent-light, #38bdf8);
      background: var(--bg-tertiary);
      padding: 2px 8px;
      border-radius: 4px;
      min-width: 34px;
      text-align: center;
    }

    .item-name {
      font-size: 15px;
      font-weight: 600;
      color: var(--text-primary);
      flex: 1;
    }

    .item-status-pill {
      font-size: 11px;
      font-weight: 700;
      padding: 2px 8px;
      border-radius: 12px;
      text-transform: uppercase;

      &.pill--new {
        background: rgba(99, 102, 241, 0.15);
        color: #818cf8;
      }

      &.pill--cooking {
        background: rgba(245, 158, 11, 0.15);
        color: #f59e0b;
      }

      &.pill--ready {
        background: rgba(16, 185, 129, 0.15);
        color: #10b981;
      }

      &.pill--served {
        background: rgba(100, 116, 139, 0.15);
        color: #94a3b8;
      }
    }

    .item-note {
      font-size: 12px;
      color: #fbbf24;
      padding-left: 44px;
    }

    .item-actions {
      display: flex;
      justify-content: flex-end;
      gap: 8px;
      margin-top: 4px;
    }

    .action-btn {
      padding: 6px 12px;
      border-radius: var(--radius-sm);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      border: none;
      transition: all var(--transition);

      &--cook {
        background: #d97706;
        color: white;
        &:hover { background: #b45309; }
      }

      &--ready {
        background: #10b981;
        color: white;
        &:hover { background: #059669; }
      }

      &--served {
        background: #3b82f6;
        color: white;
        &:hover { background: #2563eb; }
      }
    }

    .badge-served {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      padding: 4px 8px;
    }

    .kds-card-footer {
      display: flex;
      gap: 8px;
      padding: 12px 16px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);

      button {
        flex: 1;
      }
    }

    .pos-btn--sm {
      min-height: 34px;
      padding: 6px 12px;
      font-size: 12px;
    }

    /* Cancellation Notification Banner */
    .kds-cancel-alert-banner {
      background: linear-gradient(135deg, rgba(220, 38, 38, 0.95), rgba(185, 28, 28, 0.95));
      color: white;
      border: 2px solid #f87171;
      border-radius: var(--radius-md);
      padding: 14px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      box-shadow: 0 8px 24px rgba(220, 38, 38, 0.4);
      animation: alertPulse 1.5s infinite;
    }

    @keyframes alertPulse {
      0%, 100% { box-shadow: 0 4px 16px rgba(220, 38, 38, 0.4); }
      50% { box-shadow: 0 4px 28px rgba(239, 68, 68, 0.7); }
    }

    .alert-content {
      display: flex;
      align-items: center;
      gap: 14px;
      flex: 1;
    }

    .alert-icon-anim {
      font-size: 28px;
      animation: bounce 0.6s infinite alternate;
    }

    @keyframes bounce {
      from { transform: translateY(0); }
      to { transform: translateY(-4px); }
    }

    .alert-headline {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 3px;
      font-size: 15px;
      letter-spacing: 0.5px;
    }

    .alert-time {
      font-size: 12px;
      background: rgba(0, 0, 0, 0.25);
      padding: 2px 8px;
      border-radius: 10px;
    }

    .alert-body {
      font-size: 13px;
      line-height: 1.4;
    }

    .alert-highlight {
      background: rgba(0, 0, 0, 0.3);
      padding: 1px 6px;
      border-radius: 4px;
      font-weight: 700;
    }

    .alert-dismiss-btn {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      cursor: pointer;
      font-size: 16px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background var(--transition);

      &:hover {
        background: rgba(255, 255, 255, 0.4);
      }
    }

    /* Cancelled item styles in KDS card */
    .kds-item-row.item-cancelled {
      background: rgba(239, 68, 68, 0.08);
      border-left: 4px solid #ef4444;
      opacity: 0.85;
    }

    .strikethrough {
      text-decoration: line-through;
      color: #9ca3af !important;
    }

    .item-status-pill.pill--cancelled {
      background: rgba(239, 68, 68, 0.25);
      color: #f87171;
      border: 1px solid rgba(239, 68, 68, 0.5);
    }

    .item-void-meta {
      background: rgba(239, 68, 68, 0.1);
      padding: 6px 10px;
      border-radius: 4px;
      font-size: 12px;
      color: #fca5a5;
      margin-top: 4px;
      border-left: 3px solid #ef4444;
    }

    .void-reason {
      color: #fecaca;
    }

    .void-staff {
      font-size: 11px;
      color: #9ca3af;
      margin-top: 2px;
    }
  `]
})
export class KitchenComponent implements OnInit, OnDestroy {
  kitchens: KitchenStation[] = [];
  selectedKitchen: KitchenStation | null = null;
  orders: Order[] = [];
  loading = false;
  currentFilter: 'ALL' | 'NEW' | 'COOKING' | 'READY' = 'ALL';

  // Pagination
  pageIndex = 0;
  pageSize = 10;
  pageSizeOptions = [10, 25, 50, 100];

  cancellationAlert: {
    orderNumber: string;
    tableName: string;
    itemName?: string;
    quantity?: number;
    reason: string;
    cancelledByName?: string;
    timestamp: Date;
  } | null = null;

  private wsUnsubs: (() => void)[] = [];
  private timerTick?: any;

  constructor(
    private kitchenService: KitchenService,
    public wsService: WebsocketService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadKitchenStations();

    // Timer tick to update relative elapsed time without polling backend
    this.timerTick = setInterval(() => {
      this.orders = [...this.orders];
      this.cdr.markForCheck();
    }, 15000);
  }

  ngOnDestroy(): void {
    this.unsubscribeAllStations();
    if (this.timerTick) {
      clearInterval(this.timerTick);
    }
  }

  loadKitchenStations(): void {
    this.kitchenService.getKitchens().subscribe({
      next: (res) => {
        this.kitchens = res.data || [];
        this.subscribeToAllKitchenStations();
        if (this.kitchens.length > 0 && !this.selectedKitchen) {
          this.selectKitchen(this.kitchens[0]);
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load kitchen stations', err);
        this.cdr.markForCheck();
      }
    });
  }

  selectKitchen(k: KitchenStation | null): void {
    this.selectedKitchen = k;
    this.pageIndex = 0;
    this.loadOrders();
    this.cdr.markForCheck();
  }

  private unsubscribeAllStations(): void {
    this.wsUnsubs.forEach(unsub => unsub());
    this.wsUnsubs = [];
  }

  private subscribeToAllKitchenStations(): void {
    this.unsubscribeAllStations();
    for (const k of this.kitchens) {
      console.log(`[KDS] Real-time kanalga obuna bo'linmoqda: /topic/kitchen/${k.id}`);
      const unsub = this.wsService.subscribeToKitchen(k.id, (event) => {
        console.log(`[KDS Event /topic/kitchen/${k.id}]:`, event);
        this.handleRealtimeMessage(event);
      });
      this.wsUnsubs.push(unsub);
    }
  }

  private handleRealtimeMessage(event: any): void {
    if (!event) return;

    if (event.type === 'KITCHEN_NEW_TICKET' || event.type === 'NEW_ORDER') {
      const incomingOrder: Order = event.payload;
      if (!incomingOrder || !incomingOrder.id) return;

      // Filter items to guarantee only items for this station are kept
      if (this.selectedKitchen && incomingOrder.items) {
        incomingOrder.items = incomingOrder.items.filter(i => 
          !i.kitchenId || i.kitchenId === this.selectedKitchen?.id
        );
      } else if (incomingOrder.items) {
        const allowedIds = new Set(this.kitchens.map(k => k.id));
        incomingOrder.items = incomingOrder.items.filter(i =>
          !i.kitchenId || allowedIds.has(i.kitchenId)
        );
      }

      if (!incomingOrder.items || incomingOrder.items.length === 0) return;

      const idx = this.orders.findIndex(o => o.id === incomingOrder.id);
      if (idx >= 0) {
        this.orders[idx] = incomingOrder;
        this.orders = [...this.orders];
      } else {
        this.orders = [incomingOrder, ...this.orders];
        this.playChime();
      }
      this.cdr.markForCheck();
    } else if (event.type === 'KITCHEN_ITEM_STATUS') {
      const { itemId, status } = event.payload;
      for (const ord of this.orders) {
        if (ord.items) {
          const it = ord.items.find(i => i.id === itemId);
          if (it) {
            it.kitchenStatus = status;
            break;
          }
        }
      }
      this.orders = [...this.orders];
      this.cdr.markForCheck();
    } else if (event.type === 'ORDER_ITEM_CANCELLED') {
      const payload = event.payload;
      if (!payload) return;

      console.warn('[KDS] Mahsulot bekor qilindi:', payload);

      this.cancellationAlert = {
        orderNumber: payload.orderNumber,
        tableName: payload.tableName || `Stol ${payload.tableNumber || '?'}`,
        itemName: payload.productName,
        quantity: payload.cancelledQuantity,
        reason: payload.reason,
        cancelledByName: payload.cancelledByName,
        timestamp: new Date()
      };

      this.playCancelAlertSound();

      const ord = this.orders.find(o => o.id === payload.orderId);
      if (ord && ord.items) {
        if (payload.order && payload.order.items) {
          const stationItems = payload.order.items.filter((i: any) =>
            !i.kitchenId || i.kitchenId === this.selectedKitchen?.id
          );
          ord.items = stationItems;
        } else {
          const it = ord.items.find(i => i.id === payload.itemId);
          if (it) {
            it.kitchenStatus = 'CANCELLED';
            it.voided = true;
            it.voidReason = payload.reason;
            it.voidedByName = payload.cancelledByName;
            it.voidedAt = payload.cancelledAt;
          }
        }
      } else if (payload.order) {
        this.loadOrders(true);
      }
      this.orders = [...this.orders];
      this.cdr.markForCheck();

      setTimeout(() => {
        if (this.cancellationAlert?.orderNumber === payload.orderNumber) {
          this.cancellationAlert = null;
          this.cdr.markForCheck();
        }
      }, 15000);
    } else if (event.type === 'ORDER_CANCELLED') {
      const payload = event.payload;
      if (!payload) return;

      console.warn('[KDS] Butun buyurtma bekor qilindi:', payload);

      this.cancellationAlert = {
        orderNumber: payload.orderNumber,
        tableName: payload.tableName || `Stol ${payload.tableNumber || '?'}`,
        itemName: 'BUTUN BUYURTMA',
        reason: payload.reason,
        cancelledByName: payload.cancelledByName,
        timestamp: new Date()
      };

      this.playCancelAlertSound();

      const ord = this.orders.find(o => o.id === payload.orderId);
      if (ord && ord.items) {
        ord.status = 'CANCELLED';
        ord.items.forEach(it => {
          it.kitchenStatus = 'CANCELLED';
          it.voided = true;
          it.voidReason = payload.reason;
          it.voidedByName = payload.cancelledByName;
          it.voidedAt = payload.cancelledAt;
        });
      }
      this.orders = [...this.orders];
      this.cdr.markForCheck();

      setTimeout(() => {
        if (this.cancellationAlert?.orderNumber === payload.orderNumber) {
          this.cancellationAlert = null;
          this.cdr.markForCheck();
        }
      }, 15000);
    }
  }

  loadOrders(silent: boolean = false): void {
    if (!silent) this.loading = true;
    this.cdr.markForCheck();

    const kitchenId = this.selectedKitchen?.id;
    this.kitchenService.getKitchenOrders(kitchenId).subscribe({
      next: (res) => {
        if (res.data) {
          this.orders = res.data;
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load kitchen orders', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get activeOrders(): Order[] {
    return this.orders;
  }

  get filteredOrders(): Order[] {
    if (this.currentFilter === 'ALL') return this.orders;
    return this.orders.filter(order =>
      order.items?.some(item => {
        if (item.voided) return false;
        const st = item.kitchenStatus || 'NEW';
        if (this.currentFilter === 'NEW') return st === 'NEW' || st === 'SENT_TO_KITCHEN';
        if (this.currentFilter === 'COOKING') return st === 'COOKING' || st === 'PREPARING';
        return st === this.currentFilter;
      })
    );
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  get pagedOrders(): Order[] {
    const list = this.filteredOrders;
    if (this.pageIndex * this.pageSize >= list.length && list.length > 0) {
      this.pageIndex = Math.max(0, Math.ceil(list.length / this.pageSize) - 1);
    }
    const start = this.pageIndex * this.pageSize;
    return list.slice(start, start + this.pageSize);
  }

  countByStatus(status: string): number {
    return this.orders.filter(order =>
      order.items?.some(item => {
        if (item.voided) return false;
        const st = item.kitchenStatus || 'NEW';
        if (status === 'NEW') return st === 'NEW' || st === 'SENT_TO_KITCHEN';
        if (status === 'COOKING') return st === 'COOKING' || st === 'PREPARING';
        return st === status;
      })
    ).length;
  }

  getKitchenIcon(code?: string): string {
    switch (code?.toUpperCase()) {
      case 'PLOV': return '🍚';
      case 'SOMSA': return '🥟';
      case 'PIZZA': return '🍕';
      case 'BAR': return '🍹';
      case 'MAIN': return '🍲';
      default: return '👨‍🍳';
    }
  }

  getStatusText(status?: string): string {
    switch (status) {
      case 'NEW': return '🟡 Yangi';
      case 'SENT_TO_KITCHEN': return '🔵 Yuborilgan';
      case 'ACCEPTED': return '🟣 Qabul qilindi';
      case 'PREPARING':
      case 'COOKING': return '🟠 Tayyorlanmoqda';
      case 'READY': return '🟢 Tayyor';
      case 'DELIVERED':
      case 'SERVED': return '✅ Yetkazilgan';
      case 'CANCELLED': return '🔴 Bekor qilindi';
      default: return status || 'Yangi';
    }
  }

  formatTime(isoDate?: string): string {
    if (!isoDate) return '';
    try {
      const d = new Date(isoDate);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return '';
    }
  }

  isUrgent(order: Order): boolean {
    const timeStr = order.sentToKitchenAt || order.openedAt || order.createdAt;
    if (!timeStr) return false;
    const diffMins = (Date.now() - new Date(timeStr).getTime()) / 60000;
    return diffMins > 20;
  }

  getElapsedTime(order: Order): string {
    const timeStr = order.sentToKitchenAt || order.openedAt || order.createdAt;
    if (!timeStr) return 'Yangi';
    const diffSecs = Math.floor((Date.now() - new Date(timeStr).getTime()) / 1000);
    if (diffSecs < 60) return `${diffSecs} sek`;
    const mins = Math.floor(diffSecs / 60);
    return `${mins} daq`;
  }

  setItemStatus(item: OrderItem, status: 'NEW' | 'ACCEPTED' | 'COOKING' | 'PREPARING' | 'READY' | 'SERVED' | 'DELIVERED'): void {
    if (!item.id) return;
    this.kitchenService.updateItemStatus(item.id, status as any).subscribe({
      next: () => {
        item.kitchenStatus = status;
        this.orders = [...this.orders];
      },
      error: (err) => alert('Statusni yangilab bo‘lmadi: ' + (err.error?.message || err.message))
    });
  }

  setAllStatus(order: Order, status: 'COOKING' | 'READY'): void {
    if (!order.items) return;
    const itemsToUpdate = order.items.filter(i => i.id && i.kitchenStatus !== status && i.kitchenStatus !== 'SERVED');
    itemsToUpdate.forEach(item => {
      if (item.id) {
        this.kitchenService.updateItemStatus(item.id, status).subscribe({
          next: () => {
            item.kitchenStatus = status;
            this.orders = [...this.orders];
          }
        });
      }
    });
  }

  allInStatus(order: Order, status: string): boolean {
    if (!order.items || order.items.length === 0) return true;
    return order.items.every(i => i.kitchenStatus === status || i.kitchenStatus === 'SERVED');
  }

  private playChime(): void {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(587.33, ctx.currentTime); // D5
      osc.frequency.setValueAtTime(880, ctx.currentTime + 0.1); // A5
      gain.gain.setValueAtTime(0.2, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.4);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.4);
    } catch {
      // Audio might be blocked by browser autoplay policy before user interaction
    }
  }

  private playCancelAlertSound(): void {
    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();

      const playBeep = (freq: number, delay: number, dur: number) => {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(freq, ctx.currentTime + delay);
        gain.gain.setValueAtTime(0.3, ctx.currentTime + delay);
        gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + delay + dur);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(ctx.currentTime + delay);
        osc.stop(ctx.currentTime + delay + dur);
      };

      playBeep(650, 0, 0.2);
      playBeep(450, 0.25, 0.35);
    } catch {
      // Audio might be blocked by browser policy
    }
  }
}
