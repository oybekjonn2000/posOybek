import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { KitchenService, KitchenStation, CreateKitchenRequest, UpdateKitchenRequest, AssignedEmployee, AssignedCategory } from '../../core/services/kitchen.service';
import { UserService, Employee } from '../../core/services/user.service';
import { PrinterService, Printer } from '../../core/services/printer.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-kitchen-management',
  standalone: true,
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  template: `
    <div class="kitchens-page fade-in">
      <!-- Page Header -->
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon-wrap">🥘</div>
          <div>
            <h1 class="page-title">Oshxonalar Boshqaruvi</h1>
            <p class="page-subtitle">Oshxonalar (stansiyalar), buyurtma yo'nalishi (routing) va xodimlar biriktiruvi</p>
          </div>
        </div>

        <div class="header-actions">
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
            <span class="btn-icon">➕</span>
            <span>Yangi Oshxona Qo'shish</span>
          </button>
        </div>
      </div>

      <!-- Quick Metrics Summary -->
      <div class="metrics-grid">
        <div class="metric-card">
          <div class="metric-icon metric-icon--purple">🥘</div>
          <div class="metric-content">
            <span class="metric-label">Jami Oshxonalar</span>
            <span class="metric-value">{{ kitchens.length }}</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon metric-icon--green">🟢</div>
          <div class="metric-content">
            <span class="metric-label">Faol (ACTIVE)</span>
            <span class="metric-value">{{ activeCount }}</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon metric-icon--amber">⚪</div>
          <div class="metric-content">
            <span class="metric-label">Nofaol (INACTIVE)</span>
            <span class="metric-value">{{ inactiveCount }}</span>
          </div>
        </div>
        <div class="metric-card">
          <div class="metric-icon metric-icon--blue">👨‍🍳</div>
          <div class="metric-content">
            <span class="metric-label">Biriktirilgan Xodimlar</span>
            <span class="metric-value">{{ totalAssignedStaff }}</span>
          </div>
        </div>
      </div>

      <!-- Search, Filters and Toolbar -->
      <div class="pos-card toolbar-card">
        <div class="toolbar-left">
          <div class="search-input-wrap">
            <span class="search-icon">🔍</span>
            <input
              type="text"
              class="pos-input search-input"
              placeholder="Oshxona nomi yoki tavsifi bo'yicha qidirish..."
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearchChange()"
            />
            <button *ngIf="searchQuery" class="clear-search-btn" (click)="searchQuery = ''; onSearchChange()">✕</button>
          </div>

          <div class="filter-tabs">
            <button
              class="filter-tab"
              [class.active]="statusFilter === 'ALL'"
              (click)="setStatusFilter('ALL')">
              Barchasi ({{ kitchens.length }})
            </button>
            <button
              class="filter-tab"
              [class.active]="statusFilter === 'ACTIVE'"
              (click)="setStatusFilter('ACTIVE')">
              Faol ({{ activeCount }})
            </button>
            <button
              class="filter-tab"
              [class.active]="statusFilter === 'INACTIVE'"
              (click)="setStatusFilter('INACTIVE')">
              Nofaol ({{ inactiveCount }})
            </button>
          </div>
        </div>

        <div class="toolbar-right">
          <button class="pos-btn pos-btn--secondary pos-btn--sm" (click)="loadData()" [disabled]="loading">
            <span>🔄 Yangilash</span>
          </button>
        </div>
      </div>

      <!-- Kitchens Table Card -->
      <div class="pos-card table-card">
        <div *ngIf="loading && kitchens.length === 0" class="state-container">
          <div class="spinner"></div>
          <p>Oshxonalar yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && filteredKitchens.length === 0" class="state-container empty-state">
          <div class="empty-icon">🍳</div>
          <h3>Oshxonalar topilmadi</h3>
          <p *ngIf="searchQuery || statusFilter !== 'ALL'">Qidiruv yoki filtr mezonlariga mos keladigan oshxona topilmadi.</p>
          <p *ngIf="!searchQuery && statusFilter === 'ALL'">Tizimda hozircha oshxona mavjud emas. Yangi oshxona qo'shishingiz mumkin.</p>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 14px;">
            ➕ Yangi oshxona qo'shish
          </button>
        </div>

        <div class="table-responsive" *ngIf="filteredKitchens.length > 0">
          <table class="pos-table">
            <thead>
              <tr>
                <th style="width: 70px;">ID</th>
                <th>Oshxona Nomi</th>
                <th>Tavsif</th>
                <th style="width: 140px; text-align: center;">Xodimlar</th>
                <th style="width: 150px; text-align: center;">Kategoriyalar</th>
                <th style="width: 140px; text-align: center;">Status</th>
                <th style="width: 140px;">Yaratilgan Sana</th>
                <th style="width: 220px; text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let k of pagedKitchens" [class.row-inactive]="!k.active">
                <td>
                  <span class="id-badge" [title]="k.id">{{ k.id.substring(0, 6) }}</span>
                </td>
                <td>
                  <div class="kitchen-name-cell">
                    <span class="color-dot" [style.background-color]="k.color || '#6366F1'"></span>
                    <div class="kitchen-titles">
                      <strong class="kitchen-main-name">{{ k.name }}</strong>
                      <span class="kitchen-code-badge">{{ k.code }}</span>
                    </div>
                  </div>
                </td>
                <td>
                  <span class="desc-text" [title]="k.description || ''">
                    {{ k.description || '—' }}
                  </span>
                </td>
                <td style="text-align: center;">
                  <button
                    class="badge-btn badge-btn--staff"
                    (click)="openStaffAssignmentModal(k)"
                    title="Biriktirilgan xodimlarni ko'rish va o'zgartirish">
                    <span class="badge-icon">👨‍🍳</span>
                    <span class="badge-count">{{ k.assignedEmployeesCount || 0 }} ta xodim</span>
                  </button>
                </td>
                <td style="text-align: center;">
                  <button
                    class="badge-btn badge-btn--cats"
                    (click)="openCategoryViewModal(k)"
                    title="Ushbu oshxonaga biriktirilgan kategoriyalarni ko'rish">
                    <span class="badge-icon">🏷️</span>
                    <span class="badge-count">{{ k.assignedCategoriesCount || 0 }} ta kategoriya</span>
                  </button>
                </td>
                <td style="text-align: center;">
                  <span
                    class="status-chip"
                    [class.status-chip--active]="k.active"
                    [class.status-chip--inactive]="!k.active">
                    <span class="status-dot"></span>
                    {{ k.active ? 'ACTIVE' : 'INACTIVE' }}
                  </span>
                </td>
                <td>
                  <span class="date-text">
                    {{ formatDate(k.createdAt) }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <div class="action-buttons">
                    <button
                      class="action-btn action-btn--edit"
                      (click)="openEditModal(k)"
                      title="Tahrirlash">
                      ✏️ Tahrirlash
                    </button>
                    <button
                      class="action-btn"
                      [class.action-btn--deactivate]="k.active"
                      [class.action-btn--activate]="!k.active"
                      (click)="toggleStatus(k)"
                      [title]="k.active ? 'Nofaol qilish' : 'Faollashtirish'">
                      {{ k.active ? '⏸ Deaktiv' : '▶ Aktiv' }}
                    </button>
                    <button
                      class="action-btn action-btn--delete"
                      (click)="confirmDelete(k)"
                      title="O'chirish">
                      🗑️
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Material Paginator -->
        <div class="paginator-wrap" *ngIf="filteredKitchens.length > 0">
          <mat-paginator
            [length]="filteredKitchens.length"
            [pageSize]="pageSize"
            [pageIndex]="pageIndex"
            [pageSizeOptions]="pageSizeOptions"
            (page)="onPageChange($event)"
            showFirstLastButtons
            aria-label="Oshxonalar sahifalari">
          </mat-paginator>
        </div>
      </div>

      <!-- CREATE / EDIT MODAL -->
      <div class="pos-modal-backdrop" *ngIf="showFormModal" (click)="closeFormModal()">
        <div class="pos-modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div class="modal-header-title">
              <span class="modal-icon">{{ isEditing ? '✏️' : '➕' }}</span>
              <h2>{{ isEditing ? 'Oshxonani Tahrirlash' : 'Yangi Oshxona Qo‘shish' }}</h2>
            </div>
            <button class="close-modal-btn" (click)="closeFormModal()">✕</button>
          </div>

          <div class="modal-body">
            <!-- Kitchen Name -->
            <div class="form-group">
              <label class="form-label">
                Oshxona Nomi <span class="required-star">*</span>
              </label>
              <input
                type="text"
                class="pos-input"
                [class.input-error]="nameError"
                [(ngModel)]="formData.name"
                (ngModelChange)="onNameChange()"
                placeholder="Masalan: Pitsaxona, Somsapaz, Bar, Milliy oshxona..."
                maxlength="100"
                required
              />
              <div class="field-meta">
                <span class="error-hint" *ngIf="nameError">{{ nameError }}</span>
                <span class="char-count" *ngIf="!nameError">{{ (formData.name || '').length }}/100</span>
              </div>
            </div>

            <!-- Kitchen Code & Preparation Time -->
            <div class="form-row">
              <div class="form-group flex-1">
                <label class="form-label">Qisqa Kod</label>
                <input
                  type="text"
                  class="pos-input"
                  [(ngModel)]="formData.code"
                  placeholder="KOD (avtomatik yaratiladi)"
                  maxlength="20"
                />
              </div>

              <div class="form-group flex-1">
                <label class="form-label">O'rtacha Vaqt (minut)</label>
                <input
                  type="number"
                  class="pos-input"
                  [(ngModel)]="formData.preparationTimeMinutes"
                  placeholder="15"
                  min="1"
                  max="180"
                />
              </div>
            </div>

            <!-- Description -->
            <div class="form-group">
              <label class="form-label">Tavsif</label>
              <textarea
                class="pos-input pos-textarea"
                rows="3"
                [(ngModel)]="formData.description"
                placeholder="Pizza, burger va tezpishar mahsulotlar tayyorlanadigan bo‘lim...">
              </textarea>
            </div>

            <!-- Color Picker -->
            <div class="form-group">
              <label class="form-label">Yorliq Rangi</label>
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

            <!-- Printer Selection (Optional) -->
            <div class="form-group" *ngIf="printers.length > 0">
              <label class="form-label">Biriktirilgan Printer</label>
              <select class="pos-input" [(ngModel)]="formData.printerId">
                <option [ngValue]="null">Printer tanlanmagan (ixtiyoriy)</option>
                <option *ngFor="let p of printers" [value]="p.id">
                  🖨️ {{ p.name }} ({{ p.connectionType }})
                </option>
              </select>
            </div>

            <!-- Options: Active, Auto-Print, Sound -->
            <div class="options-group">
              <label class="checkbox-card" [class.checked]="formData.active">
                <input type="checkbox" [(ngModel)]="formData.active" />
                <div class="checkbox-text">
                  <strong>Faol holatda (ACTIVE)</strong>
                  <span>Nofaol qilinsa, yangi kategoriyalar va zakazlar qabul qilinmaydi</span>
                </div>
              </label>

              <label class="checkbox-card" [class.checked]="formData.autoPrint">
                <input type="checkbox" [(ngModel)]="formData.autoPrint" />
                <div class="checkbox-text">
                  <strong>Avtomatik Chop Etish (Auto-print)</strong>
                  <span>Buyurtma tushganda oshxona chekini avtomatik chop qilish</span>
                </div>
              </label>

              <label class="checkbox-card" [class.checked]="formData.soundNotification">
                <input type="checkbox" [(ngModel)]="formData.soundNotification" />
                <div class="checkbox-text">
                  <strong>Tovushli Bildirishnoma (Sound Alert)</strong>
                  <span>KDS ekranida yangi buyurtma kelganda qo'ng'iroq chalinadi</span>
                </div>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeFormModal()" [disabled]="saving">
              Bekor Qilish
            </button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveForm()"
              [disabled]="saving || !formData.name || !formData.name.trim()">
              <span>{{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- STAFF ASSIGNMENT MODAL -->
      <div class="pos-modal-backdrop" *ngIf="showStaffModal" (click)="closeStaffModal()">
        <div class="pos-modal pos-modal--wide" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div class="modal-header-title">
              <span class="modal-icon">👨‍🍳</span>
              <div>
                <h2>Xodimlarni Oshxonaga Biriktirish</h2>
                <p class="modal-subtitle">Oshxona: <strong>{{ selectedKitchenForStaff?.name }}</strong></p>
              </div>
            </div>
            <button class="close-modal-btn" (click)="closeStaffModal()">✕</button>
          </div>

          <div class="modal-body">
            <p class="section-notice">
              Ushbu oshxonada buyurtmalarni qabul qiladigan va tayyorlaydigan xodimlarni tanlang.
              Bir xodim bir nechta oshxonaga biriktirilishi mumkin (Multi-Kitchen).
            </p>

            <div *ngIf="loadingStaffList" class="state-container">
              <div class="spinner"></div>
              <p>Xodimlar ro'yxati yuklanmoqda...</p>
            </div>

            <div *ngIf="!loadingStaffList" class="staff-selection-grid">
              <label
                *ngFor="let emp of allStaff"
                class="staff-check-card"
                [class.selected]="selectedEmployeeIds.has(emp.id)">
                <input
                  type="checkbox"
                  [checked]="selectedEmployeeIds.has(emp.id)"
                  (change)="toggleStaffSelection(emp.id)"
                />
                <div class="staff-info">
                  <div class="staff-name-row">
                    <strong class="staff-name">{{ emp.fullName || (emp.firstName + ' ' + (emp.lastName || '')) }}</strong>
                    <span class="role-badge" [class.role-kitchen]="emp.role === 'KITCHEN'">{{ emp.role }}</span>
                  </div>
                  <span class="staff-username">@{{ emp.username }}</span>
                </div>
              </label>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeStaffModal()" [disabled]="savingStaff">
              Bekor Qilish
            </button>
            <button class="pos-btn pos-btn--primary" (click)="saveStaffAssignments()" [disabled]="savingStaff">
              <span>{{ savingStaff ? 'Saqlanmoqda...' : 'Biriktirishni Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- CATEGORIES VIEW MODAL -->
      <div class="pos-modal-backdrop" *ngIf="showCategoriesModal" (click)="closeCategoriesModal()">
        <div class="pos-modal" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <div class="modal-header-title">
              <span class="modal-icon">🏷️</span>
              <div>
                <h2>Biriktirilgan Kategoriyalar</h2>
                <p class="modal-subtitle">Oshxona: <strong>{{ selectedKitchenForCats?.name }}</strong></p>
              </div>
            </div>
            <button class="close-modal-btn" (click)="closeCategoriesModal()">✕</button>
          </div>

          <div class="modal-body">
            <div *ngIf="loadingCatsList" class="state-container">
              <div class="spinner"></div>
              <p>Kategoriyalar yuklanmoqda...</p>
            </div>

            <div *ngIf="!loadingCatsList && assignedCategories.length === 0" class="state-container empty-state">
              <div class="empty-icon">📁</div>
              <p>Hozircha ushbu oshxonaga biriktirilgan kategoriya yo'q.</p>
              <p class="modal-hint">Kategoriyalar bo'limidan ushbu oshxonani tanlashingiz mumkin.</p>
            </div>

            <div *ngIf="!loadingCatsList && assignedCategories.length > 0" class="assigned-cats-list">
              <div *ngFor="let cat of assignedCategories" class="cat-list-item">
                <span class="cat-bullet">●</span>
                <span class="cat-title">{{ cat.name }}</span>
                <span class="cat-status" [class.active]="cat.active">{{ cat.active ? 'Faol' : 'Nofaol' }}</span>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeCategoriesModal()">
              Yopish
            </button>
          </div>
        </div>
      </div>

      <!-- SAFE DELETE / DEACTIVATE CONFIRMATION MODAL -->
      <div class="pos-modal-backdrop" *ngIf="showDeleteModal" (click)="closeDeleteModal()">
        <div class="pos-modal" (click)="$event.stopPropagation()">
          <div class="modal-header modal-header--danger">
            <div class="modal-header-title">
              <span class="modal-icon">⚠️</span>
              <h2>Oshxonani O'chirish</h2>
            </div>
            <button class="close-modal-btn" (click)="closeDeleteModal()">✕</button>
          </div>

          <div class="modal-body">
            <p class="delete-warning-text">
              Haqiqatan ham <strong>"{{ targetKitchenForDelete?.name }}"</strong> oshxonasini o'chirmoqchimisiz?
            </p>

            <div class="delete-info-box" *ngIf="hasLinkedData(targetKitchenForDelete)">
              <div class="info-icon">🛡️</div>
              <div class="info-content">
                <strong>Xavfsiz O'chirish (Safe Delete Himoyasi):</strong>
                <p>
                  Ushbu oshxonaga boshqa ma'lumotlar (kategoriyalar yoki buyurtmalar tarixi) bog'langan.
                  Tarixiy hisobotlar va bog'lanishlar buzilmasligi uchun uni fizik o'chirish o'rniga
                  <strong>INACTIVE (Faolsizlantirish)</strong> qilish tavsiya etiladi.
                </p>
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeDeleteModal()" [disabled]="deleting">
              Bekor Qilish
            </button>

            <!-- If has linked data, offer deactivation as primary recommended action -->
            <button
              *ngIf="hasLinkedData(targetKitchenForDelete)"
              class="pos-btn pos-btn--primary"
              (click)="deactivateInsteadOfDelete()"
              [disabled]="deleting">
              <span>{{ deleting ? 'Bajarilmoqda...' : 'Nofaol (INACTIVE) Qilish' }}</span>
            </button>

            <!-- Attempt soft delete -->
            <button
              class="pos-btn pos-btn--danger"
              (click)="executeDelete()"
              [disabled]="deleting">
              <span>{{ deleting ? 'O‘chirilmoqda...' : 'O‘chirish' }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .kitchens-page {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    /* Header */
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 16px;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 14px;
    }

    .header-icon-wrap {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: linear-gradient(135deg, rgba(99, 102, 241, 0.15), rgba(168, 85, 247, 0.15));
      border: 1px solid rgba(99, 102, 241, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .page-title {
      font-size: 24px;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }

    .page-subtitle {
      font-size: 13px;
      color: var(--text-muted);
      margin: 4px 0 0 0;
    }

    /* Metrics */
    .metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 14px;
    }

    .metric-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 14px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    }

    .metric-icon {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      flex-shrink: 0;

      &--purple { background: rgba(99, 102, 241, 0.12); }
      &--green { background: rgba(16, 185, 129, 0.12); }
      &--amber { background: rgba(245, 158, 11, 0.12); }
      &--blue { background: rgba(59, 130, 246, 0.12); }
    }

    .metric-content {
      display: flex;
      flex-direction: column;
    }

    .metric-label {
      font-size: 12px;
      color: var(--text-muted);
      font-weight: 500;
    }

    .metric-value {
      font-size: 20px;
      font-weight: 700;
      color: var(--text-primary);
      line-height: 1.2;
    }

    /* Toolbar */
    .toolbar-card {
      padding: 14px 18px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 14px;
    }

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 14px;
      flex-wrap: wrap;
      flex: 1;
    }

    .search-input-wrap {
      position: relative;
      min-width: 280px;
      flex: 1;
      max-width: 380px;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      font-size: 14px;
      color: var(--text-muted);
      pointer-events: none;
    }

    .search-input {
      padding-left: 36px;
      padding-right: 32px;
      width: 100%;
    }

    .clear-search-btn {
      position: absolute;
      right: 10px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      color: var(--text-muted);
      cursor: pointer;
      font-size: 12px;
    }

    .filter-tabs {
      display: flex;
      gap: 6px;
      background: var(--bg-tertiary);
      padding: 3px;
      border-radius: var(--radius-md);
      border: 1px solid var(--border);
    }

    .filter-tab {
      padding: 6px 12px;
      border-radius: var(--radius-sm);
      border: none;
      background: transparent;
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        color: var(--text-primary);
      }

      &.active {
        background: var(--bg-card);
        color: var(--primary);
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
      }
    }

    /* Table */
    .table-card {
      padding: 0;
      overflow: hidden;
    }

    .table-responsive {
      overflow-x: auto;
      width: 100%;
    }

    .pos-table {
      width: 100%;
      border-collapse: collapse;
      text-align: left;
      font-size: 13px;

      th {
        background: var(--bg-tertiary);
        color: var(--text-muted);
        font-weight: 600;
        padding: 12px 16px;
        border-bottom: 1px solid var(--border);
        white-space: nowrap;
      }

      td {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border);
        color: var(--text-secondary);
        vertical-align: middle;
      }

      tbody tr:hover {
        background: var(--bg-hover);
      }

      tbody tr.row-inactive {
        opacity: 0.72;
        background: rgba(0, 0, 0, 0.02);
      }
    }

    .id-badge {
      font-family: monospace;
      font-size: 11px;
      padding: 2px 6px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: 4px;
      color: var(--text-muted);
    }

    .kitchen-name-cell {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .color-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .kitchen-titles {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .kitchen-main-name {
      font-size: 14px;
      color: var(--text-primary);
    }

    .kitchen-code-badge {
      font-size: 11px;
      padding: 1px 6px;
      background: rgba(99, 102, 241, 0.1);
      color: var(--primary);
      border-radius: 4px;
      font-weight: 600;
      text-transform: uppercase;
    }

    .desc-text {
      color: var(--text-muted);
      font-size: 12px;
      max-width: 250px;
      display: inline-block;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .badge-btn {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 12px;
      border: 1px solid var(--border);
      background: var(--bg-card);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.06);
      }

      &--staff {
        color: #2563eb;
        border-color: rgba(37, 99, 235, 0.25);
        background: rgba(37, 99, 235, 0.06);
      }

      &--cats {
        color: #7c3aed;
        border-color: rgba(124, 58, 237, 0.25);
        background: rgba(124, 58, 237, 0.06);
      }
    }

    .status-chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 3px 10px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.5px;

      .status-dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
      }

      &--active {
        background: rgba(16, 185, 129, 0.12);
        color: #10b981;
        .status-dot { background: #10b981; }
      }

      &--inactive {
        background: rgba(100, 116, 139, 0.15);
        color: #64748b;
        .status-dot { background: #64748b; }
      }
    }

    .date-text {
      font-size: 12px;
      color: var(--text-muted);
    }

    .action-buttons {
      display: flex;
      gap: 6px;
      justify-content: flex-end;
    }

    .action-btn {
      padding: 5px 10px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border);
      background: var(--bg-card);
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        background: var(--bg-hover);
        color: var(--text-primary);
      }

      &--edit:hover {
        border-color: var(--primary);
        color: var(--primary);
      }

      &--deactivate {
        color: #f59e0b;
        &:hover { border-color: #f59e0b; background: rgba(245, 158, 11, 0.1); }
      }

      &--activate {
        color: #10b981;
        &:hover { border-color: #10b981; background: rgba(16, 185, 129, 0.1); }
      }

      &--delete {
        color: #ef4444;
        &:hover { border-color: #ef4444; background: rgba(239, 68, 68, 0.1); }
      }
    }

    .paginator-wrap {
      border-top: 1px solid var(--border);
      background: var(--bg-card);
    }

    /* Modal Styles */
    .pos-modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.55);
      backdrop-filter: blur(3px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1100;
      padding: 16px;
    }

    .pos-modal {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-lg);
      width: 100%;
      max-width: 540px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
      display: flex;
      flex-direction: column;

      &--wide {
        max-width: 650px;
      }
    }

    .modal-header {
      padding: 16px 20px;
      border-bottom: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;

      &--danger {
        background: rgba(239, 68, 68, 0.08);
      }
    }

    .modal-header-title {
      display: flex;
      align-items: center;
      gap: 12px;

      h2 {
        font-size: 18px;
        font-weight: 700;
        margin: 0;
        color: var(--text-primary);
      }
    }

    .modal-subtitle {
      font-size: 12px;
      color: var(--text-muted);
      margin: 2px 0 0 0;
    }

    .modal-icon {
      font-size: 20px;
    }

    .close-modal-btn {
      background: none;
      border: none;
      font-size: 18px;
      color: var(--text-muted);
      cursor: pointer;
      &:hover { color: var(--text-primary); }
    }

    .modal-body {
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .form-row {
      display: flex;
      gap: 12px;
    }

    .flex-1 { flex: 1; }

    .form-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
    }

    .required-star {
      color: #ef4444;
    }

    .field-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      min-height: 16px;
    }

    .error-hint {
      font-size: 11px;
      color: #ef4444;
      font-weight: 600;
    }

    .char-count {
      font-size: 11px;
      color: var(--text-muted);
      margin-left: auto;
    }

    .input-error {
      border-color: #ef4444 !important;
    }

    .color-picker-row {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .color-input {
      width: 44px;
      height: 38px;
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
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
      width: 26px;
      height: 26px;
      border-radius: 50%;
      cursor: pointer;
      transition: transform 0.15s;
      border: 2px solid transparent;

      &:hover { transform: scale(1.15); }
      &.selected { border-color: white; box-shadow: 0 0 0 2px var(--primary); }
    }

    .options-group {
      display: flex;
      flex-direction: column;
      gap: 10px;
      margin-top: 4px;
    }

    .checkbox-card {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 10px 14px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
      }

      &.checked {
        border-color: rgba(99, 102, 241, 0.4);
        background: rgba(99, 102, 241, 0.05);
      }

      input[type="checkbox"] {
        margin-top: 3px;
        accent-color: var(--primary);
      }
    }

    .checkbox-text {
      display: flex;
      flex-direction: column;
      gap: 2px;

      strong {
        font-size: 13px;
        color: var(--text-primary);
      }

      span {
        font-size: 11px;
        color: var(--text-muted);
      }
    }

    .modal-footer {
      padding: 14px 20px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);
      display: flex;
      justify-content: flex-end;
      gap: 10px;
    }

    /* Staff & Categories Modals */
    .section-notice {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 0;
      padding: 10px 12px;
      background: rgba(59, 130, 246, 0.08);
      border-left: 3px solid #3b82f6;
      border-radius: 4px;
    }

    .staff-selection-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 10px;
      max-height: 360px;
      overflow-y: auto;
      padding-right: 4px;
    }

    .staff-check-card {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        border-color: var(--border-light);
      }

      &.selected {
        border-color: #f59e0b;
        background: rgba(245, 158, 11, 0.08);
      }

      input[type="checkbox"] {
        accent-color: #f59e0b;
        width: 16px;
        height: 16px;
      }
    }

    .staff-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;
    }

    .staff-name-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .staff-name {
      font-size: 13px;
      color: var(--text-primary);
    }

    .role-badge {
      font-size: 10px;
      padding: 1px 6px;
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: 4px;
      color: var(--text-muted);

      &.role-kitchen {
        background: rgba(245, 158, 11, 0.15);
        color: #d97706;
        border-color: rgba(245, 158, 11, 0.3);
      }
    }

    .staff-username {
      font-size: 11px;
      color: var(--text-muted);
    }

    .assigned-cats-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 320px;
      overflow-y: auto;
    }

    .cat-list-item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 12px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
    }

    .cat-bullet {
      color: var(--primary);
      font-size: 12px;
    }

    .cat-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      flex: 1;
    }

    .cat-status {
      font-size: 11px;
      color: #64748b;
      &.active { color: #10b981; }
    }

    /* Delete dialog */
    .delete-warning-text {
      font-size: 14px;
      color: var(--text-primary);
      line-height: 1.5;
      margin: 0;
    }

    .delete-info-box {
      display: flex;
      gap: 12px;
      padding: 12px 14px;
      background: rgba(245, 158, 11, 0.1);
      border-left: 4px solid #f59e0b;
      border-radius: 4px;

      .info-icon {
        font-size: 20px;
      }

      .info-content {
        font-size: 12px;
        color: var(--text-secondary);
        line-height: 1.5;

        strong {
          color: #b45309;
          display: block;
          margin-bottom: 4px;
        }

        p {
          margin: 0;
        }
      }
    }

    /* Common states */
    .state-container {
      padding: 48px 20px;
      text-align: center;
      color: var(--text-muted);
    }

    .empty-state {
      .empty-icon {
        font-size: 48px;
        margin-bottom: 12px;
      }

      h3 {
        font-size: 18px;
        color: var(--text-primary);
        margin: 0 0 6px 0;
      }

      p {
        font-size: 13px;
        margin: 0;
      }
    }

    .spinner {
      width: 32px;
      height: 32px;
      border: 3px solid var(--border);
      border-top-color: var(--primary);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 14px;
    }

    @keyframes spin {
      100% { transform: rotate(360deg); }
    }
  `]
})
export class KitchenManagementComponent implements OnInit {
  kitchens: KitchenStation[] = [];
  printers: Printer[] = [];
  allStaff: Employee[] = [];
  loading = false;

  // Search & Filter
  searchQuery = '';
  statusFilter: 'ALL' | 'ACTIVE' | 'INACTIVE' = 'ALL';

  // Pagination
  pageIndex = 0;
  pageSize = 10;
  pageSizeOptions = [10, 25, 50, 100];

  // Form Modal State
  showFormModal = false;
  isEditing = false;
  editingId: string | null = null;
  saving = false;
  nameError = '';

  presetColors = ['#6366F1', '#f59e0b', '#10b981', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#3b82f6'];

  formData: {
    name: string;
    code: string;
    description: string;
    active: boolean;
    color: string;
    autoPrint: boolean;
    soundNotification: boolean;
    preparationTimeMinutes: number;
    printerId: string | null;
  } = {
    name: '',
    code: '',
    description: '',
    active: true,
    color: '#6366F1',
    autoPrint: true,
    soundNotification: true,
    preparationTimeMinutes: 15,
    printerId: null
  };

  // Staff Modal State
  showStaffModal = false;
  selectedKitchenForStaff: KitchenStation | null = null;
  selectedEmployeeIds = new Set<string>();
  loadingStaffList = false;
  savingStaff = false;

  // Categories Modal State
  showCategoriesModal = false;
  selectedKitchenForCats: KitchenStation | null = null;
  assignedCategories: AssignedCategory[] = [];
  loadingCatsList = false;

  // Delete Modal State
  showDeleteModal = false;
  targetKitchenForDelete: KitchenStation | null = null;
  deleting = false;

  constructor(
    private kitchenService: KitchenService,
    private userService: UserService,
    private printerService: PrinterService,
    private notify: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadData();
    this.loadPrinters();
    this.loadStaff();
  }

  loadData(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.kitchenService.getKitchens().subscribe({
      next: (res) => {
        this.kitchens = res.data || [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load kitchens', err);
        this.notify.error('Oshxonalarni yuklashda xatolik yuz berdi');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  loadPrinters(): void {
    this.printerService.getPrinters().subscribe({
      next: (res) => {
        this.printers = res.data || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  loadStaff(): void {
    this.userService.getUsers().subscribe({
      next: (res) => {
        this.allStaff = res.data || [];
        this.cdr.markForCheck();
      },
      error: () => {}
    });
  }

  // Metrics
  get activeCount(): number {
    return this.kitchens.filter(k => k.active).length;
  }

  get inactiveCount(): number {
    return this.kitchens.filter(k => !k.active).length;
  }

  get totalAssignedStaff(): number {
    return this.kitchens.reduce((sum, k) => sum + (k.assignedEmployeesCount || 0), 0);
  }

  // Filter & Search Logic
  get filteredKitchens(): KitchenStation[] {
    return this.kitchens.filter(k => {
      if (this.statusFilter === 'ACTIVE' && !k.active) return false;
      if (this.statusFilter === 'INACTIVE' && k.active) return false;

      if (this.searchQuery && this.searchQuery.trim()) {
        const q = this.searchQuery.trim().toLowerCase();
        const matchesName = (k.name || '').toLowerCase().includes(q);
        const matchesCode = (k.code || '').toLowerCase().includes(q);
        const matchesDesc = (k.description || '').toLowerCase().includes(q);
        return matchesName || matchesCode || matchesDesc;
      }
      return true;
    });
  }

  get pagedKitchens(): KitchenStation[] {
    const list = this.filteredKitchens;
    if (this.pageIndex * this.pageSize >= list.length && list.length > 0) {
      this.pageIndex = Math.max(0, Math.ceil(list.length / this.pageSize) - 1);
    }
    const start = this.pageIndex * this.pageSize;
    return list.slice(start, start + this.pageSize);
  }

  onSearchChange(): void {
    this.pageIndex = 0;
  }

  setStatusFilter(filter: 'ALL' | 'ACTIVE' | 'INACTIVE'): void {
    this.statusFilter = filter;
    this.pageIndex = 0;
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  // Create / Edit Modals
  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.nameError = '';
    this.formData = {
      name: '',
      code: '',
      description: '',
      active: true,
      color: '#6366F1',
      autoPrint: true,
      soundNotification: true,
      preparationTimeMinutes: 15,
      printerId: null
    };
    this.showFormModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(kitchen: KitchenStation): void {
    this.isEditing = true;
    this.editingId = kitchen.id;
    this.nameError = '';
    this.formData = {
      name: kitchen.name,
      code: kitchen.code,
      description: kitchen.description || '',
      active: kitchen.active,
      color: kitchen.color || '#6366F1',
      autoPrint: kitchen.autoPrint !== false,
      soundNotification: kitchen.soundNotification !== false,
      preparationTimeMinutes: kitchen.preparationTimeMinutes || 15,
      printerId: kitchen.printerId || null
    };
    this.showFormModal = true;
    this.cdr.markForCheck();
  }

  closeFormModal(): void {
    this.showFormModal = false;
    this.isEditing = false;
    this.editingId = null;
    this.nameError = '';
    this.cdr.markForCheck();
  }

  onNameChange(): void {
    const name = (this.formData.name || '').trim();
    if (!name) {
      this.nameError = 'Oshxona nomi kiritilishi shart';
      return;
    }
    if (name.length > 100) {
      this.nameError = 'Oshxona nomi 100 belgidan oshmasligi kerak';
      return;
    }

    // Duplicate check on active kitchens
    const duplicate = this.kitchens.find(k =>
      k.name.trim().toLowerCase() === name.toLowerCase() &&
      (!this.isEditing || k.id !== this.editingId)
    );

    if (duplicate) {
      this.nameError = `"${duplicate.name}" nomli oshxona allaqachon mavjud!`;
      return;
    }

    this.nameError = '';
  }

  saveForm(): void {
    this.onNameChange();
    if (this.nameError) return;

    const trimmedName = (this.formData.name || '').trim();
    if (!trimmedName) {
      this.nameError = 'Oshxona nomi majburiy';
      return;
    }

    this.saving = true;
    this.cdr.markForCheck();

    if (this.isEditing && this.editingId) {
      const updateReq: UpdateKitchenRequest = {
        name: trimmedName,
        code: this.formData.code ? this.formData.code.trim().toUpperCase() : undefined,
        description: this.formData.description ? this.formData.description.trim() : '',
        active: this.formData.active,
        color: this.formData.color,
        autoPrint: this.formData.autoPrint,
        soundNotification: this.formData.soundNotification,
        preparationTimeMinutes: this.formData.preparationTimeMinutes,
        printerId: this.formData.printerId || undefined
      };

      this.kitchenService.updateKitchen(this.editingId, updateReq).subscribe({
        next: (res) => {
          this.saving = false;
          this.notify.success(res.message || 'Oshxona muvaffaqiyatli yangilandi');
          this.closeFormModal();
          this.loadData();
        },
        error: (err) => {
          this.saving = false;
          const msg = err.error?.message || 'Oshxonani saqlashda xatolik yuz berdi';
          this.notify.error(msg);
          this.cdr.markForCheck();
        }
      });
    } else {
      const createReq: CreateKitchenRequest = {
        name: trimmedName,
        code: this.formData.code ? this.formData.code.trim().toUpperCase() : undefined,
        description: this.formData.description ? this.formData.description.trim() : '',
        active: this.formData.active,
        color: this.formData.color,
        autoPrint: this.formData.autoPrint,
        soundNotification: this.formData.soundNotification,
        preparationTimeMinutes: this.formData.preparationTimeMinutes,
        printerId: this.formData.printerId || undefined
      };

      this.kitchenService.createKitchen(createReq).subscribe({
        next: (res) => {
          this.saving = false;
          this.notify.success(res.message || 'Yangi oshxona muvaffaqiyatli yaratildi');
          this.closeFormModal();
          this.loadData();
        },
        error: (err) => {
          this.saving = false;
          const msg = err.error?.message || 'Yangi oshxona yaratishda xatolik yuz berdi';
          this.notify.error(msg);
          this.cdr.markForCheck();
        }
      });
    }
  }

  // Status Toggle
  toggleStatus(kitchen: KitchenStation): void {
    const nextStatus = !kitchen.active;
    this.kitchenService.toggleKitchenStatus(kitchen.id, nextStatus).subscribe({
      next: (res) => {
        kitchen.active = nextStatus;
        this.notify.success(`"${kitchen.name}" oshxonasi ${nextStatus ? 'faollashtirildi' : 'nofaol qilindi'}`);
        this.loadData();
      },
      error: (err) => {
        const msg = err.error?.message || 'Statusni o‘zgartirishda xatolik yuz berdi';
        this.notify.error(msg);
      }
    });
  }

  // Staff Assignment Modal
  openStaffAssignmentModal(kitchen: KitchenStation): void {
    this.selectedKitchenForStaff = kitchen;
    this.selectedEmployeeIds.clear();
    this.loadingStaffList = true;
    this.showStaffModal = true;
    this.cdr.markForCheck();

    this.kitchenService.getKitchenEmployees(kitchen.id).subscribe({
      next: (res) => {
        const assigned = res.data || [];
        assigned.forEach(a => this.selectedEmployeeIds.add(a.id));
        this.loadingStaffList = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to get kitchen employees', err);
        this.loadingStaffList = false;
        this.cdr.markForCheck();
      }
    });
  }

  closeStaffModal(): void {
    this.showStaffModal = false;
    this.selectedKitchenForStaff = null;
    this.selectedEmployeeIds.clear();
    this.cdr.markForCheck();
  }

  toggleStaffSelection(empId: string): void {
    if (this.selectedEmployeeIds.has(empId)) {
      this.selectedEmployeeIds.delete(empId);
    } else {
      this.selectedEmployeeIds.add(empId);
    }
  }

  saveStaffAssignments(): void {
    if (!this.selectedKitchenForStaff) return;
    this.savingStaff = true;
    this.cdr.markForCheck();

    const employeeIds = Array.from(this.selectedEmployeeIds);
    this.kitchenService.assignKitchenEmployees(this.selectedKitchenForStaff.id, employeeIds).subscribe({
      next: (res) => {
        this.savingStaff = false;
        this.notify.success(res.message || 'Xodimlar muvaffaqiyatli biriktirildi');
        this.closeStaffModal();
        this.loadData();
      },
      error: (err) => {
        this.savingStaff = false;
        const msg = err.error?.message || 'Xodimlarni biriktirishda xatolik yuz berdi';
        this.notify.error(msg);
        this.cdr.markForCheck();
      }
    });
  }

  // Categories Modal
  openCategoryViewModal(kitchen: KitchenStation): void {
    this.selectedKitchenForCats = kitchen;
    this.assignedCategories = [];
    this.loadingCatsList = true;
    this.showCategoriesModal = true;
    this.cdr.markForCheck();

    this.kitchenService.getKitchenCategories(kitchen.id).subscribe({
      next: (res) => {
        this.assignedCategories = res.data || [];
        this.loadingCatsList = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load kitchen categories', err);
        this.loadingCatsList = false;
        this.cdr.markForCheck();
      }
    });
  }

  closeCategoriesModal(): void {
    this.showCategoriesModal = false;
    this.selectedKitchenForCats = null;
    this.assignedCategories = [];
    this.cdr.markForCheck();
  }

  // Delete / Safe Delete Logic
  hasLinkedData(kitchen: KitchenStation | null): boolean {
    if (!kitchen) return false;
    return (kitchen.assignedCategoriesCount || 0) > 0;
  }

  confirmDelete(kitchen: KitchenStation): void {
    this.targetKitchenForDelete = kitchen;
    this.showDeleteModal = true;
    this.cdr.markForCheck();
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.targetKitchenForDelete = null;
    this.deleting = false;
    this.cdr.markForCheck();
  }

  deactivateInsteadOfDelete(): void {
    if (!this.targetKitchenForDelete) return;
    this.deleting = true;
    this.kitchenService.toggleKitchenStatus(this.targetKitchenForDelete.id, false).subscribe({
      next: () => {
        this.deleting = false;
        this.notify.success(`"${this.targetKitchenForDelete?.name}" nofaol (INACTIVE) holatiga o'tkazildi`);
        this.closeDeleteModal();
        this.loadData();
      },
      error: (err) => {
        this.deleting = false;
        const msg = err.error?.message || 'Faolsizlantirishda xatolik yuz berdi';
        this.notify.error(msg);
        this.cdr.markForCheck();
      }
    });
  }

  executeDelete(): void {
    if (!this.targetKitchenForDelete) return;
    this.deleting = true;
    this.kitchenService.deleteKitchen(this.targetKitchenForDelete.id).subscribe({
      next: (res) => {
        this.deleting = false;
        this.notify.success(res.message || 'Oshxona o‘chirildi');
        this.closeDeleteModal();
        this.loadData();
      },
      error: (err) => {
        this.deleting = false;
        const msg = err.error?.message || 'Oshxonani o‘chirishda xatolik yuz berdi';
        this.notify.error(msg, 7000);
        this.cdr.markForCheck();
      }
    });
  }

  formatDate(dateStr?: string): string {
    if (!dateStr) return '—';
    try {
      const d = new Date(dateStr);
      return d.toLocaleDateString('uz-UZ', { year: 'numeric', month: 'short', day: 'numeric' });
    } catch {
      return dateStr;
    }
  }
}
