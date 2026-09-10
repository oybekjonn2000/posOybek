import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService, Product } from '../../core/services/product.service';
import { CategoryService, Category } from '../../core/services/category.service';
import { TableService, RestaurantTable } from '../../core/services/table.service';
import { OrderService, CreateOrderRequest } from '../../core/services/order.service';
import { PaymentService } from '../../core/services/payment.service';
import { NotificationService } from '../../core/services/notification.service';

interface CartItem {
  product: Product;
  quantity: number;
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
            @for (item of cart(); track item.product.id) {
              <div class="cart-item">
                <div class="cart-item__details">
                  <div class="cart-item__title">{{ item.product.name }}</div>
                  <div class="cart-item__unit-price">{{ formatPrice(item.product.salePrice || item.product.price || 0) }}</div>
                </div>

                <div class="cart-item__qty-controls">
                  <button class="qty-btn" (click)="decrementQty(item)">−</button>
                  <input type="number" class="qty-input" [(ngModel)]="item.quantity" min="1" (change)="onQtyChange(item)" />
                  <button class="qty-btn" (click)="incrementQty(item)">+</button>
                </div>

                <div class="cart-item__subtotal">
                  {{ formatPrice((item.product.salePrice || item.product.price || 0) * item.quantity) }}
                </div>

                <button class="cart-item__remove" (click)="removeItem(item)">✕</button>
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
                    [disabled]="cart().length === 0 || isSubmitting()"
                    (click)="sendToKitchen()">
              👨‍🍳 Oshxonaga Yuborish
            </button>
            <button class="btn-pay" 
                    [disabled]="cart().length === 0 || isSubmitting()"
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
    </div>
  `,
  styles: [`
    .pos-screen {
      display: grid;
      grid-template-columns: 1fr 400px;
      height: calc(100vh - 64px);
      background: var(--bg-main);
      overflow: hidden;
    }
    .pos-menu {
      display: flex;
      flex-direction: column;
      border-right: 1px solid var(--border);
      overflow: hidden;
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
    }
    .cart-header {
      padding: 16px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border);
    }
    .cart-title {
      font-size: 18px;
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
      flex: 1;
      padding: 16px;
      overflow-y: auto;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .cart-empty {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
      &__icon { font-size: 40px; margin-bottom: 12px; }
    }
    .cart-item {
      display: grid;
      grid-template-columns: 1fr auto auto auto;
      align-items: center;
      gap: 10px;
      padding: 10px;
      background: var(--bg-main);
      border-radius: var(--radius-sm);
      border: 1px solid var(--border);

      &__title {
        font-size: 14px;
        font-weight: 700;
        color: var(--text-primary);
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
        font-size: 14px;
      }
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
      padding: 16px 20px;
      border-top: 1px solid var(--border);
      background: var(--bg-card);
    }
    .summary-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 13px;
      color: var(--text-secondary);

      &--total {
        margin-top: 12px;
        padding-top: 12px;
        border-top: 1px dashed var(--border);
        font-size: 17px;
        font-weight: 800;
        color: var(--text-primary);
      }
    }
    .cart-actions {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 12px;
      margin-top: 16px;
    }
    .btn-kitchen, .btn-pay {
      padding: 12px;
      border: none;
      border-radius: var(--radius-md);
      font-weight: 700;
      font-size: 14px;
      cursor: pointer;
      transition: all var(--transition);

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

  cart = signal<CartItem[]>([]);
  isSubmitting = signal(false);

  showPaymentModal = signal(false);
  paymentMethod: 'CASH' | 'CARD' = 'CASH';
  cashReceived = 0;

  subtotal = computed(() => {
    return this.cart().reduce((sum, item) => {
      const price = item.product.salePrice || item.product.price || 0;
      return sum + (price * item.quantity);
    }, 0);
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
          const items: CartItem[] = res.data.items.map(it => ({
            product: {
              id: it.productId,
              name: it.productName,
              salePrice: it.productPrice ?? it.unitPrice ?? 0,
              sku: '',
              unit: 'ta',
              active: true,
              available: true,
              trackStock: false
            },
            quantity: it.quantity
          }));
          this.cart.set(items);
        }
      }
    });
  }

  addToCart(product: Product): void {
    const existing = this.cart().find(item => item.product.id === product.id);
    if (existing) {
      this.cart.update(items =>
        items.map(item =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        )
      );
    } else {
      this.cart.update(items => [...items, { product, quantity: 1 }]);
    }
  }

  incrementQty(item: CartItem): void {
    this.cart.update(items =>
      items.map(i => i.product.id === item.product.id ? { ...i, quantity: i.quantity + 1 } : i)
    );
  }

  decrementQty(item: CartItem): void {
    if (item.quantity <= 1) {
      this.removeItem(item);
    } else {
      this.cart.update(items =>
        items.map(i => i.product.id === item.product.id ? { ...i, quantity: i.quantity - 1 } : i)
      );
    }
  }

  onQtyChange(item: CartItem): void {
    if (item.quantity <= 0) {
      this.removeItem(item);
    }
  }

  removeItem(item: CartItem): void {
    this.cart.update(items => items.filter(i => i.product.id !== item.product.id));
  }

  clearCart(): void {
    this.cart.set([]);
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('uz-UZ').format(price) + ' UZS';
  }

  sendToKitchen(): void {
    if (this.cart().length === 0) return;
    this.isSubmitting.set(true);

    const req: CreateOrderRequest = {
      tableId: this.selectedTable()?.id,
      orderType: 'DINE_IN',
      guestCount: 2,
      notes: 'Oshxonaga yuborildi',
      items: this.cart().map(item => ({
        productId: item.product.id,
        quantity: item.quantity
      }))
    };

    this.orderService.createOrder(req).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        if (res.success && res.data) {
          this.currentOrderId.set(res.data.id);
          this.notify.success('Buyurtma oshxonaga yuborildi va stol band qilindi!');
          this.router.navigate(['/tables']);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.notify.error(err.error?.message || 'Buyurtma yuborishda xatolik yuz berdi');
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
    if (this.cart().length === 0) return;
    this.isSubmitting.set(true);

    // If no order created yet, create order first then pay
    if (!this.currentOrderId()) {
      const orderReq: CreateOrderRequest = {
        tableId: this.selectedTable()?.id,
        orderType: 'DINE_IN',
        guestCount: 2,
        items: this.cart().map(item => ({
          productId: item.product.id,
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
