import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService, Product } from '../../core/services/product.service';
import { CategoryService, Category } from '../../core/services/category.service';
import { KitchenService, KitchenStation } from '../../core/services/kitchen.service';
import { TableService, RestaurantTable } from '../../core/services/table.service';
import { OrderService, CreateOrderRequest, CreateOrderItemRequest, Order } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { NotificationService } from '../../core/services/notification.service';
import { AuthService } from '../../core/services/auth.service';
import { getProductImageUrl, handleImageError } from '../../core/utils/product-image.util';

export interface PosCartItem {
  id?: string;
  productId: string;
  productName: string;
  imageUrl?: string;
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
              <button class="btn-change-table" (click)="onLeaveTable()">O'zgartirish</button>
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

        <!-- Kitchen Station Filter Strip -->
        <div class="kitchen-stations-strip">
          <button class="station-chip" [class.station-chip--active]="!selectedKitchenId()" (click)="selectKitchen(undefined)">
            🍽️ Barcha Oshxonalar
          </button>
          @for (k of kitchens(); track k.id) {
            <button class="station-chip" [class.station-chip--active]="selectedKitchenId() === k.id" (click)="selectKitchen(k.id)">
              {{ getStationEmoji(k.code) }} {{ k.name }}
            </button>
          }
        </div>

        <!-- Category Tabs (Dynamically filtered by selected Kitchen) -->
        <div class="categories-tabs">
          <button class="cat-tab" [class.cat-tab--active]="!selectedCategoryId()" (click)="selectCategory(undefined)">
            Barcha bo'limlar
          </button>
          @for (cat of visibleCategories(); track cat.id) {
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
              <p>Ushbu oshxona yoki bo'limda mahsulotlar topilmadi.</p>
            </div>
          } @else {
            <div class="products-grid">
              @for (prod of filteredProducts(); track prod.id) {
                <div class="product-card" (click)="addToCart(prod)">
                  <div class="product-card__image-box">
                    <img
                      [src]="getImageUrl(prod.imageUrl)"
                      (error)="onImageError($event)"
                      [alt]="prod.name"
                      class="product-card__img"
                      loading="lazy"
                    />
                    <div class="product-card__station-badge">
                      {{ getProductStationBadge(prod) }}
                    </div>
                  </div>
                  <div class="product-card__body">
                    <div class="product-card__name" [title]="prod.name">{{ prod.name }}</div>
                    <div class="product-card__bottom-row">
                      <div class="product-card__price">{{ formatPrice(prod.salePrice || prod.price || 0) }}</div>
                      <button class="product-card__add" (click)="$event.stopPropagation(); addToCart(prod)" title="Buyurtmaga qo'shish">
                        ➕
                      </button>
                    </div>
                  </div>
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
                <div class="cart-item__main">
                  <div class="cart-item__thumb-wrap">
                    <img
                      [src]="getImageUrl(item.imageUrl)"
                      (error)="onImageError($event)"
                      [alt]="item.productName"
                      class="cart-item__thumb"
                      loading="lazy"
                    />
                  </div>

                  <div class="cart-item__content">
                    <!-- 1. Top Row: Title + Total Subtotal -->
                    <div class="cart-item__top">
                      <div class="cart-item__title">
                        <span>{{ item.productName }}</span>
                        @if (item.kitchenName) {
                          <span class="kitchen-tag">({{ item.kitchenName }})</span>
                        }
                      </div>
                      <div class="cart-item__subtotal" [style.text-decoration]="item.voided ? 'line-through' : 'none'" [style.opacity]="item.voided ? '0.6' : '1'">
                        {{ formatPrice(item.unitPrice * item.quantity) }}
                      </div>
                    </div>

                    <!-- 2. Meta Row: Unit Price + Status Pill -->
                    <div class="cart-item__meta-row">
                      <span class="cart-item__unit-price">{{ formatPrice(item.unitPrice) }}</span>
                      <span class="status-pill" [class]="getStatusClass(item)">
                        {{ getStatusLabel(item) }}
                      </span>
                      @if (item.voidReason) {
                        <div class="void-reason-tag">⚠️ {{ item.voidReason }}</div>
                      }
                    </div>

                    <!-- 3. Bottom Row: Stepper & Action button -->
                    @if (!item.voided) {
                      <div class="cart-item__bottom">
                        <div class="cart-item__qty-controls">
                          <button class="qty-btn" (click)="decrementQty(item)" title="Kamaytirish">−</button>
                          <span class="qty-display">{{ item.quantity }}x</span>
                          <button class="qty-btn" (click)="incrementQty(item)" title="Oshirish">+</button>
                        </div>

                        <div class="cart-item__actions">
                          @if ((item.sentQuantity || 0) === 0) {
                            <button class="cart-item__remove" title="O'chirish" (click)="removeItem(item)">
                              ✕ O'chirish
                            </button>
                          } @else if (currentOrderId()) {
                            <button class="btn-cancel-item" (click)="openCancelModal(item)" title="Oshxonadagi taomni bekor qilish">
                              🚫 Bekor qilish
                            </button>
                          }
                        </div>
                      </div>
                    } @else {
                      <div class="cart-item__voided-badge">
                        <span>❌ Bekor qilingan ({{ item.quantity }} ta)</span>
                      </div>
                    }
                  </div>
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
            @if (canProcessPayment()) {
              <button class="btn-pay" 
                      [disabled]="subtotal() === 0 || isSubmitting()"
                      (click)="openPaymentModal()">
                💳 To'lov Qilish
              </button>
            }
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
          <div class="modal-card modal-card--cancel" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span style="font-size: 20px;">🚫</span>
                <h3 style="margin: 0;">Taomni bekor qilish</h3>
              </div>
              <button class="modal-close" (click)="closeCancelModal()">✕</button>
            </div>
            <div class="modal-body">
              <div class="cancel-modal-info">
                <div class="cancel-info-row">
                  <span class="info-label">Stol:</span>
                  <strong>{{ selectedTable()?.name }}</strong>
                </div>
                <div class="cancel-info-row">
                  <span class="info-label">Mahsulot:</span>
                  <strong>{{ cancellingItem?.productName }}</strong>
                </div>
              </div>

              <div class="form-group" style="margin-top: 14px;">
                <label style="font-size: 12px; font-weight: 600; color: var(--text-secondary);">Bekor qilinadigan miqdor</label>
                <div style="display: flex; align-items: center; gap: 10px; margin-top: 6px;">
                  <input type="number" [(ngModel)]="cancelQty" min="1" [max]="cancellingItem?.sentQuantity || cancellingItem?.quantity || 1" class="pos-input" style="width: 100px; font-weight: 700;" />
                  <span style="font-size: 13px; color: var(--text-muted);">dona (Mavjud: {{ cancellingItem?.sentQuantity || cancellingItem?.quantity }} ta)</span>
                </div>
              </div>

              <div class="form-group" style="margin-top: 16px;">
                <label style="font-size: 12px; font-weight: 600; color: var(--text-secondary); display: block; margin-bottom: 8px;">
                  Bekor qilish sababini tanlang: <span style="color: #ef4444;">*</span>
                </label>

                <!-- 5 ta tezkor variantlar (Reason Options) -->
                <div class="cancel-reasons-grid">
                  @for (opt of cancelReasonOptions; track opt.id) {
                    <button 
                      type="button" 
                      class="reason-option-card"
                      [class.active]="selectedReasonId === opt.id"
                      (click)="onSelectReason(opt)">
                      <span class="reason-icon">{{ opt.icon }}</span>
                      <div class="reason-text-wrap">
                        <div class="reason-title">{{ opt.title }}</div>
                        <div class="reason-desc">{{ opt.desc }}</div>
                      </div>
                    </button>
                  }
                </div>

                @if (selectedReasonId === 'OTHER') {
                  <div style="margin-top: 10px;">
                    <input 
                      type="text" 
                      [(ngModel)]="customCancelReason" 
                      (input)="onCustomReasonChange()"
                      class="pos-input" 
                      placeholder="Sababni batafsil yozing..." 
                      style="width: 100%; box-sizing: border-box;" />
                  </div>
                }
              </div>
            </div>
            <div class="modal-footer" style="margin-top: 20px; display: flex; justify-content: flex-end; gap: 10px;">
              <button class="btn btn--secondary" (click)="closeCancelModal()">Orqaga</button>
              <button class="btn" style="background:#ef4444; color:white; font-weight: 700; padding: 10px 18px;" (click)="confirmCancelItem()" [disabled]="isSubmitting() || !cancelReason.trim()">
                @if (isSubmitting()) {
                  <span>Bajarilmoqda...</span>
                } @else {
                  <span>Bekor qilishni tasdiqlash</span>
                }
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
    .kitchen-stations-strip {
      display: flex;
      gap: 8px;
      padding: 10px 20px;
      overflow-x: auto;
      border-bottom: 1px solid var(--border);
      background: var(--bg-secondary);
      flex-shrink: 0;
    }
    .station-chip {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border: 1px solid var(--border);
      background: var(--bg-card);
      color: var(--text-secondary);
      border-radius: var(--radius-md);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all var(--transition);

      &:hover {
        border-color: var(--primary);
        color: var(--text-primary);
      }
      &--active {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
      }
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
      grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
      gap: 14px;
    }
    .product-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      overflow: hidden;
      display: flex;
      flex-direction: column;
      cursor: pointer;
      position: relative;
      transition: all 0.2s ease;

      &:hover {
        transform: translateY(-2px);
        border-color: var(--primary);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.25);

        .product-card__img {
          transform: scale(1.04);
        }
      }

      &__image-box {
        position: relative;
        width: 100%;
        height: 120px;
        background: #141721;
        overflow: hidden;
        border-bottom: 1px solid var(--border);
      }

      &__img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
        transition: transform 0.3s ease;
      }

      &__station-badge {
        position: absolute;
        top: 8px;
        left: 8px;
        font-size: 11px;
        font-weight: 700;
        color: #fff;
        background: rgba(15, 17, 23, 0.85);
        backdrop-filter: blur(4px);
        border: 1px solid rgba(255, 255, 255, 0.15);
        display: inline-flex;
        align-items: center;
        gap: 4px;
        padding: 3px 8px;
        border-radius: 6px;
        z-index: 2;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
      }

      &__body {
        padding: 12px;
        display: flex;
        flex-direction: column;
        flex: 1;
        justify-content: space-between;
      }

      &__name {
        font-weight: 700;
        font-size: 14px;
        color: var(--text-primary);
        margin-bottom: 8px;
        line-height: 1.3;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
        min-height: 36px;
      }

      &__bottom-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-top: auto;
      }

      &__price {
        font-size: 15px;
        font-weight: 800;
        color: var(--primary-light);
      }

      &__add {
        background: rgba(var(--primary-rgb), 0.15);
        color: var(--primary-light);
        border: 1px solid rgba(var(--primary-rgb), 0.3);
        border-radius: 50%;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s ease;

        &:hover {
          background: var(--primary);
          color: white;
          border-color: var(--primary);
          transform: scale(1.1);
        }
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
      width: 100%;
      box-sizing: border-box;

      &--new {
        border-color: #f59e0b;
        background: rgba(245, 158, 11, 0.04);
      }

      &--voided {
        opacity: 0.6;
        background: rgba(239, 68, 68, 0.04);
        border-color: rgba(239, 68, 68, 0.3);
      }

      &__main {
        display: flex;
        gap: 10px;
        align-items: flex-start;
      }

      &__thumb-wrap {
        width: 44px;
        height: 44px;
        border-radius: 6px;
        overflow: hidden;
        background: #141721;
        border: 1px solid var(--border);
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      &__thumb {
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }

      &__content {
        flex: 1;
        min-width: 0;
      }

      &__top {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        gap: 10px;
      }

      &__title {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-primary);
        display: flex;
        align-items: center;
        gap: 6px;
        flex-wrap: wrap;
        flex: 1;
        min-width: 0;
        word-break: break-word;
      }

      &__subtotal {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-primary);
        white-space: nowrap;
        text-align: right;
      }

      &__meta-row {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-wrap: wrap;
        margin-top: 2px;
      }

      &__unit-price {
        font-size: 12px;
        color: var(--text-muted);
      }

      &__bottom {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 10px;
        margin-top: 6px;
        padding-top: 6px;
        border-top: 1px dashed rgba(255, 255, 255, 0.08);
      }

      &__qty-controls {
        display: flex;
        align-items: center;
        gap: 6px;
      }

      .qty-display {
        font-size: 13px;
        font-weight: 700;
        color: var(--text-primary);
        min-width: 26px;
        text-align: center;
      }

      &__actions {
        display: flex;
        align-items: center;
        gap: 6px;
        flex-shrink: 0;
      }

      &__remove {
        background: rgba(239, 68, 68, 0.1);
        border: 1px solid rgba(239, 68, 68, 0.25);
        color: var(--danger);
        cursor: pointer;
        font-size: 12px;
        font-weight: 600;
        padding: 4px 8px;
        border-radius: 6px;
        transition: all 0.2s;
        &:hover { background: #ef4444; color: white; }
      }

      &__voided-badge {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding-top: 4px;
        font-size: 12px;
        color: var(--danger);
        font-weight: 600;
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
      &.pill--partial {
        background: #e0f2fe;
        color: #0369a1;
        border: 1px solid #7dd3fc;
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
      padding: 4px 10px;
      font-size: 11px;
      font-weight: 700;
      background: rgba(239, 68, 68, 0.12);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.35);
      border-radius: 6px;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 4px;
      white-space: nowrap;
      flex-shrink: 0;
      transition: all 0.2s ease;

      &:hover {
        background: #ef4444;
        color: white;
        border-color: #ef4444;
        box-shadow: 0 2px 6px rgba(239, 68, 68, 0.35);
      }
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
      display: flex;
      gap: 10px;
      margin-top: 12px;
    }
    .btn-kitchen, .btn-pay {
      flex: 1;
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

      &--cancel {
        max-width: 520px;
      }
    }

    .cancel-modal-info {
      display: flex;
      gap: 20px;
      padding: 10px 14px;
      background: var(--bg-main);
      border-radius: var(--radius-sm);
      border: 1px solid var(--border);
      font-size: 13px;
    }

    .cancel-info-row {
      display: flex;
      align-items: center;
      gap: 6px;
      .info-label { color: var(--text-muted); }
      strong { color: var(--text-primary); }
    }

    .cancel-reasons-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }

    .reason-option-card {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 10px 12px;
      background: var(--bg-main);
      border: 1.5px solid var(--border);
      border-radius: 8px;
      color: var(--text-primary);
      cursor: pointer;
      text-align: left;
      transition: all 0.2s ease;

      &:hover {
        border-color: rgba(239, 68, 68, 0.4);
        background: rgba(239, 68, 68, 0.04);
        transform: translateY(-1px);
      }

      &.active {
        border-color: #ef4444;
        background: rgba(239, 68, 68, 0.09);
        box-shadow: 0 0 0 1px #ef4444, 0 2px 8px rgba(239, 68, 68, 0.15);

        .reason-title {
          color: #ef4444;
          font-weight: 700;
        }
      }
    }

    .reason-icon {
      font-size: 18px;
      line-height: 1;
      flex-shrink: 0;
      margin-top: 1px;
    }

    .reason-text-wrap {
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
    }

    .reason-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.2;
    }

    .reason-desc {
      font-size: 11px;
      color: var(--text-muted);
      line-height: 1.2;
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
  kitchens = signal<KitchenStation[]>([]);
  products = signal<Product[]>([]);
  filteredProducts = signal<Product[]>([]);
  loadingProducts = signal(true);

  selectedKitchenId = signal<string | undefined>(undefined);
  selectedCategoryId = signal<string | undefined>(undefined);
  searchQuery = '';

  // Categories dynamically filtered by selected Kitchen
  visibleCategories = computed(() => {
    const kId = this.selectedKitchenId();
    if (!kId) return this.categories();
    return this.categories().filter(c => c.kitchenId === kId);
  });

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
  selectedReasonId = 'CLIENT_REFUSED';
  customCancelReason = '';

  cancelReasonOptions = [
    { id: 'CLIENT_REFUSED', icon: '🙅‍♂️', title: 'Mijoz rad etdi', desc: 'Mijoz buyurtmani bekor qildi' },
    { id: 'WRONG_ITEM', icon: '⚠️', title: "Noto'g'ri urilgan", desc: 'Adashib yoki ortiqcha kiritilgan' },
    { id: 'LONG_WAIT', icon: '⏳', title: 'Uzoq kuttirildi', desc: "Kutish cho'zildi, mijoz ketib qoldi" },
    { id: 'OUT_OF_STOCK', icon: '📦', title: 'Mahsulot tugagan', desc: 'Xomashyo yoki porsiya qolmagan' },
    { id: 'KITCHEN_ISSUE', icon: '👨‍🍳', title: 'Oshxona tayyorlay olmaydi', desc: 'Oshpaz ulgurmayapti yoki texnik sabab' },
    { id: 'OTHER', icon: '✍️', title: 'Boshqa sabab', desc: "Qo'lda boshqa sabab yozish" }
  ];

  // Real-time calculation of unsent (NEW) items: any item where total quantity > sent quantity
  newItems = computed(() =>
    this.cart().filter(i => !i.voided && (i.quantity - (i.sentQuantity || 0)) > 0)
  );

  newItemsCount = computed(() =>
    this.newItems().reduce((sum, item) => sum + (item.quantity - (item.sentQuantity || 0)), 0)
  );

  canSendToKitchen = computed(() => this.newItemsCount() > 0 && !this.isSubmitting());

  subtotal = computed(() => {
    return this.cart()
      .filter(i => !i.voided)
      .reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0);
  });

  serviceCharge = computed(() => Math.round(this.subtotal() * 0.10));
  total = computed(() => this.subtotal() + this.serviceCharge());

  canProcessPayment = computed(() => {
    const user = this.auth.user();
    const role = (user?.role || '').toUpperCase();
    const username = (user?.username || '').toLowerCase();
    if (role === 'WAITER' || username === 'waiter') {
      return false;
    }
    return this.auth.hasPermission('PROCESS_PAYMENT') || role === 'ADMIN' || role === 'MANAGER' || role === 'CASHIER';
  });

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private kitchenService: KitchenService,
    private tableService: TableService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private notify: NotificationService,
    private route: ActivatedRoute,
    public router: Router,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadKitchens();
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

  loadKitchens(): void {
    this.kitchenService.getKitchens().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.kitchens.set(res.data);
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
    this.productService.getProducts(undefined, undefined, true).subscribe({
      next: (res) => {
        this.loadingProducts.set(false);
        if (res.success && res.data) {
          this.products.set(res.data);
          this.filterProducts();
          // If cart has items without images, enrich them
          this.cart.update(items =>
            items.map(item => {
              if (!item.imageUrl) {
                const p = res.data.find(x => x.id === item.productId);
                if (p?.imageUrl) {
                  return { ...item, imageUrl: p.imageUrl };
                }
              }
              return item;
            })
          );
        }
      },
      error: () => {
        this.loadingProducts.set(false);
      }
    });
  }

  selectKitchen(kitchenId?: string): void {
    this.selectedKitchenId.set(kitchenId);
    // If selected category does not belong to selected kitchen, reset it
    if (kitchenId && this.selectedCategoryId()) {
      const cat = this.categories().find(c => c.id === this.selectedCategoryId());
      if (!cat || cat.kitchenId !== kitchenId) {
        this.selectedCategoryId.set(undefined);
      }
    }
    this.filterProducts();
  }

  selectCategory(catId?: string): void {
    this.selectedCategoryId.set(catId);
    this.filterProducts();
  }

  filterProducts(): void {
    let prods = this.products();
    if (this.selectedKitchenId()) {
      prods = prods.filter(p => {
        const cat = this.categories().find(c => c.id === p.categoryId);
        return (cat?.kitchenId || p.kitchenId) === this.selectedKitchenId();
      });
    }
    if (this.selectedCategoryId()) {
      prods = prods.filter(p => p.categoryId === this.selectedCategoryId());
    }
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      prods = prods.filter(p => p.name.toLowerCase().includes(q) || (p.sku && p.sku.toLowerCase().includes(q)));
    }
    this.filteredProducts.set(prods);
  }

  getStationEmoji(code?: string): string {
    if (!code) return '👨‍🍳';
    switch (code.toUpperCase()) {
      case 'PALOV': case 'PALOVCHI': return '🥘';
      case 'SOMSA': case 'SOMSAPAZ': return '🥟';
      case 'BAR': return '🍹';
      case 'PIZZA': case 'PITSA': return '🍕';
      case 'MAIN': case 'MAIN_KITCHEN': return '👨‍🍳';
      default: return '🍳';
    }
  }

  getProductStationBadge(prod: Product): string {
    const cat = this.categories().find(c => c.id === prod.categoryId);
    const kId = cat?.kitchenId || prod.kitchenId;
    const k = this.kitchens().find(item => item.id === kId);
    return `${this.getStationEmoji(k?.code)} ${k?.name || 'Oshxona'}`;
  }

  loadExistingOrder(orderId: string): void {
    this.orderService.getOrderById(orderId).subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.updateCartFromOrder(res.data);
        }
      },
      error: (err) => {
        const msg = err.error?.message || "Bu buyurtma boshqa ofitsantga tegishli!";
        this.notify.error(msg);
        this.router.navigate(['/tables']);
      }
    });
  }

  updateCartFromOrder(order: Order): void {
    const rawItems = order.items || [];
    const mergedMap = new Map<string, PosCartItem>();

    for (const it of rawItems) {
      const isVoid = it.voided || it.kitchenStatus === 'CANCELLED';
      const key = isVoid ? `void_${it.id}` : `prod_${it.productId}`;
      const sentQty = it.sentQuantity ?? (it.kitchenStatus !== 'NEW' ? it.quantity : 0);
      const unitPrice = it.productPrice ?? it.unitPrice ?? 0;
      const existingProduct = this.products().find(p => p.id === it.productId);
      const itemImg = it.imageUrl || existingProduct?.imageUrl;

      if (!isVoid && mergedMap.has(key)) {
        const existing = mergedMap.get(key)!;
        existing.quantity += it.quantity;
        existing.sentQuantity = (existing.sentQuantity || 0) + sentQty;
        existing.subtotal = existing.quantity * existing.unitPrice;
        if (!existing.imageUrl && itemImg) {
          existing.imageUrl = itemImg;
        }
        if (existing.sentQuantity >= existing.quantity) {
          existing.kitchenStatus = 'SENT_TO_KITCHEN';
        } else if (existing.sentQuantity > 0) {
          existing.kitchenStatus = 'PARTIALLY_SENT';
        }
        existing.isNew = (existing.quantity - existing.sentQuantity) > 0;
      } else {
        const rem = Math.max(0, it.quantity - sentQty);
        mergedMap.set(key, {
          id: it.id,
          productId: it.productId,
          productName: it.productName,
          imageUrl: itemImg,
          unitPrice: unitPrice,
          quantity: it.quantity,
          sentQuantity: sentQty,
          subtotal: it.subtotal ?? (unitPrice * it.quantity),
          kitchenStatus: it.kitchenStatus || (sentQty > 0 ? 'SENT_TO_KITCHEN' : 'NEW'),
          kitchenId: it.kitchenId,
          kitchenName: it.kitchenName,
          notes: it.notes,
          voided: isVoid,
          voidReason: it.voidReason,
          isNew: rem > 0
        });
      }
    }
    this.cart.set(Array.from(mergedMap.values()));
  }

  getStatusClass(item: PosCartItem): string {
    if (item.voided || item.kitchenStatus === 'CANCELLED') return 'pill--cancelled';
    const sent = item.sentQuantity || 0;
    const rem = item.quantity - sent;
    if (sent === 0) return 'pill--new';
    if (rem > 0) return 'pill--partial';
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
    const sent = item.sentQuantity || 0;
    const rem = item.quantity - sent;
    if (sent === 0) return '🟡 Yangi';
    if (rem > 0) return `🔵 ${sent}/${item.quantity} oshxonada (+${rem} yangi)`;
    switch (item.kitchenStatus) {
      case 'SENT_TO_KITCHEN': return `🔵 Oshxonaga yuborilgan (${item.quantity}x)`;
      case 'ACCEPTED': return `🟣 Qabul qilindi (${item.quantity}x)`;
      case 'PREPARING':
      case 'COOKING': return `🟠 Tayyorlanmoqda (${item.quantity}x)`;
      case 'READY': return `🟢 Tayyor (${item.quantity}x)`;
      case 'DELIVERED':
      case 'SERVED': return `✅ Yetkazilgan (${item.quantity}x)`;
      default: return `🔵 Oshxonaga yuborilgan (${item.quantity}x)`;
    }
  }

  getItemTrackKey(item: PosCartItem, index: number): string {
    return item.voided ? `void_${item.id || index}` : `prod_${item.productId}`;
  }

  addToCart(product: Product): void {
    const price = product.salePrice || product.price || 0;
    // Look for an existing non-voided item with same productId
    const existing = this.cart().find(
      item => item.productId === product.id && !item.voided
    );

    if (existing) {
      this.cart.update(items =>
        items.map(item =>
          item === existing
            ? {
                ...item,
                quantity: item.quantity + 1,
                subtotal: price * (item.quantity + 1),
                isNew: (item.sentQuantity || 0) < (item.quantity + 1)
              }
            : item
        )
      );
    } else {
      // Add as a new item row
      const newItem: PosCartItem = {
        productId: product.id,
        productName: product.name,
        imageUrl: product.imageUrl,
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

  getImageUrl(url?: string | null): string {
    return getProductImageUrl(url);
  }

  onImageError(event: Event): void {
    handleImageError(event);
  }

  incrementQty(item: PosCartItem): void {
    this.cart.update(items =>
      items.map(i =>
        i === item
          ? {
              ...i,
              quantity: i.quantity + 1,
              subtotal: i.unitPrice * (i.quantity + 1),
              isNew: (i.sentQuantity || 0) < (i.quantity + 1)
            }
          : i
      )
    );
  }

  decrementQty(item: PosCartItem): void {
    const sent = item.sentQuantity || 0;
    const remaining = item.quantity - sent;

    if (remaining > 0) {
      // We can safely reduce the unsent portion
      if (item.quantity === 1 && sent === 0) {
        this.removeItem(item);
      } else {
        this.cart.update(items =>
          items.map(i =>
            i === item
              ? {
                  ...i,
                  quantity: i.quantity - 1,
                  subtotal: i.unitPrice * (i.quantity - 1),
                  isNew: sent < (i.quantity - 1)
                }
              : i
          )
        );
      }
    } else {
      // All items in this row have already been sent to the kitchen!
      // Must use the official cancellation modal with a reason
      this.openCancelModal(item);
    }
  }

  onQtyChange(item: PosCartItem): void {
    const sent = item.sentQuantity || 0;
    if (item.quantity < sent) {
      this.notify.warning(`Oshxonaga allaqachon ${sent} ta yuborilgan. Miqdorni bundan kamaytirish uchun "Bekor qilish" tugmasidan foydalaning.`);
      item.quantity = sent;
    }
    if (item.quantity <= 0) {
      this.removeItem(item);
    } else {
      item.subtotal = item.unitPrice * item.quantity;
      item.isNew = sent < item.quantity;
      this.cart.set([...this.cart()]);
    }
  }

  removeItem(item: PosCartItem): void {
    this.cart.update(items => items.filter(i => i !== item));
  }

  clearCart(): void {
    // If order already exists, keep already-sent items, remove only new unsent drafts
    if (this.currentOrderId()) {
      this.cart.update(items =>
        items
          .map(i => {
            const sent = i.sentQuantity || 0;
            if (sent > 0) {
              return {
                ...i,
                quantity: sent,
                subtotal: i.unitPrice * sent,
                isNew: false
              };
            }
            return null;
          })
          .filter((i): i is PosCartItem => i !== null)
      );
    } else {
      this.cart.set([]);
    }
  }

  onLeaveTable(): void {
    const table = this.selectedTable();
    const orderId = this.currentOrderId();
    const hasSentItems = this.cart().some(i => (i.sentQuantity || 0) > 0);

    // If order exists in DB but no items were ever sent to kitchen and cart has no items, auto-free table:
    if (orderId && !hasSentItems && table && (!this.cart().length || this.cart().every(i => !i.id))) {
      this.tableService.releaseTable(table.id).subscribe({
        next: () => this.router.navigate(['/tables']),
        error: () => this.router.navigate(['/tables'])
      });
    } else {
      this.router.navigate(['/tables']);
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
      // Occupied table: send ONLY new unsent quantities!
      const itemsReq: CreateOrderItemRequest[] = unsent.map(item => ({
        productId: item.productId,
        quantity: item.quantity - (item.sentQuantity || 0),
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
    this.selectedReasonId = 'CLIENT_REFUSED';
    this.cancelReason = 'Mijoz rad etdi';
    this.customCancelReason = '';
    this.cancelQty = item.sentQuantity || item.quantity;
    this.showCancelModal.set(true);
  }

  onSelectReason(opt: { id: string; title: string; desc: string }): void {
    this.selectedReasonId = opt.id;
    if (opt.id === 'OTHER') {
      this.cancelReason = this.customCancelReason.trim() || 'Boshqa sabab';
    } else {
      this.cancelReason = opt.title;
    }
  }

  onCustomReasonChange(): void {
    if (this.selectedReasonId === 'OTHER') {
      this.cancelReason = this.customCancelReason.trim() || 'Boshqa sabab';
    }
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
    if (!this.canProcessPayment()) {
      this.notify.error("Ofitsiant to'lov qabul qila olmaydi. To'lov faqat Kassa orqali amalga oshiriladi!");
      return;
    }
    this.cashReceived = this.total();
    this.showPaymentModal.set(true);
  }

  closePaymentModal(): void {
    this.showPaymentModal.set(false);
  }

  submitPayment(): void {
    if (!this.canProcessPayment()) {
      this.notify.error("Ofitsiant to'lov qabul qila olmaydi. To'lov faqat Kassa orqali amalga oshiriladi!");
      return;
    }
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
          if (res.data?.receiptPrintStatus === 'PRINT_FAILED') {
            this.notify.warning("To'lov muvaffaqiyatli qabul qilindi, ammo chek chop etishda muammo bo'ldi! Buyurtmalar ro'yxatidan qayta chop etishingiz mumkin.");
          } else {
            this.notify.success("To'lov muvaffaqiyatli qabul qilindi! Chek chop etildi.");
          }
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
