import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService, Product } from '../../core/services/product.service';
import { CategoryService, Category } from '../../core/services/category.service';
import { TableService, RestaurantTable } from '../../core/services/table.service';
import { OrderService, CreateOrderRequest, CreateOrderItemRequest, Order } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { NotificationService } from '../../core/services/notification.service';

export interface PosCartItem {
  id?: string;
  productId: string;
  productName: string;
  unitPrice: number;
  quantity: number;
  sentQuantity: number;
  subtotal: number;
  kitchenStatus: string;
  kitchenId?: string;
  kitchenName?: string;
  notes?: string;
  voided: boolean;
  voidReason?: string;
  isNew: boolean;
}

@Component({
  selector: 'app-pos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="pos-screen fade-in">
      <!-- Left: Menu & Categories -->
      <div class="pos-menu">
        <!-- Top Search and Table selector bar -->
        <div class="pos-toolbar">
          <div class="table-badge">
            <span class="table-badge__icon">🪑</span>
            @if (selectedTable()) {
              <div class="table-badge__info">
                <strong>{{ selectedTable()?.name }}</strong> (#{{ selectedTable()?.tableNumber }})
              </div>
              <button class="btn-change-table" (click)="router.navigate(['/tables'])">O'zgartirish</button>
            } @else {
              <button class="btn-select-table" (click)="router.navigate(['/tables'])">
                Stol tanlang ➜
              </button>
            }
          </div>

          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input type="text" [(ngModel)]="searchQuery" (input)="filterProducts()" placeholder="Mahsulot qidirish..." class="pos-input" />
          </div>
        </div>

        <!-- Category Tabs -->
        <div class="categories-tabs">
          <button class="cat-tab" [class.cat-tab--active]="!selectedCategoryId()" (click)="selectCategory(undefined)">
            Barchasi
          </button>
          @for (cat of categories(); track cat.id) {
            <button class="cat-tab" [class.cat-tab--active]="selectedCategoryId() === cat.id" (click)="selectCategory(cat.id)">
              {{ cat.name }}
            </button>
          }
        </div>

        <!-- Products Grid -->
        <div class="products-container">
          @if (loadingProducts()) {
            <div class="loading-state">
              <div class="spinner"></div>
              <p>Mahsulotlar yuklanmoqda...</p>
            </div>
          } @else if (filteredProducts().length === 0) {
            <div class="empty-products">
              <p>Bu bo'limda mahsulotlar topilmadi.</p>
            </div>
          } @else {
            <div class="products-grid">
              @for (prod of filteredProducts(); track prod.id) {
                <div class="product-card" (click)="addToCart(prod)">
                  <div class="product-card__content">
                    <div class="product-card__name">{{ prod.name }}</div>
                    <div class="product-card__sku">{{ prod.unit }}</div>
                    <div class="product-card__price">{{ formatPrice(prod.salePrice || prod.price || 0) }}</div>
                  </div>
                  <button class="product-card__add">➕</button>
                </div>
              }
            </div>
          }
        </div>
      </div>

      <!-- Right: Order Cart -->
      <div class="pos-cart">
        <div class="cart-header">
          <div class="cart-title">
            <span>🛒 Buyurtma</span>
            @if (selectedTable()) {
              <span class="cart-table-pill">{{ selectedTable()?.name }}</span>
            }
          </div>
          @if (cart().length > 0) {
            <button class="cart-clear-btn" (click)="clearCart()">Tozalash</button>
          }
        </div>

        <!-- Cart Items List -->
        <div class="cart-items">
          @if (cart().length === 0) {
            <div class="cart-empty">
              <div class="cart-empty__icon">📋</div>
              <p>Buyurtma bo'sh</p>
              <span>Menyudan taomlarni tanlang</span>
            </div>
          } @else {
            @for (item of cart(); track getItemTrackKey(item, $index)) {
              <div class="cart-item" [class.cart-item--new]="item.isNew" [class.cart-item--voided]="item.voided">
                <div class="cart-item__row">
                  <div class="cart-item__details">
                    <div class="cart-item__title">
                      <span>{{ item.productName }}</span>
                      @if (item.kitchenName) {
                        <span class="kitchen-tag">({{ item.kitchenName }})</span>
                      }
                    </div>
                    <div class="cart-item__meta">
                      <span class="cart-item__unit-price">{{ formatPrice(item.unitPrice) }}</span>
                      <!-- STATUS PILL -->
                      <span class="status-pill" [class]="getStatusClass(item)">
                        {{ getStatusLabel(item) }}
                      </span>
                    </div>
                    @if (item.voidReason) {
                      <div class="void-reason-tag">⚠️ {{ item.voidReason }}</div>
                    }
                  </div>

                  <!-- Controls: If NEW -> Qty changer; If already SENT -> Sent badge & Cancel option -->
                  @if (item.isNew && !item.voided) {
                    <div class="cart-item__qty-controls">
                      <button class="qty-btn" (click)="decrementQty(item)">−</button>
                      <input type="number" class="qty-input" [(ngModel)]="item.quantity" min="1" (change)="onQtyChange(item)" />
                      <button class="qty-btn" (click)="incrementQty(item)">+</button>
                    </div>
                    <div class="cart-item__subtotal">
                      {{ formatPrice(item.unitPrice * item.quantity) }}
                    </div>
                    <button class="cart-item__remove" title="O'chirish" (click)="removeItem(item)">✕</button>
                  } @else if (!item.voided) {
                    <div class="cart-item__sent-controls">
                      <span class="sent-qty-badge">{{ item.quantity }}x</span>
                      @if (currentOrderId()) {
                        <button class="btn-cancel-item" (click)="openCancelModal(item)" title="Oshxonadagi taomni bekor qilish">
                          🚫 Bekor qilish
                        </button>
                      }
                    </div>
                    <div class="cart-item__subtotal">
                      {{ formatPrice(item.unitPrice * item.quantity) }}
                    </div>
                  } @else {
                    <div class="cart-item__voided-badge">
                      <del>{{ item.quantity }}x</del>
                    </div>
                    <div class="cart-item__subtotal" style="text-decoration: line-through; opacity: 0.6;">
                      {{ formatPrice(item.unitPrice * item.quantity) }}
                    </div>
                  }
                </div>
              </div>
            }
          }
        </div>

        <!-- Cart Summary -->
        <div class="cart-summary">
          <div class="summary-row">
            <span>Jami:</span>
            <span>{{ formatPrice(subtotal()) }}</span>
          </div>
          <div class="summary-row">
            <span>Xizmat haqi (10%):</span>
            <span>{{ formatPrice(serviceCharge()) }}</span>
          </div>
          <div class="summary-row summary-row--total">
            <span>Umumiy Summa:</span>
            <span>{{ formatPrice(total()) }}</span>
          </div>

          <!-- Actions -->
          <div class="cart-actions">
            <button class="btn-kitchen" 
                    [disabled]="!canSendToKitchen()"
                    (click)="sendToKitchen()">
              👨‍🍳 Oshxonaga
              @if (newItemsCount() > 0) {
                <span class="new-count-badge">{{ newItemsCount() }} ta yangi</span>
              }
            </button>
            <button class="btn-pay" 
                    [disabled]="subtotal() === 0 || isSubmitting()"
                    (click)="openPaymentModal()">
              💳 To'lov Qilish
            </button>
          </div>
        </div>
      </div>

      <!-- Payment Modal -->
      @if (showPaymentModal()) {
        <div class="modal-backdrop" (click)="closePaymentModal()">
          <div class="modal-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>To'lov Qabul Qilish</h3>
              <button class="modal-close" (click)="closePaymentModal()">✕</button>
            </div>
            <div class="modal-body">
              <div class="payment-total-box">
                <span>To'lanishi kerak bo'lgan summa:</span>
                <h2>{{ formatPrice(total()) }}</h2>
              </div>

              <div class="form-group">
                <label>To'lov Usuli</label>
                <div class="payment-methods">
                  <button type="button" class="method-btn" [class.method-btn--active]="paymentMethod === 'CASH'" (click)="paymentMethod = 'CASH'">
                    💵 Naqd (CASH)
                  </button>
                  <button type="button" class="method-btn" [class.method-btn--active]="paymentMethod === 'CARD'" (click)="paymentMethod = 'CARD'">
                    💳 Karta (CARD)
                  </button>
                </div>
              </div>

              @if (paymentMethod === 'CASH') {
                <div class="form-group">
                  <label>Mijozdan olingan summa</label>
                  <input type="number" [(ngModel)]="cashReceived" class="pos-input" placeholder="0 UZS" />
                  @if (cashReceived > total()) {
                    <div class="change-display">
                      Qaytim: <strong>{{ formatPrice(cashReceived - total()) }}</strong>
                    </div>
                  }
                </div>
              }
            </div>
            <div class="modal-footer">
              <button class="btn btn--secondary" (click)="closePaymentModal()">Bekor qilish</button>
              <button class="btn btn--primary" (click)="submitPayment()" [disabled]="isSubmitting()">
                To'lovni Tasdiqlash
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Cancel Item Modal -->
      @if (showCancelModal()) {
        <div class="modal-backdrop" (click)="closeCancelModal()">
          <div class="modal-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Taomni bekor qilish</h3>
              <button class="modal-close" (click)="closeCancelModal()">✕</button>
            </div>
            <div class="modal-body">
              <p>Stol: <strong>{{ selectedTable()?.name }}</strong></p>
              <p>Mahsulot: <strong>{{ cancellingItem?.productName }}</strong> ({{ cancellingItem?.quantity }} ta)</p>
              <div class="form-group" style="margin-top:12px;">
                <label>Bekor qilinadigan miqdor</label>
                <input type="number" [(ngModel)]="cancelQty" min="1" [max]="cancellingItem?.quantity || 1" class="pos-input" />
              </div>
              <div class="form-group" style="margin-top:12px;">
                <label>Bekor qilish sababi</label>
                <input type="text" [(ngModel)]="cancelReason" class="pos-input" placeholder="Mijoz rad etdi..." />
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn--secondary" (click)="closeCancelModal()">Yopish</button>
              <button class="btn" style="background:#ef4444; color:white;" (click)="confirmCancelItem()" [disabled]="isSubmitting()">
                Bekor qilishni tasdiqlash
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      flex: 1;
      min-height: 0;
      height: 100%;
      overflow: hidden;
    }
    .pos-screen {
      display: grid;
      grid-template-columns: 1fr 420px;
      flex: 1;
      height: 100%;
      min-height: 0;
      max-height: 100%;
      background: var(--bg-main);
      overflow: hidden;
      border-radius: var(--radius-md);
      border: 1px solid var(--border);
    }
    .pos-menu {
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--border);
      overflow: hidden;
      height: 100%;
      min-height: 0;
    }
    .pos-toolbar {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      border-bottom: 1px solid var(--border);
      background: var(--bg-card);
    }
    .table-badge {
      display: flex;
      align-items: center;
      gap: 10px;
      background: rgba(var(--primary-rgb), 0.1);
      padding: 8px 14px;
      border-radius: var(--radius-md);
      font-size: 14px;
    }
    .btn-change-table, .btn-select-table {
      background: var(--primary);
      color: white;
      border: none;
      border-radius: 4px;
      padding: 4px 8px;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
    }
    .search-box {
      display: flex;
      align-items: center;
      background: var(--bg-main);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 6px 12px;
      width: 260px;
    }
    .search-icon { margin-right: 8px; color: var(--text-muted); }
    .pos-input {
      border: none;
      background: transparent;
      outline: none;
      color: var(--text-primary);
      width: 100%;
    }
    .categories-tabs {
      display: flex;
      gap: 10px;
      padding: 12px 20px;
      overflow-x: auto;
      border-bottom: 1px solid var(--border);
      background: var(--bg-card);
      flex-shrink: 0;
    }
    .cat-tab {
      padding: 8px 16px;
      border: 1px solid var(--border);
      background: var(--bg-main);
      color: var(--text-primary);
      border-radius: 100px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all var(--transition);

      &:hover { border-color: var(--primary); }
      &--active {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
      }
    }
    .products-container {
      flex: 1;
      padding: 20px;
      overflow-y: auto;
    }
    .products-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
      gap: 16px;
    }
    .product-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      cursor: pointer;
      position: relative;
      transition: all var(--transition);

      &:hover {
        transform: translateY(-2px);
        border-color: var(--primary);
        box-shadow: var(--shadow-md);
      }

      &__name {
        font-weight: 700;
        font-size: 15px;
        color: var(--text-primary);
        margin-bottom: 4px;
      }
      &__sku {
        font-size: 12px;
        color: var(--text-muted);
        margin-bottom: 10px;
      }
      &__price {
        font-size: 16px;
        font-weight: 800;
        color: var(--primary-light);
      }
      &__add {
        position: absolute;
        bottom: 12px;
        right: 12px;
        background: var(--bg-main);
        border: 1px solid var(--border);
        border-radius: 50%;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        cursor: pointer;
      }
    }

    .pos-cart {
      display: flex;
      flex-direction: column;
      background: var(--bg-card);
      height: 100%;
      max-height: 100%;
      min-height: 0;
      overflow: hidden;
    }
    .cart-header {
      flex-shrink: 0;
      padding: 14px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border);
      background: var(--bg-card);
      z-index: 2;
    }
    .cart-title {
      font-size: 17px;
      font-weight: 800;
      display: flex;
      align-items: center;
      gap: 8px;
      color: var(--text-primary);
    }
    .cart-table-pill {
      font-size: 12px;
      background: rgba(16, 185, 129, 0.15);
      color: #10b981;
      padding: 2px 8px;
      border-radius: 100px;
    }
    .cart-clear-btn {
      background: none;
      border: none;
      color: var(--danger);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
    }
    .cart-items {
      flex: 1 1 0%;
      min-height: 0; /* CRITICAL: Enables vertical scrolling when items increase */
      padding: 14px 16px;
      overflow-y: auto;
      overflow-x: hidden;
      display: flex;
      flex-direction: column;
      gap: 10px;
      scrollbar-width: thin;
      scrollbar-color: rgba(255, 255, 255, 0.2) transparent;

      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-track {
        background: transparent;
      }
      &::-webkit-scrollbar-thumb {
        background: rgba(255, 255, 255, 0.15);
        border-radius: 4px;
      }
      &::-webkit-scrollbar-thumb:hover {
        background: rgba(255, 255, 255, 0.3);
      }
    }
    .cart-empty {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
      &__icon { font-size: 40px; margin-bottom: 12px; }
    }
    .cart-item {
      display: flex;
      flex-direction: column;
      gap: 6px;
      padding: 10px 12px;
      background: var(--bg-main);
      border-radius: var(--radius-sm);
      border: 1px solid var(--border);
      transition: all 0.2s ease;
      flex-shrink: 0;

      &--new {
        border-color: #f59e0b;
        background: rgba(245, 158, 11, 0.04);
      }

      &--voided {
        opacity: 0.6;
        background: rgba(239, 68, 68, 0.04);
        border-color: rgba(239, 68, 68, 0.3);
      }

      &__row {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 8px;
      }

      &__details {
        display: flex;
        flex-direction: column;
        gap: 2px;
        flex: 1;
      }

      &__title {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-primary);
        display: flex;
        align-items: center;
        gap: 6px;
      }

      &__meta {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
      }

      &__unit-price {
        font-size: 12px;
        color: var(--text-muted);
      }

      &__qty-controls {
        display: flex;
        align-items: center;
        gap: 4px;
      }

      &__sent-controls {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      &__subtotal {
        font-size: 13px;
        font-weight: 700;
        color: var(--text-primary);
        min-width: 65px;
        text-align: right;
      }

      &__remove {
        background: none;
        border: none;
        color: var(--danger);
        cursor: pointer;
        font-size: 16px;
        padding: 2px 6px;
        line-height: 1;
        border-radius: 4px;
        &:hover { background: rgba(239, 68, 68, 0.1); }
      }
    }

    .kitchen-tag {
      font-size: 11px;
      color: var(--text-muted);
      font-weight: normal;
    }

    .status-pill {
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 700;
      white-space: nowrap;

      &.pill--new {
        background: #fef3c7;
        color: #92400e;
        border: 1px solid #fde68a;
      }
      &.pill--sent {
        background: #dbeafe;
        color: #1e40af;
        border: 1px solid #bfdbfe;
      }
      &.pill--accepted {
        background: #ede9fe;
        color: #5b21b6;
        border: 1px solid #ddd6fe;
      }
      &.pill--cooking {
        background: #ffedd5;
        color: #9a3412;
        border: 1px solid #fed7aa;
      }
      &.pill--ready {
        background: #d1fae5;
        color: #065f46;
        border: 1px solid #a7f3d0;
      }
      &.pill--delivered {
        background: #ecfdf5;
        color: #047857;
        border: 1px solid #6ee7b7;
      }
      &.pill--cancelled {
        background: #fee2e2;
        color: #991b1b;
        border: 1px solid #fca5a5;
      }
    }

    .btn-cancel-item {
      padding: 3px 8px;
      font-size: 11px;
      font-weight: 600;
      background: rgba(239, 68, 68, 0.1);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 4px;
      cursor: pointer;
      &:hover { background: #ef4444; color: white; }
    }

    .sent-qty-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 6px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 4px;
      font-size: 12px;
      font-weight: 700;
    }

    .new-count-badge {
      display: inline-block;
      margin-left: 6px;
      background: #b45309;
      color: white;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 800;
    }

    .void-reason-tag {
      font-size: 11px;
      color: #ef4444;
      font-style: italic;
    }

    .qty-btn {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      border: 1px solid var(--border);
      background: var(--bg-card);
      color: var(--text-primary);
      cursor: pointer;
      font-weight: 700;
    }
    .qty-input {
      width: 36px;
      height: 24px;
      text-align: center;
      border: 1px solid var(--border);
      border-radius: 4px;
      background: var(--bg-card);
      color: var(--text-primary);
      font-weight: 700;
      font-size: 12px;
    }
    .cart-summary {
      flex-shrink: 0; /* CRITICAL: Never gets pushed off screen */
      padding: 14px 18px;
      border-top: 1px solid var(--border);
      background: var(--bg-card);
      box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.25);
      z-index: 2;
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
      font-size: 13px;
      color: var(--text-secondary);

      &--total {
        margin-top: 8px;
        padding-top: 8px;
        border-top: 1px dashed var(--border);
        font-size: 16px;
        font-weight: 800;
        color: var(--text-primary);
      }
    }
    .cart-actions {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-top: 12px;
    }
    .btn-kitchen, .btn-pay {
      padding: 11px 8px;
      border: none;
      border-radius: var(--radius-md);
      font-weight: 700;
      font-size: 13px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      transition: all var(--transition);
      white-space: nowrap;

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }
    .btn-kitchen {
      background: #f59e0b;
      color: white;
      &:hover:not(:disabled) { background: #d97706; }
    }
    .btn-pay {
      background: #10b981;
      color: white;
      &:hover:not(:disabled) { background: #059669; }
    }

    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0,0,0,0.6);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 440px;
      padding: 24px;
      box-shadow: var(--shadow-xl);
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      h3 { margin: 0; color: var(--text-primary); }
    }
    .modal-close {
      background: none;
      border: none;
      font-size: 18px;
      cursor: pointer;
      color: var(--text-muted);
    }
    .payment-total-box {
      text-align: center;
      padding: 16px;
      background: var(--bg-main);
      border-radius: var(--radius-md);
      margin-bottom: 20px;
      span { font-size: 13px; color: var(--text-muted); }
      h2 { margin: 4px 0 0; color: var(--primary-light); font-size: 24px; }
    }
    .payment-methods {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      margin-top: 6px;
    }
    .method-btn {
      padding: 12px;
      border: 1px solid var(--border);
      background: var(--bg-main);
      color: var(--text-primary);
      border-radius: var(--radius-md);
      font-weight: 700;
      cursor: pointer;
      &--active {
        border-color: var(--primary);
        background: rgba(var(--primary-rgb), 0.1);
        color: var(--primary-light);
      }
    }
    .change-display {
      margin-top: 8px;
      font-size: 14px;
      color: #10b981;
    }
    .btn {
      padding: 10px 18px;
      border-radius: var(--radius-md);
      font-weight: 700;
      cursor: pointer;
      border: none;
      &--primary { background: var(--primary); color: white; }
      &--secondary { background: var(--bg-main); color: var(--text-primary); border: 1px solid var(--border); }
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
    }
    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid var(--border);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 16px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class PosComponent implements OnInit {
  categories = signal<Category[]>([]);
  products = signal<Product[]>([]);
  filteredProducts = signal<Product[]>([]);
  loadingProducts = signal(true);

  selectedCategoryId = signal<string | undefined>(undefined);
  searchQuery = '';

  selectedTable = signal<RestaurantTable | null>(null);
  currentOrderId = signal<string | null>(null);

  cart = signal<PosCartItem[]>([]);
  isSubmitting = signal(false);

  showPaymentModal = signal(false);
  paymentMethod: 'CASH' | 'CARD' = 'CASH';
  cashReceived = 0;

  // Cancel item modal state
  showCancelModal = signal(false);
  cancellingItem: PosCartItem | null = null;
  cancelReason = 'Mijoz rad etdi';
  cancelQty = 1;

  // Real-time calculation of unsent (NEW) items
  newItems = computed(() =>
    this.cart().filter(i => (i.kitchenStatus === 'NEW' || i.isNew) && !i.voided && i.quantity > 0)
  );

  newItemsCount = computed(() => this.newItems().length);

  canSendToKitchen = computed(() => this.newItemsCount() > 0 && !this.isSubmitting());

  subtotal = computed(() => {
    return this.cart()
      .filter(i => !i.voided)
      .reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
  });

  serviceCharge = computed(() => Math.round(this.subtotal() * 0.10));
  total = computed(() => this.subtotal() + this.serviceCharge());

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private tableService: TableService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private notify: NotificationService,
    private route: ActivatedRoute,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();

    // Check query params for table selection
    this.route.queryParams.subscribe(params => {
      if (params['tableId']) {
        this.selectedTable.set({
          id: params['tableId'],
          tableNumber: params['tableNumber'] || '',
          name: params['tableName'] || 'Stol ' + params['tableNumber'],
          capacity: 4,
          shape: 'rectangle',
          posX: 0,
          posY: 0,
          width: 100,
          height: 80,
          status: 'OCCUPIED',
          active: true
        });

        if (params['orderId']) {
          this.currentOrderId.set(params['orderId']);
          this.loadExistingOrder(params['orderId']);
        }
      }
    });
  }

  loadCategories(): void {
    this.categoryService.getCategories(true).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.categories.set(res.data);
        }
      }
    });
  }

  loadProducts(): void {
    this.loadingProducts.set(true);
    this.productService.getProducts(this.selectedCategoryId(), undefined, true).subscribe({
      next: (res) => {
        this.loadingProducts.set(false);
        if (res.success && res.data) {
          this.products.set(res.data);
          this.filterProducts();
        }
      },
      error: () => {
        this.loadingProducts.set(false);
      }
    });
  }

  filterProducts(): void {
    let prods = this.products();
    if (this.selectedCategoryId()) {
      prods = prods.filter(p => p.categoryId === this.selectedCategoryId());
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      prods = prods.filter(p => p.name.toLowerCase().includes(q) || (p.sku && p.sku.toLowerCase().includes(q)));
    }
    this.filteredProducts.set(prods);
  }

  selectCategory(catId?: string): void {
    this.selectedCategoryId.set(catId);
    this.filterProducts();
  }

  loadExistingOrder(orderId: string): void {
    this.orderService.getOrderById(orderId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.updateCartFromOrder(res.data);
        }
      }
    });
  }

  updateCartFromOrder(order: Order): void {
    const items: PosCartItem[] = (order.items || []).map(it => {
      const isVoid = it.voided || it.kitchenStatus === 'CANCELLED';
      const isItemNew = it.kitchenStatus === 'NEW' && !isVoid;
      return {
        id: it.id,
        productId: it.productId,
        productName: it.productName,
        unitPrice: it.productPrice ?? it.unitPrice ?? 0,
        quantity: it.quantity,
        sentQuantity: it.sentQuantity ?? (!isItemNew ? it.quantity : 0),
        subtotal: it.subtotal,
        kitchenStatus: it.kitchenStatus || 'DELIVERED',
        kitchenId: it.kitchenId,
        kitchenName: it.kitchenName,
        notes: it.notes,
        voided: isVoid,
        voidReason: it.voidReason,
        isNew: isItemNew
      };
    });
    this.cart.set(items);
  }

  getStatusClass(item: PosCartItem): string {
    if (item.voided || item.kitchenStatus === 'CANCELLED') return 'pill--cancelled';
    if (item.isNew || item.kitchenStatus === 'NEW') return 'pill--new';
    switch (item.kitchenStatus) {
      case 'SENT_TO_KITCHEN': return 'pill--sent';
      case 'ACCEPTED': return 'pill--accepted';
      case 'PREPARING':
      case 'COOKING': return 'pill--cooking';
      case 'READY': return 'pill--ready';
      case 'DELIVERED':
      case 'SERVED': return 'pill--delivered';
      default: return 'pill--sent';
    }
  }

  getStatusLabel(item: PosCartItem): string {
    if (item.voided || item.kitchenStatus === 'CANCELLED') return '🔴 Bekor qilingan';
    if (item.isNew || item.kitchenStatus === 'NEW') return '🟡 Yangi';
    switch (item.kitchenStatus) {
      case 'SENT_TO_KITCHEN': return '🔵 Oshxonaga yuborilgan';
      case 'ACCEPTED': return '🟣 Qabul qilindi';
      case 'PREPARING':
      case 'COOKING': return '🟠 Tayyorlanmoqda';
      case 'READY': return '🟢 Tayyor';
      case 'DELIVERED':
      case 'SERVED': return '✅ Yetkazilgan';
      default: return '🔵 Oshxonaga yuborilgan';
    }
  }

  getItemTrackKey(item: PosCartItem, index: number): string {
    return (item.id || item.productId) + '_' + (item.isNew ? 'new' : 'sent') + '_' + index;
  }

  addToCart(product: Product): void {
    const price = product.salePrice || product.price || 0;
    // Look for an existing item with status 'NEW' (not yet sent to kitchen)
    const existingNew = this.cart().find(
      item => item.productId === product.id && (item.isNew || item.kitchenStatus === 'NEW') && !item.voided
    );

    if (existingNew) {
      this.cart.update(items =>
        items.map(item =>
          item === existingNew
            ? { ...item, quantity: item.quantity + 1, subtotal: price * (item.quantity + 1) }
            : item
        )
      );
    } else {
      // Add as a new item row
      const newItem: PosCartItem = {
        productId: product.id,
        productName: product.name,
        unitPrice: price,
        quantity: 1,
        sentQuantity: 0,
        subtotal: price,
        kitchenStatus: 'NEW',
        kitchenId: product.kitchenId,
        kitchenName: product.kitchenName,
        voided: false,
        isNew: true
      };
      this.cart.update(items => [...items, newItem]);
    }
  }

  incrementQty(item: PosCartItem): void {
    this.cart.update(items =>
      items.map(i =>
        i === item
          ? { ...i, quantity: i.quantity + 1, subtotal: i.unitPrice * (i.quantity + 1) }
          : i
      )
    );
  }

  decrementQty(item: PosCartItem): void {
    if (item.quantity <= 1) {
      this.removeItem(item);
    } else {
      this.cart.update(items =>
        items.map(i =>
          i === item
            ? { ...i, quantity: i.quantity - 1, subtotal: i.unitPrice * (i.quantity - 1) }
            : i
        )
      );
    }
  }

  onQtyChange(item: PosCartItem): void {
    if (item.quantity <= 0) {
      this.removeItem(item);
    } else {
      item.subtotal = item.unitPrice * item.quantity;
      this.cart.set([...this.cart()]);
    }
  }

  removeItem(item: PosCartItem): void {
    this.cart.update(items => items.filter(i => i !== item));
  }

  clearCart(): void {
    // If order already exists, keep already-sent items, remove only new unsent drafts
    if (this.currentOrderId()) {
      this.cart.update(items => items.filter(i => !i.isNew && i.kitchenStatus !== 'NEW'));
    } else {
      this.cart.set([]);
    }
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('uz-UZ').format(price) + ' UZS';
  }

  sendToKitchen(): void {
    const unsent = this.newItems();
    if (unsent.length === 0) return;
    this.isSubmitting.set(true);

    if (!this.currentOrderId()) {
      // First order on a free table
      const req: CreateOrderRequest = {
        tableId: this.selectedTable()?.id,
        orderType: 'DINE_IN',
        guestCount: 2,
        notes: 'Oshxonaga yuborildi',
        items: unsent.map(item => ({
          productId: item.productId,
          quantity: item.quantity,
          notes: item.notes
        }))
      };

      this.orderService.createOrder(req).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          if (res.success && res.data) {
            this.currentOrderId.set(res.data.id);
            this.updateCartFromOrder(res.data);
            this.notify.success('Buyurtma oshxonaga yuborildi va stol band qilindi!');
          }
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.notify.error(err.error?.message || 'Buyurtma yuborishda xatolik yuz berdi');
        }
      });
    } else {
      // Occupied table: send ONLY new items!
      const itemsReq: CreateOrderItemRequest[] = unsent.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        notes: item.notes
      }));

      this.orderService.sendToKitchen(this.currentOrderId()!, itemsReq).subscribe({
        next: (res) => {
          this.isSubmitting.set(false);
          if (res.success && res.data) {
            this.updateCartFromOrder(res.data);
            this.notify.success('Yangi mahsulotlar oshxonaga muvaffaqiyatli yuborildi!');
          }
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.notify.error(err.error?.message || 'Oshxonaga yuborishda xatolik yuz berdi');
        }
      });
    }
  }

  openCancelModal(item: PosCartItem): void {
    this.cancellingItem = item;
    this.cancelReason = 'Mijoz rad etdi';
    this.cancelQty = item.quantity;
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
    this.cancellingItem = null;
  }

  confirmCancelItem(): void {
    if (!this.currentOrderId() || !this.cancellingItem?.id) return;
    this.isSubmitting.set(true);

    this.orderService.cancelItem(this.currentOrderId()!, this.cancellingItem.id, {
      reason: this.cancelReason,
      quantity: this.cancelQty
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        this.closeCancelModal();
        if (res.success && res.data) {
          this.updateCartFromOrder(res.data.order);
          this.notify.success('Mahsulot muvaffaqiyatli bekor qilindi');
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.notify.error(err.error?.message || 'Bekor qilishda xatolik yuz berdi');
      }
    });
  }

  openPaymentModal(): void {
    this.cashReceived = this.total();
    this.showPaymentModal.set(true);
  }

  closePaymentModal(): void {
    this.showPaymentModal.set(false);
  }

  submitPayment(): void {
    if (this.subtotal() === 0) return;
    this.isSubmitting.set(true);

    // If no order created yet, create order first then pay
    if (!this.currentOrderId()) {
      const orderReq: CreateOrderRequest = {
        tableId: this.selectedTable()?.id,
        orderType: 'DINE_IN',
        guestCount: 2,
        items: this.cart().filter(i => !i.voided).map(item => ({
          productId: item.productId,
          quantity: item.quantity
        }))
      };

      this.orderService.createOrder(orderReq).subscribe({
        next: (orderRes) => {
          if (orderRes.success && orderRes.data) {
            this.executePayment(orderRes.data.id);
          }
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.notify.error(err.error?.message || 'Buyurtma yaratishda xatolik yuz berdi');
        }
      });
    } else {
      this.executePayment(this.currentOrderId()!);
    }
  }

  private executePayment(orderId: string): void {
    this.paymentService.processPayment({
      orderId: orderId,
      paymentMethod: this.paymentMethod,
      amount: this.total(),
      cashReceived: this.paymentMethod === 'CASH' ? this.cashReceived : this.total(),
      changeGiven: this.paymentMethod === 'CASH' ? Math.max(0, this.cashReceived - this.total()) : 0
    }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        if (res.success) {
          this.notify.success("To'lov muvaffaqiyatli qabul qilindi! Stol bo'shatildi.");
          this.closePaymentModal();
          this.clearCart();
          this.router.navigate(['/tables']);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.notify.error(err.error?.message || "To'lov jarayonida xatolik yuz berdi");
      }
    });
  }
}
