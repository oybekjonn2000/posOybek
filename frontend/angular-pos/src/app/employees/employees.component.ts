import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, Employee, Role, CreateEmployeeRequest, UpdateEmployeeRequest } from '../core/services/user.service';

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
              <select [(ngModel)]="createData.role" class="pos-input">
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
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="saving">Bekor qilish</button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveCreate()"
              [disabled]="saving || !createData.username || !createData.password || !createData.firstName">
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
              <select [(ngModel)]="editData.role" class="pos-input">
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
          </div>

          <div class="modal-footer">
            <button class="pos-btn pos-btn--secondary" (click)="closeModals()" [disabled]="saving">Bekor qilish</button>
            <button
              class="pos-btn pos-btn--primary"
              (click)="saveEdit()"
              [disabled]="saving || !editData.firstName">
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
  loading = false;
  saving = false;

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
    role: 'WAITER'
  };

  editData: UpdateEmployeeRequest = {
    firstName: '',
    lastName: '',
    phone: '',
    role: 'WAITER'
  };

  newPassword = '';

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading = true;
    this.userService.getUsers().subscribe({
      next: (res) => {
        this.employees = res.data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('Failed to load employees', err);
        this.loading = false;
      }
    });
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
      role: 'WAITER'
    };
    this.showCreateModal = true;
  }

  openEditModal(emp: Employee): void {
    this.selectedEmp = emp;
    this.editData = {
      firstName: emp.firstName,
      lastName: emp.lastName,
      phone: emp.phone,
      role: emp.role || 'WAITER',
      active: emp.active
    };
    this.showEditModal = true;
  }

  openResetPasswordModal(emp: Employee): void {
    this.selectedEmp = emp;
    this.newPassword = '';
    this.showPasswordModal = true;
  }

  closeModals(): void {
    this.showCreateModal = false;
    this.showEditModal = false;
    this.showPasswordModal = false;
    this.selectedEmp = null;
  }

  saveCreate(): void {
    this.saving = true;
    this.userService.createUser(this.createData).subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.loadEmployees();
      },
      error: (err) => {
        this.saving = false;
        alert('Xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  saveEdit(): void {
    if (!this.selectedEmp) return;
    this.saving = true;
    this.userService.updateUser(this.selectedEmp.id, this.editData).subscribe({
      next: () => {
        this.saving = false;
        this.closeModals();
        this.loadEmployees();
      },
      error: (err) => {
        this.saving = false;
        alert('Xatolik: ' + (err.error?.message || err.message));
      }
    });
  }

  savePassword(): void {
    if (!this.selectedEmp || !this.newPassword) return;
    this.saving = true;
    this.userService.resetPassword(this.selectedEmp.id, this.newPassword).subscribe({
      next: () => {
        this.saving = false;
        alert('Parol muvaffaqiyatli almashtirildi!');
        this.closeModals();
      },
      error: (err) => {
        this.saving = false;
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
      },
      error: (err) => alert('Statusni o‘zgartirishda xatolik: ' + (err.error?.message || err.message))
    });
  }
}
