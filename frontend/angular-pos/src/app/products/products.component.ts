import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, Product, CreateProductRequest } from '../core/services/product.service';
import { CategoryService, Category } from '../core/services/category.service';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="products-page fade-in">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">🍔 Mahsulotlar & Menyu</h1>
          <p class="page-subtitle">Restoran taomlari, ichimliklari va menyusini to'liq boshqarish</p>
        </div>

        <div class="header-actions">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Taom yoki SKU qidirish..."
              [(ngModel)]="searchQuery"
              class="pos-input"
            />
          </div>

          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
            <span>➕ Yangi Mahsulot</span>
          </button>
        </div>
      </div>

      <!-- Categories Filter Tabs -->
      <div class="categories-strip">
        <button
          class="cat-chip"
          [class.active]="selectedCategoryId === null"
          (click)="selectedCategoryId = null">
          Barchasi ({{ products.length }})
        </button>
        <button
          *ngFor="let cat of categories"
          class="cat-chip"
          [class.active]="selectedCategoryId === cat.id"
          (click)="selectedCategoryId = cat.id">
          {{ cat.name }}
        </button>
      </div>

      <!-- Products Table Card -->
      <div class="pos-card table-card">
        <div *ngIf="loading && products.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Mahsulotlar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && filteredProducts.length === 0" class="empty-state">
          <div class="empty-icon">🍽️</div>
          <h3>Mahsulotlar topilmadi</h3>
          <p>Ushbu kategoriya bo'yicha yoki qidiruv natijasida mahsulot yo'q.</p>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 12px;">
            Yangi mahsulot qo'shish
          </button>
        </div>

        <div *ngIf="filteredProducts.length > 0" class="table-responsive">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Taom / Mahsulot</th>
                <th>SKU</th>
                <th>Kategoriya</th>
                <th>Oshxona</th>
                <th style="text-align: right;">Sotish narxi</th>
                <th style="text-align: right;">Tannarxi</th>
                <th>Birligi</th>
                <th>Holati</th>
                <th style="text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of filteredProducts">
                <td>
                  <div class="product-title-cell">
                    <span class="p-icon">🍲</span>
                    <div>
                      <strong>{{ p.name }}</strong>
                      <div *ngIf="p.nameUz" class="sub-name">{{ p.nameUz }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <code class="sku-tag">{{ p.sku }}</code>
                </td>
                <td>
                  <span class="cat-tag">{{ p.categoryName || getCategoryName(p.categoryId) }}</span>
                </td>
                <td>
                  <span class="kitchen-tag">
                    👨‍🍳 {{ p.kitchenName || getKitchenName(p.kitchenId) }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <strong class="price-val">{{ p.salePrice | number:'1.0-0' }} so'm</strong>
                </td>
                <td style="text-align: right; color: var(--text-muted);">
                  {{ (p.purchasePrice || 0) | number:'1.0-0' }} so'm
                </td>
                <td>
                  <span class="unit-tag">{{ p.unit || 'dona' }}</span>
                </td>
                <td>
                  <span class="status-badge" [class.active]="p.available" [class.inactive]="!p.available">
                    {{ p.available ? '● Mavjud' : '○ Tugagan' }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <div class="table-actions">
                    <button class="pos-btn pos-btn--secondary pos-btn--sm" (click)="openEditModal(p)">
                      ✏️
                    </button>
                    <button class="pos-btn pos-btn--danger pos-btn--sm" (click)="deleteProduct(p)">
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL: ADD / EDIT PRODUCT                                      -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">{{ isEditing ? '✏️ Mahsulotni tahrirlash' : '➕ Yangi Mahsulot Qo‘shish' }}</h2>
            <button class="close-btn" (click)="closeModal()">✕</button>
          </div>

          <div class="modal-body form-grid">
            <div class="form-group full-width">
              <label class="form-label">Taom / Mahsulot nomi *</label>
              <input
                type="text"
                [(ngModel)]="formData.name"
                class="pos-input"
                placeholder="Masalan: Maxsus Palov"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Kategoriya *</label>
              <select [(ngModel)]="formData.categoryId" class="pos-input">
                <option [ngValue]="undefined">Kategoriyani tanlang</option>
                <option *ngFor="let cat of categories" [value]="cat.id">
                  {{ cat.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Oshxona (KDS Stansiyasi) *</label>
              <select [(ngModel)]="formData.kitchenId" class="pos-input" required>
                <option [ngValue]="undefined">Oshxonani tanlang</option>
                <option *ngFor="let k of kitchens" [value]="k.id">
                  {{ k.name }} ({{ k.code }})
                </option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">SKU / Kod</label>
              <input
                type="text"
                [(ngModel)]="formData.sku"
                class="pos-input"
                placeholder="Avtomatik yaratiladi"
              />
            </div>

            <div class="form-group">
              <label class="form-label">Sotish narxi (so'm) *</label>
              <input
                type="number"
                [(ngModel)]="formData.salePrice"
                class="pos-input"
                placeholder="Masalan: 35000"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Tannarxi (so'm)</label>
              <input
                type="number"
                [(ngModel)]="formData.purchasePrice"
                class="pos-input"
                placeholder="Masalan: 20000"
              />
            </div>

            <div class="form-group">
              <label class="form-label">O'lchov birligi</label>
              <select [(ngModel)]="formData.unit" class="pos-input">
                <option value="dona">dona</option>
                <option value="porsiya">porsiya</option>
                <option value="kg">kg</option>
                <option value="litr">litr</option>
              </select>
            </div>

            <div class="form-group checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="formData.available" />
                <span>Menyuda mavjud (Sotuvga ruxsat)</span>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModal()" [disabled]="saving">
              Bekor qilish
            </button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveProduct()"
              [disabled]="saving || !formData.name || !formData.salePrice">
              <span>{{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .products-page {
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
        width: 220px;
      }
    }

    /* Category strip */
    .categories-strip {
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 4px;
    }

    .cat-chip {
      background: var(--bg-card);
      border: 1px solid var(--border);
      color: var(--text-secondary);
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      white-space: nowrap;
      transition: all var(--transition);

      &:hover {
        color: var(--text-primary);
        border-color: var(--border-light);
      }

      &.active {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
      }
    }

    /* Table Card */
    .table-card {
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
        padding: 12px 16px;
        border-bottom: 1px solid var(--divider);
        color: var(--text-primary);
      }

      tr:hover {
        background: var(--bg-hover);
      }
    }

    .product-title-cell {
      display: flex;
      align-items: center;
      gap: 10px;

      .p-icon {
        font-size: 20px;
        background: var(--bg-tertiary);
        padding: 4px;
        border-radius: 6px;
      }

      .sub-name {
        font-size: 12px;
        color: var(--text-muted);
      }
    }

    .sku-tag {
      font-family: var(--font-mono);
      font-size: 12px;
      color: var(--primary-light);
      background: var(--bg-secondary);
      padding: 2px 6px;
      border-radius: 4px;
    }

    .cat-tag {
      background: var(--bg-tertiary);
      padding: 4px 10px;
      border-radius: 12px;
      font-size: 12px;
    }

    .price-val {
      color: #34d399;
      font-size: 15px;
    }

    .unit-tag {
      font-size: 12px;
      color: var(--text-muted);
    }

    .status-badge {
      font-size: 12px;
      font-weight: 600;
      padding: 3px 8px;
      border-radius: 10px;

      &.active {
        background: rgba(16, 185, 129, 0.15);
        color: var(--success);
      }

      &.inactive {
        background: rgba(239, 68, 68, 0.15);
        color: var(--danger);
      }
    }

    .table-actions {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
    }

    .pos-btn--sm {
      min-height: 32px;
      padding: 4px 8px;
      font-size: 13px;
    }

    /* Modal */
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
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
      max-width: 540px;
      box-shadow: var(--shadow-lg);
      overflow: hidden;
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
      margin: 0;
    }

    .close-btn {
      background: transparent;
      border: none;
      color: var(--text-muted);
      font-size: 18px;
      cursor: pointer;
    }

    .form-grid {
      padding: 20px;
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;

      &.full-width {
        grid-column: span 2;
      }
    }

    .form-label {
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .checkbox-group {
      grid-column: span 2;
      padding-top: 6px;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 14px;
      color: var(--text-primary);
      cursor: pointer;

      input[type="checkbox"] {
        width: 18px;
        height: 18px;
        accent-color: var(--primary);
      }
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 16px 20px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);
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

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }
    .kitchen-tag {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: rgba(99, 102, 241, 0.12);
      color: #818cf8;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 12px;
      font-weight: 600;
    }
  `]
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  categories: Category[] = [];
  kitchens: KitchenStation[] = [];
  loading = false;
  searchQuery = '';
  selectedCategoryId: string | null = null;

  // Modal
  showModal = false;
  isEditing = false;
  editingId: string | null = null;
  saving = false;

  formData: CreateProductRequest = {
    name: '',
    categoryId: undefined,
    sku: '',
    unit: 'dona',
    salePrice: 0,
    purchasePrice: 0,
    available: true
  };

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private kitchenService: KitchenService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.cdr.markForCheck();

    this.kitchenService.getKitchens().subscribe({
      next: (kRes) => {
        this.kitchens = kRes.data || [];
        this.cdr.markForCheck();
      },
      error: (e) => {
        console.error('Failed to load kitchens', e);
        this.cdr.markForCheck();
      }
    });

    this.categoryService.getCategories().subscribe({
      next: (catRes) => {
        this.categories = catRes.data || [];
        this.cdr.markForCheck();

        this.productService.getProducts().subscribe({
          next: (prodRes) => {
            this.products = prodRes.data || [];
            this.loading = false;
            this.cdr.markForCheck();
          },
          error: (err) => {
            console.error('Failed to load products', err);
            this.loading = false;
            this.cdr.markForCheck();
          }
        });
      },
      error: (err) => {
        console.error('Failed to load categories', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  get filteredProducts(): Product[] {
    return this.products.filter(p => {
      if (this.selectedCategoryId && p.categoryId !== this.selectedCategoryId) {
        return false;
      }
      if (this.searchQuery.trim()) {
        const q = this.searchQuery.toLowerCase();
        return p.name.toLowerCase().includes(q) || (p.sku && p.sku.toLowerCase().includes(q));
      }
      return true;
    });
  }

  getCategoryName(catId?: string): string {
    if (!catId) return 'Umumiy';
    const c = this.categories.find(cat => cat.id === catId);
    return c ? c.name : 'Umumiy';
  }

  getKitchenName(kitchenId?: string): string {
    if (!kitchenId) return 'Biriktirilmagan';
    const k = this.kitchens.find(item => item.id === kitchenId);
    return k ? k.name : 'Biriktirilmagan';
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.formData = {
      name: '',
      categoryId: this.selectedCategoryId || (this.categories.length > 0 ? this.categories[0].id : undefined),
      kitchenId: this.kitchens.length > 0 ? this.kitchens[0].id : undefined,
      sku: 'SKU-' + Math.floor(1000 + Math.random() * 9000),
      unit: 'dona',
      salePrice: 0,
      purchasePrice: 0,
      available: true
    };
    this.showModal = true;
  }

  openEditModal(p: Product): void {
    this.isEditing = true;
    this.editingId = p.id;
    this.formData = {
      name: p.name,
      categoryId: p.categoryId,
      kitchenId: p.kitchenId,
      sku: p.sku,
      unit: p.unit || 'dona',
      salePrice: p.salePrice || p.price || 0,
      purchasePrice: p.purchasePrice || 0,
      available: p.available !== false
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveProduct(): void {
    if (!this.formData.name || !this.formData.salePrice) {
      alert('Iltimos, mahsulot nomi va sotish narxini kiriting!');
      return;
    }
    if (!this.formData.kitchenId) {
      alert('Iltimos, mahsulot tayyorlanadigan oshxonani tanlang!');
      return;
    }

    this.saving = true;
    if (this.isEditing && this.editingId) {
      this.productService.updateProduct(this.editingId, this.formData).subscribe({
        next: () => {
          this.saving = false;
          this.closeModal();
          this.loadData();
        },
        error: (err) => {
          this.saving = false;
          alert('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    } else {
      this.productService.createProduct(this.formData).subscribe({
        next: () => {
          this.saving = false;
          this.closeModal();
          this.loadData();
        },
        error: (err) => {
          this.saving = false;
          alert('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  deleteProduct(p: Product): void {
    if (!confirm(`Haqiqatan ham "${p.name}" mahsulotini o‘chirmoqchimisiz?`)) {
      return;
    }
    this.productService.deleteProduct(p.id).subscribe({
      next: () => {
        this.loadData();
      },
      error: (err) => alert('O‘chirishda xatolik: ' + (err.error?.message || err.message))
    });
  }
}
