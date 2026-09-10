import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { OrderService, Order, OrderItem, CancellationReceipt } from '../../core/services/order.service';
import { PaymentService, PaymentProcessRequest } from '../../core/services/payment.service';
import { TableService } from '../../core/services/table.service';

@Component({
  selector: 'app-orders-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="orders-page fade-in">
      <!-- Top Bar -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">📋 Buyurtmalar & Kassa</h1>
          <p class="page-subtitle">Barcha buyurtmalar monitoringi, to'lovlarni qabul qilish va chek chiqarish</p>
        </div>

        <div class="header-actions">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Qidiruv (Stol, #raqam, ofitsiant)..."
              [(ngModel)]="searchQuery"
              class="pos-input"
            />
          </div>

          <button class="pos-btn pos-btn--secondary" (click)="loadOrders()" [disabled]="loading">
            <span [class.spinning]="loading">🔄</span>
            <span>Yangilash</span>
          </button>

          <a routerLink="/pos" class="pos-btn pos-btn--primary">
            <span>➕ Yangi Buyurtma</span>
          </a>
        </div>
      </div>

      <!-- Filter Tabs & Stats -->
      <div class="filter-strip">
        <div class="tabs-group">
          <button
            class="tab-btn"
            [class.active]="activeTab === 'ALL'"
            (click)="activeTab = 'ALL'">
            Barchasi ({{ orders.length }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab === 'ACTIVE'"
            (click)="activeTab = 'ACTIVE'">
            Faol ({{ activeOrdersCount }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab === 'KITCHEN'"
            (click)="activeTab = 'KITCHEN'">
            Oshxonada ({{ kitchenOrdersCount }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab === 'READY'"
            (click)="activeTab = 'READY'">
            Tayyor ({{ readyOrdersCount }})
          </button>
          <button
            class="tab-btn"
            [class.active]="activeTab === 'PAID'"
            (click)="activeTab = 'PAID'">
            To'langan ({{ paidOrdersCount }})
          </button>
        </div>

        <div class="revenue-pill" *ngIf="todayTotalRevenue > 0">
          <span>Jami tushum:</span>
          <strong>{{ todayTotalRevenue | number:'1.0-0' }} so'm</strong>
        </div>
      </div>

      <!-- Orders Table Card -->
      <div class="pos-card orders-table-card">
        <div *ngIf="loading && orders.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Buyurtmalar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && filteredOrders.length === 0" class="empty-state">
          <div class="empty-icon">📂</div>
          <h3>Buyurtmalar topilmadi</h3>
          <p>Belgilangan filtr bo'yicha hech qanday buyurtma mavjud emas.</p>
        </div>

        <div *ngIf="filteredOrders.length > 0" class="table-responsive">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Chek #</th>
                <th>Stol / Joy</th>
                <th>Ofitsiant</th>
                <th>Mahsulotlar</th>
                <th>Jami summa</th>
                <th>Holati</th>
                <th>Vaqti</th>
                <th style="text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of filteredOrders" class="order-row">
                <td class="order-num-col">
                  <strong>#{{ order.orderNumber }}</strong>
                </td>
                <td>
                  <span class="table-tag">{{ order.tableName || order.tableNumber || 'Olib ketish' }}</span>
                </td>
                <td>
                  <span class="waiter-name">{{ order.waiterName || '—' }}</span>
                </td>
                <td>
                  <span class="items-count-badge">{{ order.items ? order.items.length : 0 }} xil taom</span>
                </td>
                <td>
                  <strong class="total-amount">{{ (order.total || order.subtotal || 0) | number:'1.0-0' }} so'm</strong>
                </td>
                <td>
                  <span class="pos-badge" [ngClass]="getStatusClass(order.status)">
                    {{ getStatusLabel(order.status) }}
                  </span>
                </td>
                <td class="time-col">
                  {{ formatTime(order.openedAt || order.createdAt) }}
                </td>
                <td class="actions-col">
                  <div class="action-buttons">
                    <!-- View Details -->
                    <button
                      class="pos-btn pos-btn--secondary pos-btn--sm"
                      title="Tafsilotlar"
                      (click)="openDetailModal(order)">
                      👁️ Ko'rish
                    </button>

                    <!-- Payment Button (Cashier) -->
                    <button
                      *ngIf="order.status !== 'PAID' && order.status !== 'CANCELLED'"
                      class="pos-btn pos-btn--success pos-btn--sm"
                      title="To'lovni qabul qilish"
                      (click)="openPaymentModal(order)">
                      💳 To'lov
                    </button>

                    <!-- Receipt Button -->
                    <button
                      class="pos-btn pos-btn--secondary pos-btn--sm"
                      title="Chek chiqarish"
                      (click)="openReceiptModal(order)">
                      🧾 Chek
                    </button>

                    <!-- Cancel / Void -->
                    <button
                      *ngIf="order.status !== 'PAID' && order.status !== 'CANCELLED'"
                      class="pos-btn pos-btn--danger pos-btn--sm"
                      title="Bekor qilish"
                      (click)="openCancelOrderModal(order, $event)">
                      ❌
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 1: ORDER DETAILS MODAL                                   -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="selectedOrder && showDetailModal" (click)="closeModals()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div>
              <h2 class="modal-title">Buyurtma #{{ selectedOrder.orderNumber }}</h2>
              <span class="table-tag" style="margin-top: 4px;">{{ selectedOrder.tableName || selectedOrder.tableNumber || 'Stol' }}</span>
            </div>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body">
            <div class="detail-meta-grid">
              <div><span>Ofitsiant:</span> <strong>{{ selectedOrder.waiterName || '—' }}</strong></div>
              <div><span>Holati:</span> <strong>{{ getStatusLabel(selectedOrder.status) }}</strong></div>
              <div><span>Ochilgan vaqt:</span> <strong>{{ formatTime(selectedOrder.openedAt || selectedOrder.createdAt) }}</strong></div>
              <div><span>Mehmonlar soni:</span> <strong>{{ selectedOrder.guestCount || 1 }} kishi</strong></div>
            </div>

            <div *ngIf="selectedOrder.notes" class="order-notes-box">
              💬 Izoh: {{ selectedOrder.notes }}
            </div>

            <h3 style="margin: 16px 0 8px; font-size: 15px; color: var(--text-secondary);">Taomlar ro'yxati:</h3>
            <table class="pos-table" style="margin-bottom: 16px;">
              <thead>
                <tr>
                  <th>Taom</th>
                  <th style="text-align: center;">Soni</th>
                  <th style="text-align: right;">Narxi</th>
                  <th style="text-align: right;">Jami</th>
                  <th>Oshxona</th>
                  <th style="text-align: right;">Amal</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let item of selectedOrder.items" [class.item-row--voided]="item.voided">
                  <td>
                    <div [style.text-decoration]="item.voided ? 'line-through' : 'none'" [style.opacity]="item.voided ? '0.65' : '1'">
                      <strong>{{ item.productName }}</strong>
                      <span *ngIf="item.voided" class="badge-cancelled" style="margin-left: 8px; font-size: 11px; padding: 2px 6px;">
                        BEKOR QILINDI
                      </span>
                    </div>
                    <div *ngIf="item.notes" style="font-size: 11px; color: #fbbf24;">{{ item.notes }}</div>
                    <div *ngIf="item.voided" class="void-audit-details" style="font-size: 11px; color: #f87171; margin-top: 3px;">
                      ⚠️ Sabab: <em>{{ item.voidReason || 'Mijoz rad etdi' }}</em>
                      <span *ngIf="item.voidedByName"> • Bekor qilgan: {{ item.voidedByName }}</span>
                      <span *ngIf="item.voidedAt"> • {{ formatTime(item.voidedAt) }}</span>
                    </div>
                  </td>
                  <td style="text-align: center; font-weight: 700;" [style.text-decoration]="item.voided ? 'line-through' : 'none'">
                    {{ item.quantity }}
                  </td>
                  <td style="text-align: right;">{{ (item.unitPrice || item.productPrice || 0) | number:'1.0-0' }}</td>
                  <td style="text-align: right; font-weight: 600;" [style.text-decoration]="item.voided ? 'line-through' : 'none'">
                    {{ item.subtotal | number:'1.0-0' }} so'm
                  </td>
                  <td>
                    <span class="kitchen-badge" [ngClass]="item.voided ? 'cancelled' : (item.kitchenStatus?.toLowerCase() || 'new')">
                      {{ item.voided ? 'CANCELLED' : (item.kitchenStatus || 'NEW') }}
                    </span>
                  </td>
                  <td style="text-align: right;">
                    <button
                      *ngIf="!item.voided && selectedOrder.status !== 'PAID' && selectedOrder.status !== 'CANCELLED'"
                      class="pos-btn pos-btn--danger pos-btn--sm"
                      style="min-height: 28px; padding: 4px 8px; font-size: 11px;"
                      title="Mahsulotni bekor qilish"
                      (click)="openCancelItemModal(selectedOrder, item, $event)">
                      🚫 Bekor qilish
                    </button>
                    <span *ngIf="item.voided" style="color: #ef4444; font-size: 11px; font-weight: 600;">Bekor qilingan</span>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Summary Totals -->
            <div class="summary-box">
              <div class="summary-line">
                <span>Oraliq summa (Subtotal):</span>
                <span>{{ selectedOrder.subtotal | number:'1.0-0' }} so'm</span>
              </div>
              <div class="summary-line" *ngIf="selectedOrder.discountAmount">
                <span>Chegirma:</span>
                <span style="color: var(--danger);">-{{ selectedOrder.discountAmount | number:'1.0-0' }} so'm</span>
              </div>
              <div class="summary-line total">
                <span>Jami to'lanishi kerak:</span>
                <strong>{{ (selectedOrder.total || selectedOrder.subtotal) | number:'1.0-0' }} so'm</strong>
              </div>
            </div>
          </div>

          <div class="modal-footer" style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px;">
            <div>
              <button
                *ngIf="selectedOrder.status !== 'PAID' && selectedOrder.status !== 'CANCELLED'"
                class="pos-btn pos-btn--danger"
                (click)="openCancelOrderModal(selectedOrder, $event)">
                🚫 Butun buyurtmani bekor qilish
              </button>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="pos-btn pos-btn--secondary" (click)="closeModals()">Yopish</button>
              <button
                *ngIf="selectedOrder.status !== 'PAID' && selectedOrder.status !== 'CANCELLED'"
                class="pos-btn pos-btn--success"
                (click)="showDetailModal = false; openPaymentModal(selectedOrder)">
                💳 To'lovga o'tish
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 2: CASHIER PAYMENT MODAL                                 -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="selectedOrder && showPaymentModal" (click)="closeModals()">
        <div class="modal-card modal-card--payment" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div>
              <h2 class="modal-title">💳 To'lovni qabul qilish</h2>
              <p class="page-subtitle">Buyurtma #{{ selectedOrder.orderNumber }} — {{ selectedOrder.tableName || 'Stol' }}</p>
            </div>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body">
            <!-- Amount Display -->
            <div class="pay-amount-display">
              <span class="label">TO'LANADIGAN SUMMA:</span>
              <span class="amount-val">{{ (selectedOrder.total || selectedOrder.subtotal) | number:'1.0-0' }} SO'M</span>
            </div>

            <!-- Payment Method Selector -->
            <div class="payment-method-tabs">
              <button
                type="button"
                class="method-btn"
                [class.selected]="payMethod === 'CASH'"
                (click)="setPaymentMethod('CASH')">
                <span class="icon">💵</span>
                <span>Naqd Pul</span>
              </button>
              <button
                type="button"
                class="method-btn"
                [class.selected]="payMethod === 'CARD'"
                (click)="setPaymentMethod('CARD')">
                <span class="icon">💳</span>
                <span>Bank Kartasi (Humo/Uzcard)</span>
              </button>
            </div>

            <!-- Cash Input Section -->
            <div *ngIf="payMethod === 'CASH'" class="cash-calc-section">
              <label class="form-label">Mijoz bergan naqd pul:</label>
              <div class="input-with-currency">
                <input
                  type="number"
                  [(ngModel)]="cashReceived"
                  (input)="calcChange()"
                  class="pos-input pos-input--lg"
                  placeholder="0"
                />
                <span class="currency">so'm</span>
              </div>

              <!-- Quick cash buttons -->
              <div class="quick-cash-row">
                <button type="button" class="quick-cash-btn" (click)="setExactCash()">Aniq summa</button>
                <button type="button" class="quick-cash-btn" (click)="addCash(50000)">50 000</button>
                <button type="button" class="quick-cash-btn" (click)="addCash(100000)">100 000</button>
                <button type="button" class="quick-cash-btn" (click)="addCash(200000)">200 000</button>
              </div>

              <!-- Change calculation result -->
              <div class="change-display" [class.negative]="changeAmount < 0">
                <span class="change-label">Qaytim (Сдача):</span>
                <span class="change-val">{{ (changeAmount >= 0 ? changeAmount : 0) | number:'1.0-0' }} so'm</span>
              </div>
              <div *ngIf="changeAmount < 0" class="error-text">
                ⚠️ Berilgan summa yetarli emas (kamida {{ (selectedOrder.total || selectedOrder.subtotal) | number:'1.0-0' }} so'm bo'lishi shart)
              </div>
            </div>

            <div *ngIf="payMethod === 'CARD'" class="card-info-box">
              <div class="card-icon">💳</div>
              <p>POS Terminal orqali to'lovni tasdiqlang:</p>
              <h3>{{ (selectedOrder.total || selectedOrder.subtotal) | number:'1.0-0' }} so'm</h3>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="processingPayment">
              Bekor qilish
            </button>
            <button
              class="pos-btn pos-btn--success pos-btn--lg"
              (click)="submitPayment()"
              [disabled]="processingPayment || (payMethod === 'CASH' && changeAmount < 0)">
              <span *ngIf="processingPayment" class="spinner-sm"></span>
              <span>{{ processingPayment ? 'To‘lov amalga oshirilmoqda...' : '✅ To‘lovni tasdiqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 3: RECEIPT PRINT MODAL                                   -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="selectedOrder && showReceiptModal" (click)="closeModals()">
        <div class="modal-card modal-card--receipt" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">🧾 Chek Chop Etish</h2>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body receipt-wrapper">
            <!-- Thermal Receipt Design -->
            <div class="receipt-paper" id="printable-receipt">
              <div class="receipt-header">
                <h2 class="restaurant-name">RESTORAN POS</h2>
                <p>Toshkent sh., Oybek ko'chasi</p>
                <p>Tel: +998 71 200-00-00</p>
                <div class="receipt-divider">--------------------------------</div>
              </div>

              <div class="receipt-meta">
                <div>Chek: #{{ selectedOrder.orderNumber }}</div>
                <div>Stol: {{ selectedOrder.tableName || selectedOrder.tableNumber || 'Stol' }}</div>
                <div>Ofitsiant: {{ selectedOrder.waiterName || 'Kassa' }}</div>
                <div>Vaqt: {{ formatTime(selectedOrder.openedAt || selectedOrder.createdAt) }}</div>
                <div class="receipt-divider">--------------------------------</div>
              </div>

              <div class="receipt-items">
                <div *ngFor="let item of selectedOrder.items" class="receipt-item-row">
                  <div class="r-item-name">{{ item.productName }}</div>
                  <div class="r-item-calc">
                    <span>{{ item.quantity }} x {{ (item.unitPrice || item.productPrice || 0) | number:'1.0-0' }}</span>
                    <span>{{ item.subtotal | number:'1.0-0' }}</span>
                  </div>
                </div>
                <div class="receipt-divider">--------------------------------</div>
              </div>

              <div class="receipt-totals">
                <div class="r-total-row">
                  <span>Oraliq summa:</span>
                  <span>{{ selectedOrder.subtotal | number:'1.0-0' }} so'm</span>
                </div>
                <div class="r-total-row" *ngIf="selectedOrder.discountAmount">
                  <span>Chegirma:</span>
                  <span>-{{ selectedOrder.discountAmount | number:'1.0-0' }} so'm</span>
                </div>
                <div class="r-total-row final">
                  <span>JAMI:</span>
                  <span>{{ (selectedOrder.total || selectedOrder.subtotal) | number:'1.0-0' }} so'm</span>
                </div>
                <div class="receipt-divider">================================</div>
              </div>

              <div class="receipt-footer">
                <p>Tashrifingiz uchun rahmat!</p>
                <p>Yoqimli ishtaha tilaymiz!</p>
                <p style="font-size: 10px; margin-top: 6px;">Powered by Oybek POS</p>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()">Yopish</button>
            <button class="pos-btn pos-btn--primary" (click)="printReceipt()">
              🖨️ Chop etish (Print)
            </button>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 4: CANCELLATION CONFIRMATION MODAL                      -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showCancelModal && selectedOrder" (click)="closeCancelModal()">
        <div class="modal-card modal-card--cancel" (click)="$event.stopPropagation()">
          <div class="modal-header modal-header--danger">
            <div>
              <h2 class="modal-title" style="color: #ef4444;">
                🚫 {{ isFullOrderCancel ? "Buyurtmani to'liq bekor qilish" : "Mahsulotni bekor qilish" }}
              </h2>
              <p class="page-subtitle">
                Buyurtma #{{ selectedOrder.orderNumber }} — {{ selectedOrder.tableName || 'Stol' }}
              </p>
            </div>
            <button class="close-btn" (click)="closeCancelModal()">✕</button>
          </div>

          <div class="modal-body">
            <!-- Warning Notice -->
            <div class="cancel-warning-banner">
              ⚠️ <strong>DIQQAT:</strong> Bekor qilish ma'lumoti tegishli oshxona paneliga REAL VAQTDA (0 refresh) yetkaziladi va bekor qilish cheki shakllantiriladi.
            </div>

            <!-- Single Item Details -->
            <div *ngIf="!isFullOrderCancel && cancellingItem" class="cancel-item-summary">
              <div class="info-row">
                <span class="info-label">Mahsulot:</span>
                <strong class="info-val">{{ cancellingItem.productName }}</strong>
              </div>
              <div class="info-row">
                <span class="info-label">Buyurtmadagi miqdor:</span>
                <span class="info-val">{{ cancellingItem.quantity }} ta</span>
              </div>
              <div class="info-row">
                <span class="info-label">Bir dona narxi:</span>
                <span class="info-val">{{ (cancellingItem.unitPrice || cancellingItem.productPrice || 0) | number:'1.0-0' }} so'm</span>
              </div>

              <!-- Quantity to Cancel with Stepper -->
              <div class="form-group" style="margin-top: 14px;">
                <label class="form-label">Bekor qilinadigan miqdor (dona):</label>
                <div class="qty-stepper-box">
                  <button type="button" class="stepper-btn" (click)="decCancelQty()" [disabled]="cancelQuantity <= 1">-</button>
                  <input
                    type="number"
                    class="pos-input stepper-input"
                    [(ngModel)]="cancelQuantity"
                    [min]="1"
                    [max]="cancellingItem.quantity"
                  />
                  <button type="button" class="stepper-btn" (click)="incCancelQty()" [disabled]="cancelQuantity >= cancellingItem.quantity">+</button>
                </div>
                <div style="font-size: 12px; color: #9ca3af; margin-top: 4px;" *ngIf="cancelQuantity < cancellingItem.quantity">
                  ℹ️ Qisman bekor qilish: <strong>{{ cancellingItem.quantity - cancelQuantity }} ta</strong> buyurtmada qoladi, <strong>{{ cancelQuantity }} ta</strong> bekor qilinadi.
                </div>
              </div>
            </div>

            <!-- Full Order Warning -->
            <div *ngIf="isFullOrderCancel" class="full-cancel-alert">
              <p>Stoldagi barcha <strong>{{ selectedOrder.items?.length || 0 }} ta</strong> mahsulot bekor qilinadi va buyurtma yopiladi.</p>
              <p style="font-size: 13px; color: #fca5a5; margin-top: 4px;">Agar stolga boshqa aktiv buyurtma qolmasa, stol avtomatik bo'shaydi (FREE).</p>
            </div>

            <!-- Reason Selector -->
            <div class="form-group" style="margin-top: 16px;">
              <label class="form-label">Bekor qilish sababi: <span style="color: #ef4444;">*</span></label>
              <select class="pos-input pos-select" [(ngModel)]="cancelReasonCategory">
                <option value="Mijoz rad etdi">Mijoz rad etdi</option>
                <option value="Noto‘g‘ri buyurtma">Noto‘g‘ri buyurtma</option>
                <option value="Mahsulot tugagan">Mahsulot tugagan</option>
                <option value="Oshxona tayyorlay olmaydi">Oshxona tayyorlay olmaydi</option>
                <option value="Boshqa">Boshqa sabab</option>
              </select>
            </div>

            <!-- Custom Reason Note -->
            <div class="form-group" style="margin-top: 12px;">
              <label class="form-label">Qo'shimcha izoh / sabab tafsiloti:</label>
              <textarea
                class="pos-input"
                rows="2"
                [(ngModel)]="cancelReasonCustom"
                placeholder="Sabab haqida qo'shimcha ma'lumot (masalan: mijoz fikrini o'zgartirdi)..."></textarea>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeCancelModal()" [disabled]="processingCancel">
              Orqaga
            </button>
            <button
              class="pos-btn pos-btn--danger pos-btn--lg"
              (click)="submitCancellation()"
              [disabled]="processingCancel">
              <span *ngIf="processingCancel" class="spinner-sm"></span>
              <span>⚠️ {{ isFullOrderCancel ? "HA, BUTUN BUYURTMANI BEKOR QILISH" : "BEKOR QILISHNI TASDIQLASH" }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 5: CANCELLATION RECEIPT MODAL (POS THERMAL 80mm)        -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showCancelReceiptModal && activeCancelReceipt" (click)="showCancelReceiptModal = false">
        <div class="modal-card modal-card--receipt" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">🧾 Bekor qilish cheki</h2>
            <button class="close-btn" (click)="showCancelReceiptModal = false">✕</button>
          </div>

          <div class="modal-body" style="background: #f3f4f6; padding: 20px; border-radius: 8px;">
            <div id="cancellation-thermal-receipt" class="pos-thermal-receipt">
              <div class="thermal-header">
                <div class="thermal-title">BEKOR QILISH CHEKI</div>
                <div class="thermal-subtitle">Restoran POS Tizimi</div>
                <div class="thermal-divider">================================</div>
              </div>

              <div class="thermal-meta">
                <div class="meta-row">
                  <span>Chek raqami:</span>
                  <strong>{{ activeCancelReceipt.receiptNumber }}</strong>
                </div>
                <div class="meta-row">
                  <span>Sana va vaqt:</span>
                  <span>{{ formatDateTime(activeCancelReceipt.createdAt) }}</span>
                </div>
                <div class="meta-row">
                  <span>Stol:</span>
                  <strong>{{ activeCancelReceipt.tableName || 'Joy' }}</strong>
                </div>
                <div class="meta-row">
                  <span>Buyurtma:</span>
                  <strong>#{{ activeCancelReceipt.orderNumber }}</strong>
                </div>
                <div class="meta-row">
                  <span>Bekor qilgan:</span>
                  <span>{{ activeCancelReceipt.cancelledByName || 'Ofitsiant' }}</span>
                </div>
              </div>

              <div class="thermal-divider">--------------------------------</div>

              <div class="thermal-items-header">
                <span class="col-name">Mahsulot</span>
                <span class="col-qty">Soni</span>
                <span class="col-sum">Summa</span>
              </div>
              <div class="thermal-divider">--------------------------------</div>

              <div class="thermal-item-row">
                <span class="col-name"><strong>{{ activeCancelReceipt.itemName }}</strong></span>
                <span class="col-qty">{{ activeCancelReceipt.cancelledQuantity }}</span>
                <span class="col-sum">{{ activeCancelReceipt.totalAmount | number:'1.0-0' }}</span>
              </div>

              <div class="thermal-divider">================================</div>

              <div class="thermal-total-row">
                <span>BEKOR QILINGAN JAMI:</span>
                <strong style="color: #dc2626;">{{ activeCancelReceipt.totalAmount | number:'1.0-0' }} so'm</strong>
              </div>

              <div class="thermal-divider">--------------------------------</div>

              <div class="thermal-reason-box">
                <div class="reason-title">Sabab:</div>
                <div class="reason-text">{{ activeCancelReceipt.reason }}</div>
              </div>

              <div class="thermal-footer">
                <div>Ushbu chek audit va oshxona</div>
                <div>hisoboti uchun chiqarildi.</div>
                <div class="thermal-divider">================================</div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="showCancelReceiptModal = false">
              Yopish
            </button>
            <button class="pos-btn pos-btn--primary pos-btn--lg" (click)="printReceipt()">
              🖨️ Chekni chiqarish (Print)
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .orders-page {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .page-header {
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

    .page-title {
      font-size: 22px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }

    .page-subtitle {
      font-size: 13px;
      color: var(--text-muted);
      margin: 4px 0 0 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .search-box {
      display: flex;
      align-items: center;
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 0 10px;
      input {
        border: none;
        background: transparent;
        color: var(--text-primary);
        font-size: 13px;
        padding: 8px;
        outline: none;
        width: 200px;
      }
    }

    /* Filter Strip */
    .filter-strip {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 12px;
    }

    .tabs-group {
      display: flex;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 3px;
      gap: 3px;
    }

    .tab-btn {
      background: transparent;
      border: none;
      color: var(--text-secondary);
      padding: 8px 16px;
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

    .revenue-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      color: var(--success);
      padding: 6px 14px;
      border-radius: 20px;
      font-size: 13px;

      strong {
        font-size: 15px;
      }
    }

    /* Table */
    .orders-table-card {
      padding: 0;
      overflow: hidden;
    }

    .table-responsive {
      overflow-x: auto;
    }

    .pos-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 14px;

      th {
        background: var(--bg-tertiary);
        color: var(--text-secondary);
        font-weight: 600;
        padding: 12px 16px;
        border-bottom: 1px solid var(--border);
      }

      td {
        padding: 14px 16px;
        border-bottom: 1px solid var(--divider);
        color: var(--text-primary);
      }

      tr.order-row:hover {
        background: var(--bg-hover);
      }
    }

    .order-num-col strong {
      font-family: var(--font-mono);
      color: var(--primary-light);
    }

    .table-tag {
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      padding: 4px 8px;
      border-radius: 4px;
      font-weight: 600;
      font-size: 13px;
    }

    .items-count-badge {
      background: rgba(255, 255, 255, 0.05);
      padding: 3px 8px;
      border-radius: 12px;
      font-size: 12px;
    }

    .total-amount {
      color: #34d399;
      font-size: 15px;
    }

    .pos-badge {
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 600;
      display: inline-block;

      &.badge-open {
        background: rgba(59, 130, 246, 0.15);
        color: #60a5fa;
      }

      &.badge-kitchen {
        background: rgba(245, 158, 11, 0.15);
        color: #fbbf24;
      }

      &.badge-ready {
        background: rgba(16, 185, 129, 0.15);
        color: #34d399;
      }

      &.badge-paid {
        background: rgba(139, 92, 246, 0.15);
        color: #c084fc;
      }

      &.badge-cancelled {
        background: rgba(239, 68, 68, 0.15);
        color: #f87171;
      }
    }

    .time-col {
      color: var(--text-muted);
      font-size: 13px;
    }

    .action-buttons {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
    }

    .pos-btn--sm {
      min-height: 32px;
      padding: 4px 10px;
      font-size: 12px;
    }

    /* Modal Overlay & Card */
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(4px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 20px;
    }

    .modal-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 600px;
      max-height: 90vh;
      display: flex;
      flex-direction: column;
      box-shadow: var(--shadow-lg);
      overflow: hidden;

      &--payment {
        max-width: 500px;
      }

      &--receipt {
        max-width: 420px;
      }
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      background: var(--bg-tertiary);
      border-bottom: 1px solid var(--border);
    }

    .modal-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }

    .close-btn {
      background: transparent;
      border: none;
      color: var(--text-muted);
      font-size: 18px;
      cursor: pointer;
      &:hover { color: var(--text-primary); }
    }

    .modal-body {
      padding: 20px;
      overflow-y: auto;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 16px 20px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);
    }

    .detail-meta-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 10px;
      background: var(--bg-secondary);
      padding: 12px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      color: var(--text-secondary);

      strong {
        color: var(--text-primary);
        margin-left: 6px;
      }
    }

    .order-notes-box {
      margin-top: 10px;
      background: rgba(245, 158, 11, 0.1);
      color: #fbbf24;
      padding: 8px 12px;
      border-radius: var(--radius-sm);
      font-size: 13px;
    }

    .kitchen-badge {
      font-size: 11px;
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;

      &.new { background: #3b82f6; color: white; }
      &.cooking { background: #f59e0b; color: white; }
      &.ready { background: #10b981; color: white; }
      &.served { background: #64748b; color: white; }
    }

    .summary-box {
      background: var(--bg-secondary);
      border-radius: var(--radius-sm);
      padding: 12px 16px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .summary-line {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      color: var(--text-secondary);

      &.total {
        font-size: 16px;
        font-weight: 700;
        color: var(--text-primary);
        border-top: 1px solid var(--divider);
        padding-top: 8px;
        strong {
          color: #34d399;
          font-size: 18px;
        }
      }
    }

    /* Payment Specific */
    .pay-amount-display {
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.2), rgba(16, 185, 129, 0.2));
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 20px;
      text-align: center;
      margin-bottom: 20px;

      .label {
        font-size: 12px;
        letter-spacing: 1px;
        color: var(--text-muted);
        display: block;
        margin-bottom: 6px;
      }

      .amount-val {
        font-size: 28px;
        font-weight: 800;
        color: #34d399;
      }
    }

    .payment-method-tabs {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-bottom: 20px;
    }

    .method-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      background: var(--bg-secondary);
      border: 2px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      cursor: pointer;
      color: var(--text-secondary);
      font-weight: 600;
      font-size: 13px;
      transition: all var(--transition);

      .icon {
        font-size: 28px;
      }

      &:hover {
        border-color: var(--border-light);
      }

      &.selected {
        border-color: var(--primary);
        background: rgba(99, 102, 241, 0.15);
        color: var(--text-primary);
      }
    }

    .cash-calc-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .form-label {
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .input-with-currency {
      position: relative;
      input {
        width: 100%;
        font-size: 20px;
        font-weight: 700;
        padding-right: 60px;
      }
      .currency {
        position: absolute;
        right: 14px;
        top: 50%;
        transform: translateY(-50%);
        color: var(--text-muted);
        font-weight: 600;
      }
    }

    .quick-cash-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 8px;
    }

    .quick-cash-btn {
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      color: var(--text-primary);
      padding: 8px 4px;
      border-radius: var(--radius-sm);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      &:hover {
        background: var(--bg-hover);
        border-color: var(--primary);
      }
    }

    .change-display {
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: rgba(16, 185, 129, 0.15);
      border: 1px solid rgba(16, 185, 129, 0.3);
      padding: 12px 16px;
      border-radius: var(--radius-sm);
      margin-top: 8px;

      .change-label {
        font-size: 14px;
        color: var(--text-secondary);
      }

      .change-val {
        font-size: 20px;
        font-weight: 800;
        color: var(--success);
      }

      &.negative {
        background: rgba(239, 68, 68, 0.1);
        border-color: rgba(239, 68, 68, 0.3);
        .change-val { color: var(--danger); }
      }
    }

    .error-text {
      font-size: 12px;
      color: #f87171;
    }

    .card-info-box {
      text-align: center;
      padding: 24px;
      background: var(--bg-secondary);
      border-radius: var(--radius-md);

      .card-icon {
        font-size: 40px;
        margin-bottom: 12px;
      }

      h3 {
        color: #34d399;
        font-size: 22px;
        margin-top: 8px;
      }
    }

    /* Receipt Paper */
    .receipt-paper {
      background: white;
      color: #111827;
      font-family: var(--font-mono);
      font-size: 12px;
      padding: 20px;
      border-radius: 4px;
      box-shadow: 0 4px 12px rgba(0,0,0,0.15);
      line-height: 1.4;
    }

    .receipt-header, .receipt-footer {
      text-align: center;
    }

    .restaurant-name {
      font-size: 18px;
      font-weight: 800;
      margin-bottom: 4px;
    }

    .receipt-divider {
      margin: 8px 0;
      color: #9ca3af;
      overflow: hidden;
      white-space: nowrap;
    }

    .receipt-item-row {
      margin-bottom: 6px;
    }

    .r-item-name {
      font-weight: 700;
    }

    .r-item-calc {
      display: flex;
      justify-content: space-between;
      color: #4b5563;
      font-size: 11px;
    }

    .r-total-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 4px;

      &.final {
        font-size: 15px;
        font-weight: 800;
        margin-top: 6px;
      }
    }

    .loading-state, .empty-state {
      padding: 50px 20px;
      text-align: center;
      color: var(--text-secondary);
    }

    .empty-icon {
      font-size: 48px;
      margin-bottom: 12px;
    }

    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid var(--border);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 12px;
    }

    .spinner-sm {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid white;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .spinning {
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }

    /* Cancellation Styles */
    .modal-card--cancel {
      max-width: 520px;
      width: 95%;
    }

    .modal-header--danger {
      border-bottom: 2px solid rgba(239, 68, 68, 0.4);
    }

    .cancel-warning-banner {
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: #ef4444;
      padding: 10px 14px;
      border-radius: var(--radius-sm);
      font-size: 13px;
      line-height: 1.4;
      margin-bottom: 14px;
    }

    .cancel-item-summary {
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      padding: 12px 14px;
      margin-bottom: 12px;
    }

    .info-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
      font-size: 13px;
    }

    .info-label {
      color: var(--text-secondary);
    }

    .info-val {
      color: var(--text-primary);
    }

    .full-cancel-alert {
      background: rgba(220, 38, 38, 0.15);
      border: 1px solid rgba(220, 38, 38, 0.4);
      color: #fca5a5;
      padding: 12px 14px;
      border-radius: var(--radius-sm);
      font-size: 14px;
      margin-bottom: 14px;
    }

    .qty-stepper-box {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 6px;
    }

    .stepper-btn {
      width: 38px;
      height: 38px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      color: var(--text-primary);
      border-radius: var(--radius-sm);
      font-size: 18px;
      font-weight: 700;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition);

      &:hover:not(:disabled) {
        border-color: var(--primary);
        background: var(--bg-hover);
      }

      &:disabled {
        opacity: 0.4;
        cursor: not-allowed;
      }
    }

    .stepper-input {
      width: 90px;
      text-align: center;
      font-weight: 700;
      font-size: 16px;
    }

    .pos-btn--danger {
      background: #dc2626;
      color: white;
      border: none;

      &:hover:not(:disabled) {
        background: #b91c1c;
      }
    }

    .badge-cancelled, .kitchen-badge.cancelled {
      background: rgba(239, 68, 68, 0.2);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.4);
      padding: 2px 8px;
      border-radius: 4px;
      font-weight: 700;
      font-size: 11px;
    }

    .item-row--voided {
      background: rgba(239, 68, 68, 0.04);
    }

    /* POS Thermal 80mm Receipt Styles */
    .pos-thermal-receipt {
      background: #ffffff;
      color: #111827;
      font-family: 'Courier New', Courier, monospace;
      width: 320px;
      margin: 0 auto;
      padding: 16px 20px;
      box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
      border-radius: 4px;
      font-size: 13px;
      line-height: 1.4;
    }

    .thermal-header {
      text-align: center;
      margin-bottom: 6px;
    }

    .thermal-title {
      font-size: 16px;
      font-weight: 900;
      letter-spacing: 1px;
    }

    .thermal-subtitle {
      font-size: 12px;
      color: #4b5563;
    }

    .thermal-divider {
      color: #9ca3af;
      text-align: center;
      margin: 4px 0;
      white-space: nowrap;
      overflow: hidden;
      font-size: 11px;
    }

    .thermal-meta .meta-row {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      margin-bottom: 3px;
    }

    .thermal-items-header {
      display: flex;
      justify-content: space-between;
      font-weight: 700;
      font-size: 12px;
      padding: 2px 0;
    }

    .thermal-item-row {
      display: flex;
      justify-content: space-between;
      font-size: 13px;
      margin: 4px 0;
    }

    .col-name { flex: 2; text-align: left; }
    .col-qty { flex: 1; text-align: center; font-weight: 700; }
    .col-sum { flex: 1.5; text-align: right; font-weight: 700; }

    .thermal-total-row {
      display: flex;
      justify-content: space-between;
      font-size: 14px;
      font-weight: 800;
      margin: 6px 0;
    }

    .thermal-reason-box {
      font-size: 12px;
      margin: 6px 0;
      background: #fef2f2;
      padding: 6px 8px;
      border-radius: 4px;
      border: 1px dashed #fca5a5;
    }

    .reason-title {
      font-weight: 700;
      color: #b91c1c;
      margin-bottom: 2px;
    }

    .reason-text {
      color: #1f2937;
      word-break: break-word;
    }

    .thermal-footer {
      text-align: center;
      font-size: 11px;
      color: #6b7280;
      margin-top: 8px;
    }

    @media print {
      body * {
        visibility: hidden !important;
      }
      #cancellation-thermal-receipt, #cancellation-thermal-receipt * {
        visibility: visible !important;
      }
      #cancellation-thermal-receipt {
        position: absolute !important;
        left: 0 !important;
        top: 0 !important;
        width: 80mm !important;
        margin: 0 !important;
        padding: 5mm !important;
        background: white !important;
        color: black !important;
        font-family: 'Courier New', monospace !important;
        box-shadow: none !important;
      }
    }
  `]
})
export class OrdersListComponent implements OnInit {
  orders: Order[] = [];
  loading = false;
  searchQuery = '';
  activeTab: 'ALL' | 'ACTIVE' | 'KITCHEN' | 'READY' | 'PAID' = 'ALL';

  // Modals
  selectedOrder: Order | null = null;
  showDetailModal = false;
  showPaymentModal = false;
  showReceiptModal = false;

  // Cancellation state
  showCancelModal = false;
  isFullOrderCancel = false;
  cancellingItem: OrderItem | null = null;
  cancelQuantity: number = 1;
  cancelReasonCategory: string = 'Mijoz rad etdi';
  cancelReasonCustom: string = '';
  processingCancel = false;

  // Cancellation receipt modal
  showCancelReceiptModal = false;
  activeCancelReceipt: CancellationReceipt | null = null;

  // Payment form state
  payMethod: 'CASH' | 'CARD' = 'CASH';
  cashReceived: number = 0;
  changeAmount: number = 0;
  processingPayment = false;

  constructor(
    private orderService: OrderService,
    private paymentService: PaymentService,
    private tableService: TableService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.orderService.getActiveOrders().subscribe({
      next: (res) => {
        if (res.data) {
          this.orders = res.data;
        }
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load orders', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get filteredOrders(): Order[] {
    return this.orders.filter(order => {
      // Tab filter
      if (this.activeTab === 'ACTIVE') {
        if (order.status === 'PAID' || order.status === 'CANCELLED') return false;
      } else if (this.activeTab === 'KITCHEN') {
        if (order.status !== 'SENT_TO_KITCHEN' && order.status !== 'PREPARING') return false;
      } else if (this.activeTab === 'READY') {
        if (order.status !== 'READY') return false;
      } else if (this.activeTab === 'PAID') {
        if (order.status !== 'PAID') return false;
      }

      // Search filter
      if (this.searchQuery.trim()) {
        const q = this.searchQuery.toLowerCase();
        const numMatch = order.orderNumber?.toLowerCase().includes(q);
        const tblMatch = (order.tableName || order.tableNumber)?.toLowerCase().includes(q);
        const waiterMatch = order.waiterName?.toLowerCase().includes(q);
        return numMatch || tblMatch || waiterMatch;
      }

      return true;
    });
  }

  get activeOrdersCount(): number {
    return this.orders.filter(o => o.status !== 'PAID' && o.status !== 'CANCELLED').length;
  }

  get kitchenOrdersCount(): number {
    return this.orders.filter(o => o.status === 'SENT_TO_KITCHEN' || o.status === 'PREPARING').length;
  }

  get readyOrdersCount(): number {
    return this.orders.filter(o => o.status === 'READY').length;
  }

  get paidOrdersCount(): number {
    return this.orders.filter(o => o.status === 'PAID').length;
  }

  get todayTotalRevenue(): number {
    return this.orders
      .filter(o => o.status === 'PAID')
      .reduce((sum, o) => sum + (o.total || o.subtotal || 0), 0);
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN':
      case 'DRAFT':
        return 'badge-open';
      case 'SENT_TO_KITCHEN':
      case 'PREPARING':
        return 'badge-kitchen';
      case 'READY':
        return 'badge-ready';
      case 'PAID':
      case 'COMPLETED':
        return 'badge-paid';
      case 'CANCELLED':
        return 'badge-cancelled';
      default:
        return 'badge-open';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'OPEN': return 'Ochiq';
      case 'DRAFT': return 'Qoralama';
      case 'SENT_TO_KITCHEN': return 'Oshxonada';
      case 'PREPARING': return 'Tayyorlanmoqda';
      case 'READY': return 'Tayyor';
      case 'PAID': return 'To‘langan';
      case 'COMPLETED': return 'Yakunlangan';
      case 'CANCELLED': return 'Bekor qilingan';
      default: return status;
    }
  }

  formatTime(isoStr?: string): string {
    if (!isoStr) return '—';
    try {
      const d = new Date(isoStr);
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoStr;
    }
  }

  openDetailModal(order: Order): void {
    this.selectedOrder = order;
    this.showDetailModal = true;
  }

  openPaymentModal(order: Order): void {
    this.selectedOrder = order;
    this.payMethod = 'CASH';
    const total = order.total || order.subtotal || 0;
    this.cashReceived = total;
    this.changeAmount = 0;
    this.showPaymentModal = true;
  }

  openReceiptModal(order: Order): void {
    this.selectedOrder = order;
    this.showReceiptModal = true;
  }

  closeModals(): void {
    this.showDetailModal = false;
    this.showPaymentModal = false;
    this.showReceiptModal = false;
  }

  setPaymentMethod(method: 'CASH' | 'CARD'): void {
    this.payMethod = method;
    if (this.selectedOrder) {
      const total = this.selectedOrder.total || this.selectedOrder.subtotal || 0;
      this.cashReceived = total;
      this.calcChange();
    }
  }

  calcChange(): void {
    if (!this.selectedOrder) return;
    const total = this.selectedOrder.total || this.selectedOrder.subtotal || 0;
    this.changeAmount = (this.cashReceived || 0) - total;
  }

  setExactCash(): void {
    if (!this.selectedOrder) return;
    this.cashReceived = this.selectedOrder.total || this.selectedOrder.subtotal || 0;
    this.calcChange();
  }

  addCash(amount: number): void {
    this.cashReceived = (this.cashReceived || 0) + amount;
    this.calcChange();
  }

  submitPayment(): void {
    if (!this.selectedOrder) return;
    const total = this.selectedOrder.total || this.selectedOrder.subtotal || 0;

    this.processingPayment = true;
    const req: PaymentProcessRequest = {
      orderId: this.selectedOrder.id,
      paymentMethod: this.payMethod,
      amount: total,
      cashAmount: this.payMethod === 'CASH' ? this.cashReceived : 0,
      cardAmount: this.payMethod === 'CARD' ? total : 0,
      changeAmount: this.payMethod === 'CASH' ? Math.max(0, this.changeAmount) : 0,
      notes: `Kassa to'lovi: ${this.payMethod}`
    };

    this.paymentService.processPayment(req).subscribe({
      next: () => {
        this.processingPayment = false;
        // Also update table status to FREE if tableId exists
        if (this.selectedOrder?.tableId) {
          this.tableService.updateTableStatus(this.selectedOrder.tableId, 'FREE').subscribe({
            next: () => console.log('Table freed successfully'),
            error: (e) => console.warn('Could not free table', e)
          });
        }
        alert('To‘lov muvaffaqiyatli qabul qilindi! Stol bo‘shatildi.');
        this.closeModals();
        this.loadOrders();
      },
      error: (err) => {
        this.processingPayment = false;
        alert('To‘lovda xatolik yuz berdi: ' + (err.error?.message || err.message));
      }
    });
  }

  openCancelItemModal(order: Order, item: OrderItem, event?: Event): void {
    if (event) event.stopPropagation();
    this.selectedOrder = order;
    this.cancellingItem = item;
    this.isFullOrderCancel = false;
    this.cancelQuantity = item.quantity;
    this.cancelReasonCategory = 'Mijoz rad etdi';
    this.cancelReasonCustom = '';
    this.showCancelModal = true;
  }

  openCancelOrderModal(order?: Order, event?: Event): void {
    if (event) event.stopPropagation();
    if (order) this.selectedOrder = order;
    this.cancellingItem = null;
    this.isFullOrderCancel = true;
    this.cancelReasonCategory = 'Mijoz rad etdi';
    this.cancelReasonCustom = '';
    this.showCancelModal = true;
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.cancellingItem = null;
    this.processingCancel = false;
  }

  incCancelQty(): void {
    if (this.cancellingItem && this.cancelQuantity < this.cancellingItem.quantity) {
      this.cancelQuantity++;
    }
  }

  decCancelQty(): void {
    if (this.cancelQuantity > 1) {
      this.cancelQuantity--;
    }
  }

  get finalCancelReason(): string {
    if (this.cancelReasonCategory === 'Boshqa') {
      return this.cancelReasonCustom.trim() || 'Boshqa sabab';
    }
    if (this.cancelReasonCustom.trim()) {
      return `${this.cancelReasonCategory} (${this.cancelReasonCustom.trim()})`;
    }
    return this.cancelReasonCategory;
  }

  submitCancellation(): void {
    if (!this.selectedOrder) return;

    if (this.cancelReasonCategory === 'Boshqa' && !this.cancelReasonCustom.trim()) {
      alert('Iltimos, bekor qilish sababini yozing!');
      return;
    }

    this.processingCancel = true;

    if (!this.isFullOrderCancel && this.cancellingItem && this.cancellingItem.id) {
      this.orderService.cancelItem(this.selectedOrder.id, this.cancellingItem.id, {
        reason: this.finalCancelReason,
        quantity: this.cancelQuantity
      }).subscribe({
        next: (res) => {
          this.processingCancel = false;
          this.showCancelModal = false;
          if (res.data) {
            this.activeCancelReceipt = res.data.receipt;
            this.showCancelReceiptModal = true;
            this.selectedOrder = res.data.order;
            const idx = this.orders.findIndex(o => o.id === res.data.order.id);
            if (idx >= 0) {
              this.orders[idx] = res.data.order;
            }
          }
          this.loadOrders();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.processingCancel = false;
          alert('Mahsulotni bekor qilishda xatolik: ' + (err.error?.message || err.message));
          this.cdr.markForCheck();
        }
      });
    } else {
      this.orderService.cancelOrder(this.selectedOrder.id, {
        reason: this.finalCancelReason
      }).subscribe({
        next: (res) => {
          this.processingCancel = false;
          this.showCancelModal = false;
          if (res.data) {
            this.activeCancelReceipt = res.data.receipt;
            this.showCancelReceiptModal = true;
            this.selectedOrder = res.data.order;
            const idx = this.orders.findIndex(o => o.id === res.data.order.id);
            if (idx >= 0) {
              this.orders[idx] = res.data.order;
            }
          }
          this.loadOrders();
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.processingCancel = false;
          alert('Buyurtmani bekor qilishda xatolik: ' + (err.error?.message || err.message));
          this.cdr.markForCheck();
        }
      });
    }
  }

  formatDateTime(isoStr?: string): string {
    if (!isoStr) return '';
    try {
      const d = new Date(isoStr);
      return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoStr;
    }
  }

  printReceipt(): void {
    window.print();
  }
}
