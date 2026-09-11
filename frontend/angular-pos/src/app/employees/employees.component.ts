import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, Employee, Role, CreateEmployeeRequest, UpdateEmployeeRequest } from '../core/services/user.service';
import { KitchenService, KitchenStation } from '../core/services/kitchen.service';

@Component({
  selector: 'app-employees',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="employees-page fade-in">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">👥 Xodimlar & Lavozimlar</h1>
          <p class="page-subtitle">Ofitsiantlar, kassirlar, oshpazlar va tizim foydalanuvchilarini boshqarish</p>
        </div>

        <button class="pos-btn pos-btn--primary" (click)="openCreateModal()">
          <span>➕ Yangi Xodim Qo'shish</span>
        </button>
      </div>

      <!-- Employees Table Card -->
      <div class="pos-card table-card">
        <div *ngIf="loading && employees.length === 0" class="loading-state">
          <div class="spinner"></div>
          <p>Xodimlar ro'yxati yuklanmoqda...</p>
        </div>

        <div *ngIf="!loading && employees.length === 0" class="empty-state">
          <div class="empty-icon">👥</div>
          <h3>Xodimlar topilmadi</h3>
          <button class="pos-btn pos-btn--primary" (click)="openCreateModal()" style="margin-top: 12px;">
            Yangi xodim qo'shish
          </button>
        </div>

        <div *ngIf="employees.length > 0" class="table-responsive">
          <table class="pos-table">
            <thead>
              <tr>
                <th>Xodim (F.I.Sh)</th>
                <th>Login (Username)</th>
                <th>Lavozim (Rol)</th>
                <th>Oshxonalar</th>
                <th>Telefon</th>
                <th>Holati</th>
                <th style="text-align: right;">Amallar</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let emp of employees">
                <td>
                  <div class="user-cell">
                    <div class="user-avatar">{{ getInitials(emp) }}</div>
                    <div>
                      <strong>{{ emp.firstName }} {{ emp.lastName || '' }}</strong>
                      <div class="email-sub" *ngIf="emp.email">{{ emp.email }}</div>
                    </div>
                  </div>
                </td>
                <td>
                  <code class="username-tag">&#64;{{ emp.username }}</code>
                </td>
                <td>
                  <span class="role-badge" [ngClass]="emp.role?.toLowerCase()">
                    {{ getRoleLabel(emp.role) }}
                  </span>
                </td>
                <td>
                  <div class="kitchen-chips" *ngIf="emp.kitchens && emp.kitchens.length > 0">
                    <span *ngFor="let k of emp.kitchens" class="kitchen-chip">
                      🍳 {{ k.name }}
                    </span>
                  </div>
                  <span *ngIf="!emp.kitchens || emp.kitchens.length === 0" class="text-muted">—</span>
                </td>
                <td>{{ emp.phone || '—' }}</td>
                <td>
                  <span class="status-pill" [class.active]="emp.active" [class.inactive]="!emp.active">
                    {{ emp.active ? '● Faol' : '○ Nofaol' }}
                  </span>
                </td>
                <td style="text-align: right;">
                  <div class="action-buttons">
                    <button
                      class="pos-btn pos-btn--secondary pos-btn--sm"
                      title="Tahrirlash"
                      (click)="openEditModal(emp)">
                      ✏️ Tahrirlash
                    </button>
                    <button
                      class="pos-btn pos-btn--secondary pos-btn--sm"
                      title="Parolni almashtirish"
                      (click)="openResetPasswordModal(emp)">
                      🔑 Parol
                    </button>
                    <button
                      *ngIf="emp.active"
                      class="pos-btn pos-btn--danger pos-btn--sm"
                      title="Nofaol qilish"
                      (click)="toggleActive(emp)">
                      🚫
                    </button>
                    <button
                      *ngIf="!emp.active"
                      class="pos-btn pos-btn--success pos-btn--sm"
                      title="Faollashtirish"
                      (click)="toggleActive(emp)">
                      ✅
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 1: ADD EMPLOYEE                                          -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showCreateModal" (click)="closeModals()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">➕ Yangi Xodim Ro‘yxatga Olish</h2>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body form-grid">
            <div class="form-group">
              <label class="form-label">Ismi *</label>
              <input type="text" [(ngModel)]="createData.firstName" class="pos-input" placeholder="Ali" required />
            </div>

            <div class="form-group">
              <label class="form-label">Familiyasi</label>
              <input type="text" [(ngModel)]="createData.lastName" class="pos-input" placeholder="Valiyev" />
            </div>

            <div class="form-group">
              <label class="form-label">Login (Foydalanuvchi nomi) *</label>
              <input type="text" [(ngModel)]="createData.username" class="pos-input" placeholder="ali_waiter" required />
            </div>

            <div class="form-group">
              <label class="form-label">Parol *</label>
              <input type="password" [(ngModel)]="createData.password" class="pos-input" placeholder="••••••••" required />
            </div>

            <div class="form-group">
              <label class="form-label">Lavozim (Rol) *</label>
              <select [(ngModel)]="createData.role" class="pos-input" (change)="onRoleChange('create')">
                <option value="ADMIN">Admin (Boshqaruvchi)</option>
                <option value="MANAGER">Menejer</option>
                <option value="WAITER">Ofitsiant</option>
                <option value="KITCHEN">Oshpaz (Oshxona)</option>
                <option value="CASHIER">Kassir</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Telefon raqam</label>
              <input type="text" [(ngModel)]="createData.phone" class="pos-input" placeholder="+998 90 123-45-67" />
            </div>

            <!-- Multi-select Kitchens for KITCHEN role -->
            <div class="form-group full-width" *ngIf="createData.role === 'KITCHEN'">
              <label class="form-label">Biriktiriladigan Oshxonalar *</label>
              <p class="field-hint">Kamida bitta oshxona tanlanishi shart (bir yoki bir nechta)</p>
              <div class="kitchen-checkbox-grid">
                <label *ngFor="let k of kitchens" class="kitchen-check-card" [class.selected]="isKitchenSelected(k.id, 'create')">
                  <input
                    type="checkbox"
                    [checked]="isKitchenSelected(k.id, 'create')"
                    (change)="toggleKitchen(k.id, 'create')"
                  />
                  <div class="kitchen-check-info">
                    <span class="kitchen-check-name">{{ k.name }}</span>
                    <span class="kitchen-check-code">{{ k.code }}</span>
                  </div>
                </label>
              </div>
              <div *ngIf="createKitchenError" class="validation-error">
                ⚠️ Oshpaz kamida bitta oshxonaga biriktirilishi kerak.
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="saving">Bekor qilish</button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveCreate()"
              [disabled]="saving || !createData.username || !createData.password || !createData.firstName || (createData.role === 'KITCHEN' && selectedCreateKitchenIds.size === 0)">
              <span>{{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 2: EDIT EMPLOYEE                                         -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showEditModal && selectedEmp" (click)="closeModals()">
        <div class="modal-card" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">✏️ Xodimni Tahrirlash: &#64;{{ selectedEmp.username }}</h2>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body form-grid">
            <div class="form-group">
              <label class="form-label">Ismi *</label>
              <input type="text" [(ngModel)]="editData.firstName" class="pos-input" required />
            </div>

            <div class="form-group">
              <label class="form-label">Familiyasi</label>
              <input type="text" [(ngModel)]="editData.lastName" class="pos-input" />
            </div>

            <div class="form-group">
              <label class="form-label">Lavozim (Rol) *</label>
              <select [(ngModel)]="editData.role" class="pos-input" (change)="onRoleChange('edit')">
                <option value="ADMIN">Admin (Boshqaruvchi)</option>
                <option value="MANAGER">Menejer</option>
                <option value="WAITER">Ofitsiant</option>
                <option value="KITCHEN">Oshpaz (Oshxona)</option>
                <option value="CASHIER">Kassir</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Telefon raqam</label>
              <input type="text" [(ngModel)]="editData.phone" class="pos-input" />
            </div>

            <!-- Multi-select Kitchens for KITCHEN role in Edit Modal -->
            <div class="form-group full-width" *ngIf="editData.role === 'KITCHEN'">
              <label class="form-label">Biriktirilgan Oshxonalar *</label>
              <p class="field-hint">Kamida bitta oshxona tanlanishi shart (bir yoki bir nechta)</p>
              <div class="kitchen-checkbox-grid">
                <label *ngFor="let k of kitchens" class="kitchen-check-card" [class.selected]="isKitchenSelected(k.id, 'edit')">
                  <input
                    type="checkbox"
                    [checked]="isKitchenSelected(k.id, 'edit')"
                    (change)="toggleKitchen(k.id, 'edit')"
                  />
                  <div class="kitchen-check-info">
                    <span class="kitchen-check-name">{{ k.name }}</span>
                    <span class="kitchen-check-code">{{ k.code }}</span>
                  </div>
                </label>
              </div>
              <div *ngIf="editKitchenError" class="validation-error">
                ⚠️ Oshpaz kamida bitta oshxonaga biriktirilishi kerak.
              </div>
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="saving">Bekor qilish</button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveEdit()"
              [disabled]="saving || !editData.firstName || (editData.role === 'KITCHEN' && selectedEditKitchenIds.size === 0)">
              <span>{{ saving ? 'Saqlanmoqda...' : 'Saqlash' }}</span>
            </button>
          </div>
        </div>
      </div>

      <!-- ============================================================ -->
      <!-- MODAL 3: RESET PASSWORD                                        -->
      <!-- ============================================================ -->
      <div class="modal-overlay" *ngIf="showPasswordModal && selectedEmp" (click)="closeModals()">
        <div class="modal-card modal-card--sm" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">🔑 Parolni O'zgartirish</h2>
            <button class="close-btn" (click)="closeModals()">✕</button>
          </div>

          <div class="modal-body">
            <p style="font-size: 13px; color: var(--text-secondary); margin-bottom: 16px;">
              Foydalanuvchi: <strong>&#64;{{ selectedEmp.username }} ({{ selectedEmp.firstName }})</strong>
            </p>

            <div class="form-group">
              <label class="form-label">Yangi Parol *</label>
              <input
                type="password"
                [(ngModel)]="newPassword"
                class="pos-input"
                placeholder="Yangi parol (kamida 6 belgi)"
                required
              />
            </div>
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="saving">Bekor qilish</button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="savePassword()"
              [disabled]="saving || !newPassword || newPassword.length < 4">
              <span>{{ saving ? 'O‘zgartirilmoqda...' : 'Parolni yangilash' }}</span>
            </button>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: [`
    .employees-page {
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

    .user-cell {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, var(--primary), var(--primary-dark));
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
    }

    .email-sub {
      font-size: 11px;
      color: var(--text-muted);
    }

    .username-tag {
      font-family: var(--font-mono);
      font-size: 12px;
      color: var(--primary-light);
      background: var(--bg-secondary);
      padding: 2px 6px;
      border-radius: 4px;
    }

    .role-badge {
      font-size: 12px;
      font-weight: 600;
      padding: 4px 10px;
      border-radius: 12px;

      &.admin { background: rgba(239, 68, 68, 0.15); color: #f87171; }
      &.manager { background: rgba(139, 92, 246, 0.15); color: #c084fc; }
      &.waiter { background: rgba(59, 130, 246, 0.15); color: #60a5fa; }
      &.kitchen { background: rgba(245, 158, 11, 0.15); color: #fbbf24; }
      &.cashier { background: rgba(16, 185, 129, 0.15); color: #34d399; }
    }

    .status-pill {
      font-size: 12px;
      font-weight: 600;

      &.active { color: var(--success); }
      &.inactive { color: var(--danger); }
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

      &--sm { max-width: 400px; }
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
      gap: 14px;
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

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      padding: 16px 20px;
      background: var(--bg-tertiary);
      border-top: 1px solid var(--border);
    }

    .full-width {
      grid-column: 1 / -1;
    }

    .field-hint {
      font-size: 11px;
      color: var(--text-muted);
      margin: -2px 0 4px 0;
    }

    .kitchen-checkbox-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
      gap: 8px;
      margin-top: 4px;
    }

    .kitchen-check-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 12px;
      background: var(--bg-secondary);
      border: 1px solid var(--border);
      border-radius: var(--radius-sm);
      cursor: pointer;
      transition: all var(--transition);

      &:hover {
        background: var(--bg-hover);
        border-color: var(--primary-light);
      }

      &.selected {
        background: rgba(245, 158, 11, 0.12);
        border-color: #f59e0b;
      }

      input[type="checkbox"] {
        accent-color: #f59e0b;
        cursor: pointer;
        width: 16px;
        height: 16px;
      }
    }

    .kitchen-check-info {
      display: flex;
      flex-direction: column;
    }

    .kitchen-check-name {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-primary);
    }

    .kitchen-check-code {
      font-size: 10px;
      color: var(--text-muted);
    }

    .validation-error {
      font-size: 12px;
      color: #ef4444;
      font-weight: 600;
      margin-top: 6px;
    }

    .kitchen-chips {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
    }

    .kitchen-chip {
      font-size: 11px;
      font-weight: 600;
      background: rgba(245, 158, 11, 0.15);
      color: #fbbf24;
      border: 1px solid rgba(245, 158, 11, 0.3);
      padding: 2px 8px;
      border-radius: 12px;
      display: inline-flex;
      align-items: center;
      gap: 3px;
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
export class EmployeesComponent implements OnInit {
  employees: Employee[] = [];
  kitchens: KitchenStation[] = [];
  loading = false;
  saving = false;

  // Multi-select kitchen sets
  selectedCreateKitchenIds = new Set<string>();
  selectedEditKitchenIds = new Set<string>();
  createKitchenError = false;
  editKitchenError = false;

  // Modals
  showCreateModal = false;
  showEditModal = false;
  showPasswordModal = false;
  selectedEmp: Employee | null = null;

  createData: CreateEmployeeRequest = {
    username: '',
    password: '',
    firstName: '',
    lastName: '',
    phone: '',
    role: 'WAITER',
    kitchenIds: []
  };

  editData: UpdateEmployeeRequest = {
    firstName: '',
    lastName: '',
    phone: '',
    role: 'WAITER',
    kitchenIds: []
  };

  newPassword = '';

  constructor(
    private userService: UserService,
    private kitchenService: KitchenService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadEmployees();
    this.loadKitchens();
  }

  loadKitchens(): void {
    this.kitchenService.getKitchens().subscribe({
      next: (res) => {
        this.kitchens = res.data || [];
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load kitchens', err);
      }
    });
  }

  loadEmployees(): void {
    this.loading = true;
    this.cdr.markForCheck();
    this.userService.getUsers().subscribe({
      next: (res) => {
        this.employees = res.data || [];
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load employees', err);
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  onRoleChange(mode: 'create' | 'edit'): void {
    if (mode === 'create') {
      if (this.createData.role === 'KITCHEN') {
        this.createKitchenError = this.selectedCreateKitchenIds.size === 0;
      } else {
        this.createKitchenError = false;
      }
    } else {
      if (this.editData.role === 'KITCHEN') {
        this.editKitchenError = this.selectedEditKitchenIds.size === 0;
      } else {
        this.editKitchenError = false;
      }
    }
    this.cdr.markForCheck();
  }

  toggleKitchen(kitchenId: string, mode: 'create' | 'edit'): void {
    const targetSet = mode === 'create' ? this.selectedCreateKitchenIds : this.selectedEditKitchenIds;
    if (targetSet.has(kitchenId)) {
      targetSet.delete(kitchenId);
    } else {
      targetSet.add(kitchenId);
    }

    if (mode === 'create') {
      this.createKitchenError = this.createData.role === 'KITCHEN' && targetSet.size === 0;
    } else {
      this.editKitchenError = this.editData.role === 'KITCHEN' && targetSet.size === 0;
    }
    this.cdr.markForCheck();
  }

  isKitchenSelected(kitchenId: string, mode: 'create' | 'edit'): boolean {
    return mode === 'create'
      ? this.selectedCreateKitchenIds.has(kitchenId)
      : this.selectedEditKitchenIds.has(kitchenId);
  }

  getInitials(emp: Employee): string {
    const first = emp.firstName ? emp.firstName[0].toUpperCase() : '';
    const last = emp.lastName ? emp.lastName[0].toUpperCase() : '';
    return first + last || 'U';
  }

  getRoleLabel(role?: string): string {
    switch (role) {
      case 'ADMIN': return '👑 Admin';
      case 'MANAGER': return '👔 Menejer';
      case 'WAITER': return '🛎️ Ofitsiant';
      case 'KITCHEN': return '👨‍🍳 Oshpaz';
      case 'CASHIER': return '💳 Kassir';
      default: return role || 'Xodim';
    }
  }

  openCreateModal(): void {
    this.createData = {
      username: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      role: 'WAITER',
      kitchenIds: []
    };
    this.selectedCreateKitchenIds.clear();
    this.createKitchenError = false;
    this.showCreateModal = true;
    this.cdr.markForCheck();
  }

  openEditModal(emp: Employee): void {
    this.selectedEmp = emp;
    const empKitchenIds = emp.kitchenIds || emp.kitchens?.map(k => k.id) || [];
    this.editData = {
      firstName: emp.firstName,
      lastName: emp.lastName,
      phone: emp.phone,
      role: emp.role || 'WAITER',
      active: emp.active,
      kitchenIds: [...empKitchenIds]
    };
    this.selectedEditKitchenIds = new Set<string>(empKitchenIds);
    this.editKitchenError = false;
    this.showEditModal = true;
    this.cdr.markForCheck();
  }

  openResetPasswordModal(emp: Employee): void {
    this.selectedEmp = emp;
    this.newPassword = '';
    this.showPasswordModal = true;
    this.cdr.markForCheck();
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.showPasswordModal = false;
    this.selectedEmp = null;
    this.cdr.markForCheck();
  }

  saveCreate(): void {
    if (this.createData.role === 'KITCHEN') {
      this.createData.kitchenIds = Array.from(this.selectedCreateKitchenIds);
      if (this.createData.kitchenIds.length === 0) {
        this.createKitchenError = true;
        this.cdr.markForCheck();
        alert('Oshpaz kamida bitta oshxonaga biriktirilishi kerak.');
        return;
      }
    } else {
      this.createData.kitchenIds = [];
    }

    this.saving = true;
    this.cdr.markForCheck();
    this.userService.createUser(this.createData).subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.loadEmployees();
      },
      error: (err) => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  saveEdit(): void {
    if (!this.selectedEmp) return;
    if (this.editData.role === 'KITCHEN') {
      this.editData.kitchenIds = Array.from(this.selectedEditKitchenIds);
      if (this.editData.kitchenIds.length === 0) {
        this.editKitchenError = true;
        this.cdr.markForCheck();
        alert('Oshpaz kamida bitta oshxonaga biriktirilishi kerak.');
        return;
      }
    } else {
      this.editData.kitchenIds = [];
    }

    this.saving = true;
    this.cdr.markForCheck();
    this.userService.updateUser(this.selectedEmp.id, this.editData).subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.loadEmployees();
      },
      error: (err) => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  savePassword(): void {
    if (!this.selectedEmp || !this.newPassword) return;
    this.saving = true;
    this.cdr.markForCheck();
    this.userService.resetPassword(this.selectedEmp.id, this.newPassword).subscribe({
      next: () => {
        this.saving = false;
        alert('Parol muvaffaqiyatli almashtirildi!');
        this.closeModals();
      },
      error: (err) => {
        this.saving = false;
        this.cdr.markForCheck();
        alert('Xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  toggleActive(emp: Employee): void {
    const updatedStatus = !emp.active;
    const req: UpdateEmployeeRequest = {
      firstName: emp.firstName,
      lastName: emp.lastName,
      phone: emp.phone,
      role: emp.role,
      active: updatedStatus
    };

    this.userService.updateUser(emp.id, req).subscribe({
      next: () => {
        emp.active = updatedStatus;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.cdr.markForCheck();
        alert('Statusni o‘zgartirishda xatolik: ' + (err.error?.message || err.message));
      }
    });
  }
}
