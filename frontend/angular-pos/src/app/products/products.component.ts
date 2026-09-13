import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ProductService, Product, CreateProductRequest } from '../core/services/product.service';
import { CategoryService, Category } from '../core/services/category.service';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';
import { getProductImageUrl, handleImageError } from '../core/utils/product-image.util';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  template: `
    <div class="products-page fade-in">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-left">
          <h1 class="page-title">🍔 Mahsulotlar & Menyu</h1>
          <p class="page-subtitle">
            Ierarxiya: <strong>Oshxona ➔ Kategoriya ➔ Mahsulot</strong>. Mahsulot oshxonani faqat o'z kategoriyasi orqali aniqlaydi.
          </p>
        </div>

        <div class="header-actions">
          <div class="search-box">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              placeholder="Taom yoki SKU qidirish..."
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearchChange()"
              class="pos-input"
            />
          </div>

          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
            <span>➕ Yangi Mahsulot</span>
          </button>
        </div>
      </div>

      <!-- 1. KITCHEN FILTER STRIP -->
      <div class="filter-strip kitchen-strip">
        <span class="filter-title">Oshxona:</span>
        <button
          class="filter-chip"
          [class.active]="selectedKitchenId === null"
          (click)="selectKitchenFilter(null)">
          <span>🍽️ Barcha Oshxonalar</span>
          <span class="count-pill">{{ products.length }}</span>
        </button>
        <button
          *ngFor="let k of kitchens"
          class="filter-chip"
          [class.active]="selectedKitchenId === k.id"
          (click)="selectKitchenFilter(k.id)">
          <span>{{ getKitchenEmoji(k.code) }} {{ k.name }}</span>
          <span class="count-pill">{{ getProductsCountForKitchen(k.id) }}</span>
        </button>
      </div>

      <!-- 2. CATEGORY FILTER STRIP (Filtered by selected Kitchen) -->
      <div class="filter-strip category-strip">
        <span class="filter-title">Kategoriya:</span>
        <button
          class="filter-chip filter-chip--sm"
          [class.active]="selectedCategoryId === null"
          (click)="selectCategoryFilter(null)">
          Barcha bo'limlar
        </button>
        <button
          *ngFor="let cat of visibleCategories"
          class="filter-chip filter-chip--sm"
          [class.active]="selectedCategoryId === cat.id"
          (click)="selectCategoryFilter(cat.id)">
          {{ cat.name }} ({{ cat.productCount || getProductsCountForCategory(cat.id) }})
        </button>
      </div>

      <!-- Products Table Card -->
      <div class="pos-card table-card">
        <div *ngIf="loading && products.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Mahsulotlar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && totalItems === 0" class="empty-state">
          <div class="empty-icon">🍽️</div>
          <h3>Mahsulotlar topilmadi</h3>
          <p>Ushbu oshxona/kategoriya bo'yicha yoki qidiruv natijasida mahsulot yo'q.</p>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 12px;">
            Yangi mahsulot qo'shish
          </button>
        </div>

        <div *ngIf="totalItems > 0" class="table-responsive">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Taom / Mahsulot</th>
                <th>SKU</th>
                <th>Kategoriya</th>
                <th>Oshxona (KDS)</th>
                <th style="text-align: right;">Sotish narxi</th>
                <th style="text-align: right;">Tannarxi</th>
                <th>Birligi</th>
                <th>Holati</th>
                <th style="text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let p of pagedProducts">
                <td>
                  <div class="product-title-cell">
                    <div class="product-thumb-wrap">
                      <img
                        *ngIf="p.imageUrl"
                        [src]="getImageUrl(p.imageUrl)"
                        (error)="onImageError($event)"
                        class="product-thumb"
                        alt="Product"
                        loading="lazy"
                      />
                      <span *ngIf="!p.imageUrl" class="p-icon">{{ getProductEmoji(p.name, p.categoryName) }}</span>
                    </div>
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
                  <span class="kitchen-badge-pill">
                    {{ getKitchenEmojiForProduct(p) }} {{ p.kitchenName || getKitchenNameForProduct(p) }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <strong class="price-val">{{ (p.salePrice || p.price || 0) | number:'1.0-0' }} so'm</strong>
                </td>
                <td style="text-align: right; color: var(--text-muted);">
                  {{ (p.purchasePrice || 0) | number:'1.0-0' }} so'm
                </td>
                <td>
                  <span class="unit-tag">{{ p.unit || 'dona' }}</span>
                </td>
                <td>
                  <span class="status-badge" [class.active]="p.available !== false" [class.inactive]="p.available === false">
                    {{ p.available !== false ? '● Mavjud' : '○ Tugagan' }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <div class="table-actions">
                    <button class="pos-btn pos-btn--secondary pos-btn--sm" title="Tahrirlash" (click)="openEditModal(p)">
                      ✏️
                    </button>
                    <button class="pos-btn pos-btn--danger pos-btn--sm" title="O'chirish" (click)="deleteProduct(p)">
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <mat-paginator
            [length]="totalItems"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex"
            [pageSizeOptions]="pageSizeOptions"
            [showFirstLastButtons]="true"
            (page)="onPageChange($event)">
          </mat-paginator>
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
            <!-- KATEGORIYA -->
            <div class="form-group full-width">
              <label class="form-label">
                Kategoriya <span class="required-star">*</span>
              </label>
              <select
                [(ngModel)]="formData.categoryId"
                class="pos-input"
                required>
                <option [ngValue]="''" disabled selected>Kategoriyani tanlang ▼</option>
                <option *ngFor="let cat of categories" [value]="cat.id">
                  {{ cat.name }} ({{ cat.kitchenName || getKitchenName(cat.kitchenId) }})
                </option>
              </select>
              <div *ngIf="formData.categoryId && getSelectedCategoryKitchen(formData.categoryId)" class="auto-kitchen-badge">
                <span class="kitchen-pill">{{ getSelectedCategoryKitchen(formData.categoryId) }}</span>
                <span class="kitchen-note">Oshxona kategoriya orqali avtomatik aniqlanadi</span>
              </div>
            </div>

            <!-- TAOM / MAHSULOT NOMI -->
            <div class="form-group full-width">
              <label class="form-label">
                Taom / Mahsulot nomi <span class="required-star">*</span>
              </label>
              <input
                type="text"
                [(ngModel)]="formData.name"
                class="pos-input"
                placeholder="Masalan: Maxsus Palov, Tandir somsa, Pizza Margarita..."
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Sotish narxi (so'm) <span class="required-star">*</span></label>
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

            <div class="form-group">
              <label class="form-label">SKU / Kod</label>
              <input
                type="text"
                [(ngModel)]="formData.sku"
                class="pos-input"
                placeholder="Avtomatik yaratiladi"
              />
            </div>

            <!-- MAHSULOT RASMI -->
            <div class="form-group full-width image-upload-group">
              <label class="form-label">
                Mahsulot rasmi
                <span class="optional-hint">(Ixtiyoriy — JPG, JPEG, PNG, WEBP — maks 5 MB)</span>
              </label>

              <input
                type="file"
                #imageFileInput
                (change)="onFileSelected($event)"
                accept="image/jpeg,image/png,image/webp"
                style="display: none;"
              />

              <!-- Preview card if image exists -->
              <div *ngIf="imagePreview || formData.imageUrl" class="image-preview-card">
                <div class="preview-img-container">
                  <img
                    [src]="imagePreview || getImageUrl(formData.imageUrl)"
                    (error)="onImageError($event)"
                    alt="Product preview"
                    class="preview-img"
                  />
                  <div *ngIf="uploadingImage" class="preview-loading-overlay">
                    <div class="mini-spinner"></div>
                    <span>Yuklanmoqda...</span>
                  </div>
                </div>
                <div class="preview-actions">
                  <button
                    type="button"
                    class="pos-btn pos-btn--secondary pos-btn--sm"
                    (click)="imageFileInput.click()"
                    [disabled]="uploadingImage">
                    🔄 Rasmni almashtirish
                  </button>
                  <button
                    type="button"
                    class="pos-btn pos-btn--danger pos-btn--sm"
                    (click)="removeImage()"
                    [disabled]="uploadingImage">
                    🗑️ Rasmni o‘chirish
                  </button>
                </div>
              </div>

              <!-- Dropzone if no image -->
              <div
                *ngIf="!imagePreview && !formData.imageUrl"
                class="image-dropzone"
                (click)="imageFileInput.click()">
                <div class="dropzone-icon">📷</div>
                <div class="dropzone-title">Rasm yuklash uchun bosing</div>
                <div class="dropzone-hint">JPG, JPEG, PNG yoki WEBP (maks 5 MB)</div>
                <button type="button" class="pos-btn pos-btn--secondary pos-btn--sm" style="margin-top: 8px;">
                  📁 Kompyuterdan tanlash
                </button>
              </div>

              <!-- Validation Error -->
              <div *ngIf="imageError" class="image-error-alert">
                ⚠️ {{ imageError }}
              </div>
            </div>

            <div class="form-group full-width checkbox-group">
              <label class="checkbox-label">
                <input type="checkbox" [(ngModel)]="formData.available" />
                <span>Menyuda mavjud (Sotuvga ruxsat berilgan)</span>
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
              [disabled]="saving || !formData.name || !formData.categoryId || !formData.salePrice">
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
      gap: 14px;
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
      gap: 12px;
      align-items: center;
    }

    .search-box {
      display: flex;
      align-items: center;
      background: var(--bg-main);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 6px 12px;
      gap: 8px;
      min-width: 240px;

      .pos-input {
        border: none;
        background: transparent;
        padding: 0;
        font-size: 13px;
        color: var(--text-primary);
        width: 100%;
        &:focus { outline: none; box-shadow: none; }
      }
    }

    /* Filters Strips */
    .filter-strip {
      display: flex;
      align-items: center;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 2px;
    }

    .filter-title {
      font-size: 12px;
      font-weight: 700;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-right: 4px;
      white-space: nowrap;
    }

    .filter-chip {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 14px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      color: var(--text-secondary);
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition);
      white-space: nowrap;

      &:hover {
        border-color: var(--border-light);
        color: var(--text-primary);
      }

      &.active {
        background: var(--primary);
        color: white;
        border-color: var(--primary);
        .count-pill {
          background: rgba(255, 255, 255, 0.25);
          color: white;
        }
      }

      &--sm {
        padding: 6px 12px;
        font-size: 12px;
        border-radius: 20px;
      }
    }

    .count-pill {
      padding: 2px 7px;
      border-radius: 10px;
      font-size: 11px;
      background: var(--bg-tertiary);
      color: var(--text-muted);
    }

    .table-card {
      padding: 0;
      overflow: hidden;
    }

    .pos-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;

      th {
        background: var(--bg-tertiary);
        padding: 12px 16px;
        font-weight: 600;
        color: var(--text-secondary);
        border-bottom: 1px solid var(--border);
        white-space: nowrap;
      }

      td {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border);
        color: var(--text-primary);
        vertical-align: middle;
      }

      tr:hover td {
        background: rgba(var(--primary-rgb), 0.02);
      }
    }

    .product-title-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .p-icon {
      font-size: 20px;
    }

    .sub-name {
      font-size: 11px;
      color: var(--text-muted);
    }

    .sku-tag {
      font-family: monospace;
      font-size: 11px;
      background: var(--bg-tertiary);
      padding: 3px 6px;
      border-radius: 4px;
      color: var(--text-secondary);
    }

    .cat-tag {
      display: inline-block;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-primary);
      background: var(--bg-secondary);
      padding: 3px 8px;
      border-radius: 4px;
      border: 1px solid var(--border);
    }

    .kitchen-badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      font-size: 12px;
      font-weight: 600;
      color: var(--primary-light);
      background: rgba(var(--primary-rgb), 0.1);
      padding: 3px 8px;
      border-radius: 6px;
      border: 1px solid rgba(var(--primary-rgb), 0.2);
    }

    .price-val {
      font-size: 14px;
      color: var(--primary-light);
    }

    .unit-tag {
      font-size: 12px;
      color: var(--text-muted);
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 700;

      &.active {
        background: rgba(16, 185, 129, 0.12);
        color: #10b981;
      }
      &.inactive {
        background: rgba(239, 68, 68, 0.12);
        color: #ef4444;
      }
    }

    .table-actions {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
    }

    .pos-btn--sm {
      min-height: 30px;
      padding: 2px 8px;
      font-size: 12px;
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
      max-width: 520px;
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

    .full-width {
      grid-column: 1 / -1;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .required-star {
      color: #ef4444;
      font-weight: bold;
    }

    .input-hint {
      font-size: 11px;
      color: var(--text-muted);
    }

    .error-hint {
      font-size: 12px;
      color: #ef4444;
      margin-top: 2px;
    }

    .auto-kitchen-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 4px;
      flex-wrap: wrap;
    }

    .kitchen-pill {
      font-size: 12px;
      font-weight: 700;
      color: var(--primary-light);
      background: rgba(var(--primary-rgb), 0.12);
      border: 1px solid rgba(var(--primary-rgb), 0.25);
      padding: 3px 8px;
      border-radius: 6px;
    }

    .kitchen-note {
      font-size: 11px;
      color: var(--text-muted);
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      font-size: 13px;
      color: var(--text-primary);
    }

    .product-thumb-wrap {
      width: 42px;
      height: 42px;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      flex-shrink: 0;
    }

    .product-thumb {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .image-upload-group {
      margin-top: 4px;
    }

    .optional-hint {
      font-size: 11px;
      font-weight: normal;
      color: var(--text-muted);
      margin-left: 6px;
    }

    .image-preview-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 12px;
      background: var(--bg-main);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      flex-wrap: wrap;
    }

    .preview-img-container {
      position: relative;
      width: 90px;
      height: 90px;
      border-radius: 8px;
      overflow: hidden;
      border: 1px solid var(--border);
      background: #111;
      flex-shrink: 0;
    }

    .preview-img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }

    .preview-loading-overlay {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 4px;
      color: white;
      font-size: 11px;
    }

    .mini-spinner {
      width: 20px;
      height: 20px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    .preview-actions {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .image-dropzone {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 20px 16px;
      border: 2px dashed var(--border);
      border-radius: var(--radius-md);
      background: var(--bg-main);
      cursor: pointer;
      text-align: center;
      transition: all 0.2s ease;

      &:hover {
        border-color: var(--primary);
        background: rgba(var(--primary-rgb), 0.04);
      }
    }

    .dropzone-icon {
      font-size: 30px;
      margin-bottom: 6px;
    }

    .dropzone-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .dropzone-hint {
      font-size: 11px;
      color: var(--text-muted);
      margin-top: 2px;
    }

    .image-error-alert {
      margin-top: 8px;
      padding: 8px 12px;
      background: rgba(239, 68, 68, 0.12);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: 6px;
      font-size: 12px;
      color: #ef4444;
      font-weight: 600;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      padding: 16px 20px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);
    }

    .loading-state, .empty-state {
      text-align: center;
      padding: 40px 20px;
    }

    .empty-icon {
      font-size: 48px;
      margin-bottom: 12px;
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
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  categories: Category[] = [];
  kitchens: KitchenStation[] = [];

  selectedKitchenId: string | null = null;
  selectedCategoryId: string | null = null;
  searchQuery = '';

  loading = false;
  showModal = false;
  isEditing = false;
  editingId: string | null = null;
  saving = false;
  imagePreview: string | null = null;
  uploadingImage = false;
  imageError: string | null = null;

  formData: CreateProductRequest = {
    name: '',
    categoryId: '',
    sku: '',
    unit: 'dona',
    salePrice: 0,
    purchasePrice: 0,
    active: true,
    available: true,
    imageUrl: ''
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
    this.kitchenService.getKitchens().subscribe({
      next: (kRes) => {
        this.kitchens = kRes.data || [];
        this.loadCategoriesAndProducts();
      },
      error: (err) => {
        console.error('Failed to load kitchens', err);
        this.loadCategoriesAndProducts();
      }
    });
  }

  loadCategoriesAndProducts(): void {
    this.categoryService.getCategories().subscribe({
      next: (catRes) => {
        this.categories = catRes.data || [];
        this.productService.getProducts().subscribe({
          next: (prodRes) => {
            this.products = prodRes.data || [];
            if (this.pageIndex > 0 && this.pageIndex * this.pageSize >= this.totalItems) {
              this.pageIndex = Math.max(0, Math.floor((this.totalItems - 1) / this.pageSize));
            }
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

  pageIndex = 0;
  pageSize = 10;
  pageSizeOptions = [10, 25, 50, 100];

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.cdr.markForCheck();
  }

  onSearchChange(): void {
    this.pageIndex = 0;
  }

  selectKitchenFilter(kitchenId: string | null): void {
    this.selectedKitchenId = kitchenId;
    this.pageIndex = 0;
    // If current category does not belong to the selected kitchen, reset category filter
    if (kitchenId && this.selectedCategoryId) {
      const cat = this.categories.find(c => c.id === this.selectedCategoryId);
      if (!cat || cat.kitchenId !== kitchenId) {
        this.selectedCategoryId = null;
      }
    }
  }

  selectCategoryFilter(categoryId: string | null): void {
    this.selectedCategoryId = categoryId;
    this.pageIndex = 0;
  }

  get visibleCategories(): Category[] {
    if (!this.selectedKitchenId) {
      return this.categories;
    }
    return this.categories.filter(c => c.kitchenId === this.selectedKitchenId);
  }

  get allFilteredProducts(): Product[] {
    return this.products.filter(p => {
      // 1. Filter by kitchen (derived via category or product.kitchenId)
      if (this.selectedKitchenId) {
        const cat = this.categories.find(c => c.id === p.categoryId);
        const pKitchenId = cat?.kitchenId || p.kitchenId;
        if (pKitchenId !== this.selectedKitchenId) {
          return false;
        }
      }

      // 2. Filter by category
      if (this.selectedCategoryId && p.categoryId !== this.selectedCategoryId) {
        return false;
      }

      // 3. Search query
      if (this.searchQuery.trim()) {
        const q = this.searchQuery.toLowerCase();
        return p.name.toLowerCase().includes(q) || (p.sku && p.sku.toLowerCase().includes(q));
      }
      return true;
    });
  }

  get totalItems(): number {
    return this.allFilteredProducts.length;
  }

  get pagedProducts(): Product[] {
    const start = this.pageIndex * this.pageSize;
    return this.allFilteredProducts.slice(start, start + this.pageSize);
  }

  get filteredProducts(): Product[] {
    return this.pagedProducts;
  }

  getProductsCountForKitchen(kitchenId: string): number {
    return this.products.filter(p => {
      const cat = this.categories.find(c => c.id === p.categoryId);
      return (cat?.kitchenId || p.kitchenId) === kitchenId;
    }).length;
  }

  getProductsCountForCategory(categoryId: string): number {
    return this.products.filter(p => p.categoryId === categoryId).length;
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

  getKitchenNameForProduct(p: Product): string {
    const cat = this.categories.find(c => c.id === p.categoryId);
    const kId = cat?.kitchenId || p.kitchenId;
    if (!kId) return 'Biriktirilmagan';
    const k = this.kitchens.find(item => item.id === kId);
    return k ? k.name : 'Biriktirilmagan';
  }

  getKitchenEmojiForProduct(p: Product): string {
    const cat = this.categories.find(c => c.id === p.categoryId);
    const kId = cat?.kitchenId || p.kitchenId;
    const k = this.kitchens.find(item => item.id === kId);
    return this.getKitchenEmoji(k?.code);
  }

  getKitchenEmoji(code?: string): string {
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

  getProductEmoji(name: string, categoryName?: string): string {
    const n = (name + ' ' + (categoryName || '')).toLowerCase();
    if (n.includes('osh') || n.includes('palov')) return '🥘';
    if (n.includes('somsa')) return '🥟';
    if (n.includes('pitsa') || n.includes('pizza')) return '🍕';
    if (n.includes('cola') || n.includes('fanta') || n.includes('choy') || n.includes('suv') || n.includes('ichimlik')) return '🍹';
    if (n.includes('shashlik') || n.includes('kabob')) return '🥩';
    if (n.includes('manti')) return '🥟';
    if (n.includes('salat')) return '🥗';
    if (n.includes('shorva') || n.includes("sho'rva")) return '🍲';
    return '🍽️';
  }

  getSelectedCategoryKitchen(categoryId?: string): string {
    if (!categoryId) return '';
    const cat = this.categories.find(c => c.id === categoryId);
    if (!cat) return '';
    const kitchenName = cat.kitchenName || this.getKitchenName(cat.kitchenId);
    const emoji = this.getKitchenEmoji(cat.kitchenCode);
    return `${emoji} Oshxona: ${kitchenName}`;
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.imagePreview = null;
    this.imageError = null;
    this.uploadingImage = false;

    const defaultCatId = this.selectedCategoryId || (this.categories.length > 0 ? this.categories[0].id : '');

    this.formData = {
      name: '',
      categoryId: defaultCatId,
      sku: 'SKU-' + Math.floor(1000 + Math.random() * 9000),
      unit: 'dona',
      salePrice: 0,
      purchasePrice: 0,
      active: true,
      available: true,
      imageUrl: ''
    };
    this.showModal = true;
  }

  openEditModal(p: Product): void {
    this.isEditing = true;
    this.editingId = p.id;
    this.imagePreview = null;
    this.imageError = null;
    this.uploadingImage = false;

    this.formData = {
      name: p.name,
      categoryId: p.categoryId,
      sku: p.sku,
      unit: p.unit || 'dona',
      salePrice: p.salePrice || p.price || 0,
      purchasePrice: p.purchasePrice || 0,
      active: p.active !== false,
      available: p.available !== false,
      imageUrl: p.imageUrl || ''
    };
    this.showModal = true;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.imageError = null;

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    const ext = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = ['jpg', 'jpeg', 'png', 'webp'];

    if (!allowedExts.includes(ext) || (file.type && !allowedTypes.includes(file.type.toLowerCase()))) {
      this.imageError = 'Faqat JPG, JPEG, PNG yoki WEBP formatidagi rasm yuklash mumkin.';
      input.value = '';
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.imageError = 'Rasm hajmi 5 MB dan oshmasligi kerak.';
      input.value = '';
      return;
    }

    // Local instant preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.imagePreview = e.target?.result as string;
      this.cdr.markForCheck();
    };
    reader.readAsDataURL(file);

    // Upload to server
    this.uploadingImage = true;
    this.productService.uploadImage(file).subscribe({
      next: (res) => {
        this.uploadingImage = false;
        if (res.success && res.data?.imageUrl) {
          this.formData.imageUrl = res.data.imageUrl;
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.uploadingImage = false;
        this.imageError = err.error?.message || 'Rasmni yuklashda xatolik yuz berdi';
        this.imagePreview = null;
        this.cdr.markForCheck();
      }
    });
  }

  removeImage(): void {
    this.imagePreview = null;
    this.formData.imageUrl = '';
    this.imageError = null;
    this.cdr.markForCheck();
  }

  getImageUrl(url?: string | null): string {
    return getProductImageUrl(url);
  }

  onImageError(event: Event): void {
    handleImageError(event);
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveProduct(): void {
    if (!this.formData.name || !this.formData.salePrice) {
      alert('Iltimos, mahsulot nomi va sotish narxini kiriting!');
      return;
    }
    if (!this.formData.categoryId) {
      alert('Iltimos, mahsulot uchun kategoriyani tanlang! Mahsulot qat\'iy kategoriya orqali oshxonaga bog\'lanadi.');
      return;
    }

    this.formData.active = this.formData.available !== false;
    this.saving = true;
    if (this.isEditing && this.editingId) {
      this.productService.updateProduct(this.editingId, this.formData).subscribe({
        next: () => {
          this.saving = false;
          this.closeModal();
          this.loadCategoriesAndProducts();
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
          this.loadCategoriesAndProducts();
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
        this.products = this.products.filter(item => item.id !== p.id);
        if (this.pageIndex > 0 && this.pageIndex * this.pageSize >= this.totalItems) {
          this.pageIndex = Math.max(0, this.pageIndex - 1);
        }
        this.loadCategoriesAndProducts();
      },
      error: (err) => alert('O‘chirishda xatolik: ' + (err.error?.message || err.message))
    });
  }
}
