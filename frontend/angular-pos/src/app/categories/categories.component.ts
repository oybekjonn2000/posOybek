import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CategoryService, Category, CreateCategoryRequest } from '../core/services/category.service';
import { ProductService } from '../core/services/product.service';

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
          <p class="page-subtitle">Menyu bo'limlarini shakllantirish, tartibga solish va boshqarish</p>
        </div>

        <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
          <span>➕ Yangi Bo'lim Qo'shish</span>
        </button>
      </div>

      <!-- Categories Grid -->
      <div class="pos-card content-card">
        <div *ngIf="loading && categories.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Kategoriyalar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && categories.length === 0" class="empty-state">
          <div class="empty-icon">📁</div>
          <h3>Kategoriyalar mavjud emas</h3>
          <p>Restoran menyusi uchun birinchi bo'limni yarating.</p>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 12px;">
            Yangi bo'lim yaratish
          </button>
        </div>

        <div *ngIf="categories.length > 0" class="category-grid">
          <div *ngFor="let cat of categories" class="category-card">
            <div class="card-top">
              <div class="color-badge" [style.background-color]="cat.color || '#6366f1'"></div>
              <div class="cat-details">
                <h3 class="cat-name">{{ cat.name }}</h3>
                <span class="cat-count">{{ cat.productCount || 0 }} ta taom</span>
              </div>
              <div class="sort-badge">#{{ cat.sortOrder }}</div>
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
            <h2 class="modal-title">{{ isEditing ? '✏️ Bo‘limni tahrirlash' : '➕ Yangi Bo‘lim' }}</h2>
            <button class="close-btn" (click)="closeModal()">✕</button>
          </div>

          <div class="modal-body form-body">
            <div class="form-group">
              <label class="form-label">Bo'lim nomi *</label>
              <input
                type="text"
                [(ngModel)]="formData.name"
                class="pos-input"
                placeholder="Masalan: Milliy Taomlar"
                required
              />
            </div>

            <div class="form-group">
              <label class="form-label">Tartib raqami (Sort order)</label>
              <input
                type="number"
                [(ngModel)]="formData.sortOrder"
                class="pos-input"
                placeholder="1, 2, 3..."
              />
            </div>

            <div class="form-group">
              <label class="form-label">Rangi</label>
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
              [disabled]="saving || !formData.name">
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
      gap: 16px;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
        box-shadow: var(--shadow-sm);
        transform: translateY(-2px);
      }
    }

    .card-top {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .color-badge {
      width: 16px;
      height: 42px;
      border-radius: 4px;
      flex-shrink: 0;
    }

    .cat-details {
      flex: 1;
    }

    .cat-name {
      font-size: 16px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0 0 2px 0;
    }

    .cat-count {
      font-size: 12px;
      color: var(--text-muted);
    }

    .sort-badge {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      background: var(--bg-tertiary);
      padding: 3px 8px;
      border-radius: 4px;
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
      max-width: 440px;
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

    .form-label {
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 500;
    }

    .color-picker-row {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .color-input {
      width: 44px;
      height: 44px;
      padding: 0;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      background: transparent;
    }

    .preset-colors {
      display: flex;
      gap: 8px;
    }

    .preset-color-dot {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      cursor: pointer;
      transition: transform var(--transition);

      &:hover {
        transform: scale(1.15);
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
  `]
})
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  loading = false;

  showModal = false;
  isEditing = false;
  editingId: string | null = null;
  saving = false;

  presetColors = ['#6366f1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899'];

  formData: CreateCategoryRequest = {
    name: '',
    sortOrder: 1,
    color: '#6366f1'
  };

  constructor(
    private categoryService: CategoryService,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
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

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.formData = {
      name: '',
      sortOrder: this.categories.length + 1,
      color: this.presetColors[this.categories.length % this.presetColors.length]
    };
    this.showModal = true;
  }

  openEditModal(cat: Category): void {
    this.isEditing = true;
    this.editingId = cat.id;
    this.formData = {
      name: cat.name,
      sortOrder: cat.sortOrder,
      color: cat.color || '#6366f1'
    };
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
  }

  saveCategory(): void {
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
    if (!confirm(`"${cat.name}" bo'limini o'chirmoqchimisiz?`)) {
      return;
    }
    this.categoryService.deleteCategory(cat.id).subscribe({
      next: () => {
        this.loadCategories();
      },
      error: (err) => alert('Xatolik: ' + (err.error?.message || err.message))
    });
  }
}
