import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoryService, Category, CreateCategoryRequest } from '../core/services/category.service';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="categories-page fade-in">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">🏷️ Bo'limlar & Kategoriyalar</h1>
          <p class="page-subtitle">Ierarxiya: <strong>Oshxona ➔ Kategoriya ➔ Mahsulot</strong>. Har bir kategoriya qat'iy bitta oshxonaga tegishli.</p>
        </div>

        <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
          <span>➕ Yangi Kategoriya Qo'shish</span>
        </button>
      </div>

      <!-- Kitchen Filter Tabs -->
      <div class="kitchen-filter-tabs">
        <button
          class="kitchen-tab-btn"
          [class.active]="selectedKitchenFilter === null"
          (click)="selectedKitchenFilter = null">
          <span>🍽️ Barcha Oshxonalar</span>
          <span class="count-badge">{{ categories.length }}</span>
        </button>
        <button
          *ngFor="let k of kitchens"
          class="kitchen-tab-btn"
          [class.active]="selectedKitchenFilter === k.id"
          (click)="selectedKitchenFilter = k.id">
          <span>{{ getKitchenEmoji(k.code) }} {{ k.name }}</span>
          <span class="count-badge">{{ getCategoriesCountForKitchen(k.id) }}</span>
        </button>
      </div>

      <!-- Categories Grid -->
      <div class="pos-card content-card">
        <div *ngIf="loading && categories.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Kategoriyalar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && filteredCategories.length === 0" class="empty-state">
          <div class="empty-icon">📁</div>
          <h3>Kategoriyalar mavjud emas</h3>
          <p>Ushbu oshxona uchun hali kategoriya yaratilmagan.</p>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 12px;">
            Yangi kategoriya yaratish
          </button>
        </div>

        <div *ngIf="filteredCategories.length > 0" class="category-grid">
          <div *ngFor="let cat of filteredCategories" class="category-card">
            <div class="card-top">
              <div class="color-badge" [style.background-color]="cat.color || '#6366f1'"></div>
              <div class="cat-details">
                <h3 class="cat-name">{{ cat.name }}</h3>
                <div class="cat-station-badge">
                  <span class="station-icon">{{ getKitchenEmoji(cat.kitchenCode) }}</span>
                  <span class="station-name">{{ cat.kitchenName || getKitchenName(cat.kitchenId) }}</span>
                </div>
              </div>
              <div class="sort-badge">#{{ cat.sortOrder }}</div>
            </div>

            <div class="card-meta">
              <span class="meta-item">📦 {{ cat.productCount || 0 }} ta mahsulot</span>
              <span class="status-indicator" [class.active]="cat.active !== false">
                {{ cat.active !== false ? '● Faol' : '○ Nofaol' }}
              </span>
            </div>

            <div class="card-actions">
              <button class="pos-btn pos-btn--secondary pos-btn--sm" (click)="openEditModal(cat)">
                ✏️ Tahrirlash
              </button>
              <button class="pos-btn pos-btn--danger pos-btn--sm" (click)="deleteCategory(cat)">
                🗑️ O'chirish
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL: ADD / EDIT CATEGORY                                     -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">{{ isEditing ? '✏️ Kategoriyani tahrirlash' : '➕ Yangi Kategoriya' }}</h2>
            <button class="close-btn" (click)="closeModal()">✕</button>
          </div>

          <div class="modal-body form-body">
            <!-- Kitchen Selection (MANDATORY) -->
            <div class="form-group">
              <label class="form-label">
                Oshxona (KDS Stansiyasi) <span class="required-star">*</span>
              </label>
              <select
                [(ngModel)]="formData.kitchenId"
                class="pos-input"
                [class.input-error]="kitchenError"
                (change)="onKitchenSelected()"
                required>
                <option [ngValue]="''" disabled selected>Oshxonani tanlang ▼</option>
                <option *ngFor="let k of kitchens" [value]="k.id">
                  {{ getKitchenEmoji(k.code) }} {{ k.name }} ({{ k.code }})
                </option>
              </select>
              <span class="error-hint" *ngIf="kitchenError">
                ⚠️ Oshxona tanlanishi kerak!
              </span>
            </div>

            <!-- Category Name -->
            <div class="form-group">
              <label class="form-label">
                Kategoriya nomi <span class="required-star">*</span>
              </label>
              <input
                type="text"
                [(ngModel)]="formData.name"
                class="pos-input"
                placeholder="Masalan: Milliy taomlar, Sovuq ichimliklar..."
                required
              />
            </div>

            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">Tartib raqami</label>
                <input
                  type="number"
                  [(ngModel)]="formData.sortOrder"
                  class="pos-input"
                  placeholder="1, 2, 3..."
                />
              </div>

              <div class="form-group checkbox-flex">
                <label class="checkbox-label">
                  <input type="checkbox" [(ngModel)]="formData.active" />
                  <span>Faol holatda</span>
                </label>
              </div>
            </div>

            <!-- Color Picker -->
            <div class="form-group">
              <label class="form-label">Rangi / Yorlig'i</label>
              <div class="color-picker-row">
                <input
                  type="color"
                  [(ngModel)]="formData.color"
                  class="color-input"
                />
                <div class="preset-colors">
                  <span
                    *ngFor="let col of presetColors"
                    class="preset-color-dot"
                    [style.background-color]="col"
                    [class.selected]="formData.color === col"
                    (click)="formData.color = col"></span>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModal()" [disabled]="saving">
              Bekor qilish
            </button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveCategory()"
              [disabled]="saving || !formData.name || !formData.kitchenId">
              <span>{{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .categories-page {
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

    .kitchen-filter-tabs {
      display: flex;
      gap: 8px;
      overflow-x: auto;
      padding-bottom: 4px;
    }

    .kitchen-tab-btn {
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
        .count-badge {
          background: rgba(255, 255, 255, 0.25);
          color: white;
        }
      }
    }

    .count-badge {
      padding: 2px 7px;
      border-radius: 10px;
      font-size: 11px;
      background: var(--bg-tertiary);
      color: var(--text-muted);
    }

    .content-card {
      padding: 24px;
    }

    .category-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }

    .category-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
        box-shadow: var(--shadow-sm);
        transform: translateY(-2px);
      }
    }

    .card-top {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .color-badge {
      width: 14px;
      height: 48px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    .cat-details {
      flex: 1;
      min-width: 0;
    }

    .cat-name {
      font-size: 16px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0 0 4px 0;
      word-break: break-word;
    }

    .cat-station-badge {
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

    .sort-badge {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      background: var(--bg-tertiary);
      padding: 3px 8px;
      border-radius: 4px;
    }

    .card-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 12px;
      color: var(--text-muted);
      padding-top: 4px;
    }

    .status-indicator {
      font-weight: 600;
      &.active { color: #10b981; }
      &:not(.active) { color: #ef4444; }
    }

    .card-actions {
      display: flex;
      gap: 8px;
      justify-content: flex-end;
      border-top: 1px solid var(--divider);
      padding-top: 12px;
    }

    .pos-btn--sm {
      min-height: 32px;
      padding: 4px 10px;
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
      max-width: 480px;
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

    .form-body {
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .form-row {
      display: flex;
      gap: 12px;
      align-items: flex-end;
    }

    .flex-1 { flex: 1; }

    .checkbox-flex {
      padding-bottom: 8px;
    }

    .checkbox-label {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      font-size: 13px;
      color: var(--text-primary);
    }

    .required-star {
      color: #ef4444;
      font-weight: bold;
    }

    .input-error {
      border-color: #ef4444 !important;
      background: rgba(239, 68, 68, 0.05);
    }

    .error-hint {
      font-size: 12px;
      color: #ef4444;
      margin-top: 2px;
    }

    .color-picker-row {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .color-input {
      width: 42px;
      height: 36px;
      border: 1px solid var(--border);
      border-radius: 4px;
      background: transparent;
      cursor: pointer;
      padding: 2px;
    }

    .preset-colors {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .preset-color-dot {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      cursor: pointer;
      transition: transform 0.15s;
      border: 2px solid transparent;

      &:hover { transform: scale(1.15); }
      &.selected { border-color: white; box-shadow: 0 0 0 2px var(--primary); }
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
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  kitchens: KitchenStation[] = [];
  selectedKitchenFilter: string | null = null;
  loading = false;
  showModal = false;
  isEditing = false;
  editingId: string | null = null;
  saving = false;
  kitchenError = false;

  presetColors = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

  formData: CreateCategoryRequest = {
    name: '',
    kitchenId: '',
    sortOrder: 1,
    color: '#6366f1',
    active: true
  };

  constructor(
    private categoryService: CategoryService,
    private kitchenService: KitchenService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.kitchenService.getKitchens().subscribe({
      next: (kRes) => {
        this.kitchens = kRes.data || [];
        this.loadCategories();
      },
      error: (err) => {
        console.error('Failed to load kitchens', err);
        this.loadCategories();
      }
    });
  }

  loadCategories(): void {
    this.categoryService.getCategories().subscribe({
      next: (res) => {
        this.categories = res.data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load categories', err);
        this.loading = false;
      }
    });
  }

  get filteredCategories(): Category[] {
    if (!this.selectedKitchenFilter) {
      return this.categories;
    }
    return this.categories.filter(c => c.kitchenId === this.selectedKitchenFilter);
  }

  getCategoriesCountForKitchen(kitchenId: string): number {
    return this.categories.filter(c => c.kitchenId === kitchenId).length;
  }

  getKitchenName(kitchenId?: string): string {
    if (!kitchenId) return 'Oshxona biriktirilmagan';
    const k = this.kitchens.find(item => item.id === kitchenId);
    return k ? k.name : 'Oshxona biriktirilmagan';
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

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.kitchenError = false;

    // Default to currently selected filter kitchen if one is selected
    const defaultKitchenId = this.selectedKitchenFilter || (this.kitchens.length > 0 ? this.kitchens[0].id : '');

    this.formData = {
      name: '',
      kitchenId: defaultKitchenId,
      sortOrder: this.categories.length + 1,
      color: this.presetColors[this.categories.length % this.presetColors.length],
      active: true
    };
    this.showModal = true;
  }

  openEditModal(cat: Category): void {
    this.isEditing = true;
    this.editingId = cat.id;
    this.kitchenError = false;
    this.formData = {
      name: cat.name,
      kitchenId: cat.kitchenId || '',
      sortOrder: cat.sortOrder,
      color: cat.color || '#6366f1',
      active: cat.active !== false
    };
    this.showModal = true;
  }

  onKitchenSelected(): void {
    if (this.formData.kitchenId) {
      this.kitchenError = false;
    }
  }

  closeModal(): void {
    this.showModal = false;
    this.kitchenError = false;
  }

  saveCategory(): void {
    if (!this.formData.kitchenId) {
      this.kitchenError = true;
      alert('Oshxona tanlanishi kerak!');
      return;
    }

    if (!this.formData.name.trim()) {
      alert('Iltimos, kategoriya nomini kiriting!');
      return;
    }

    this.saving = true;
    if (this.isEditing && this.editingId) {
      this.categoryService.updateCategory(this.editingId, this.formData).subscribe({
        next: () => {
          this.saving = false;
          this.closeModal();
          this.loadCategories();
        },
        error: (err) => {
          this.saving = false;
          alert('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    } else {
      this.categoryService.createCategory(this.formData).subscribe({
        next: () => {
          this.saving = false;
          this.closeModal();
          this.loadCategories();
        },
        error: (err) => {
          this.saving = false;
          alert('Xatolik: ' + (err.error?.message || err.message));
        }
      });
    }
  }

  deleteCategory(cat: Category): void {
    if (!confirm(`"${cat.name}" kategoriyasini o'chirmoqchimisiz?`)) {
      return;
    }
    this.categoryService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.loadCategories();
      },
      error: (err) => {
        alert('O‘chirishda xatolik:\n' + (err.error?.message || err.message));
      }
    });
  }
}
