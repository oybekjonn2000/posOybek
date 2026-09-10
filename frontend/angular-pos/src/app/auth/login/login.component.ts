import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

interface QuickAccount {
  role: string;
  title: string;
  name: string;
  username: string;
  password: string;
  icon: string;
  badgeClass: string;
  description: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="login-page">
      <!-- Background decorations -->
      <div class="login-page__bg">
        <div class="login-page__blob login-page__blob--1"></div>
        <div class="login-page__blob login-page__blob--2"></div>
      </div>

      <div class="login-container">
        <!-- Header -->
        <div class="login-header">
          <div class="login-logo">🍽️</div>
          <h1 class="login-title">RestaurantPOS</h1>
          <p class="login-subtitle">Professional Restaurant Management</p>
        </div>

        <!-- Login Card -->
        <div class="login-card">
          <h2 class="login-card__title">Welcome back</h2>
          <p class="login-card__desc">Sign in to continue to your POS</p>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="login-form">
            <div class="form-group">
              <label class="form-label">Username</label>
              <input
                type="text"
                class="pos-input pos-input--lg"
                formControlName="username"
                placeholder="Enter your username"
                autocomplete="username"
              />
              @if (loginForm.get('username')?.invalid && loginForm.get('username')?.touched) {
                <span class="form-error">Username is required</span>
              }
            </div>

            <div class="form-group">
              <label class="form-label">Password</label>
              <div class="password-field">
                <input
                  [type]="showPassword() ? 'text' : 'password'"
                  class="pos-input pos-input--lg"
                  formControlName="password"
                  placeholder="Enter your password"
                  autocomplete="current-password"
                />
                <button
                  type="button"
                  class="password-toggle"
                  (click)="showPassword.update(v => !v)"
                >
                  {{ showPassword() ? '🙈' : '👁️' }}
                </button>
              </div>
              @if (loginForm.get('password')?.invalid && loginForm.get('password')?.touched) {
                <span class="form-error">Password is required</span>
              }
            </div>

            @if (errorMessage()) {
              <div class="login-error">
                ❌ {{ errorMessage() }}
              </div>
            }

            <button
              type="submit"
              class="pos-btn pos-btn--primary pos-btn--lg w-full"
              [disabled]="loading() || loginForm.invalid"
            >
              @if (loading() && !activeQuickUser()) {
                <span class="spinner"></span>
                Signing in...
              } @else {
                Sign In
              }
            </button>
          </form>

          <!-- Quick Role Login Section -->
          <div class="quick-login-divider">
            <span>Tezkor kirish (Lavozimlar)</span>
          </div>

          <div class="quick-login-grid">
            @for (acc of quickAccounts; track acc.username) {
              <button
                type="button"
                class="quick-account-btn {{ acc.badgeClass }}"
                [class.quick-account-btn--loading]="activeQuickUser() === acc.username && loading()"
                [disabled]="loading()"
                (click)="quickLogin(acc)"
                [title]="acc.name + ' (' + acc.title + ') sifatida 1-bosishda kirish'"
              >
                <span class="quick-icon">{{ acc.icon }}</span>
                <div class="quick-text">
                  <div class="quick-role-row">
                    <span class="quick-role">{{ acc.title }}</span>
                  </div>
                  <span class="quick-user">{{ acc.name }}</span>
                </div>
                @if (activeQuickUser() === acc.username && loading()) {
                  <span class="quick-spinner"></span>
                } @else {
                  <span class="quick-badge-arrow">⚡</span>
                }
              </button>
            }
          </div>
        </div>

        <!-- Footer -->
        <div class="login-footer">
          <div class="offline-badge">
            🔒 Works offline • Data stays local
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-primary);
      position: relative;
      overflow: hidden;

      &__bg { position: absolute; inset: 0; pointer-events: none; }

      &__blob {
        position: absolute;
        border-radius: 50%;
        filter: blur(80px);
        opacity: 0.15;

        &--1 {
          width: 600px;
          height: 600px;
          background: var(--primary);
          top: -100px;
          right: -100px;
        }

        &--2 {
          width: 400px;
          height: 400px;
          background: var(--accent);
          bottom: -100px;
          left: -100px;
        }
      }
    }

    .login-container {
      width: 100%;
      max-width: 450px;
      padding: 24px;
      position: relative;
      z-index: 1;
    }

    .login-header {
      text-align: center;
      margin-bottom: 24px;
    }

    .login-logo {
      font-size: 52px;
      margin-bottom: 10px;
      filter: drop-shadow(0 4px 12px rgba(0,0,0,0.3));
    }

    .login-title {
      font-size: 28px;
      font-weight: 800;
      background: linear-gradient(135deg, var(--primary-light), var(--accent));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin-bottom: 4px;
    }

    .login-subtitle {
      color: var(--text-muted);
      font-size: 14px;
    }

    .login-card {
      background: var(--bg-card);
      border: 1px solid var(--border);
      border-radius: var(--radius-xl);
      padding: 28px 26px;
      box-shadow: var(--shadow-lg);

      &__title {
        font-size: 20px;
        font-weight: 700;
        color: var(--text-primary);
        margin-bottom: 4px;
      }

      &__desc {
        color: var(--text-muted);
        font-size: 14px;
        margin-bottom: 22px;
      }
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 7px;
    }

    .form-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--text-secondary);
    }

    .form-error {
      font-size: 12px;
      color: var(--danger);
    }

    .password-field {
      position: relative;
    }

    .password-toggle {
      position: absolute;
      right: 12px;
      top: 50%;
      transform: translateY(-50%);
      background: none;
      border: none;
      cursor: pointer;
      font-size: 18px;
      line-height: 1;
    }

    .login-error {
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--radius-sm);
      padding: 12px 16px;
      color: var(--danger);
      font-size: 14px;
    }

    /* Quick Login Section */
    .quick-login-divider {
      display: flex;
      align-items: center;
      margin: 22px 0 14px;
      text-align: center;
      color: var(--text-muted);
      font-size: 11px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.8px;

      &::before, &::after {
        content: '';
        flex: 1;
        border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      }

      span {
        padding: 0 12px;
        color: var(--text-secondary);
      }
    }

    .quick-login-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 9px;

      & > button:nth-child(5) {
        grid-column: span 2;
      }
    }

    .quick-account-btn {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 9px 12px;
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: var(--radius-md);
      cursor: pointer;
      text-align: left;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
      position: relative;
      overflow: hidden;

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0, 0, 0, 0.3);
      }

      &:active:not(:disabled) {
        transform: translateY(0);
      }

      &:disabled {
        opacity: 0.6;
        cursor: not-allowed;
      }

      &--loading {
        border-color: var(--primary) !important;
        background: rgba(99, 102, 241, 0.15) !important;
      }

      /* Roles Colors & Accents */
      &.badge-admin {
        border-color: rgba(168, 85, 247, 0.25);
        &:hover:not(:disabled) {
          border-color: #a855f7;
          background: rgba(168, 85, 247, 0.12);
        }
        .quick-icon { background: rgba(168, 85, 247, 0.2); }
        .quick-role { color: #d8b4fe; }
      }

      &.badge-manager {
        border-color: rgba(56, 189, 248, 0.25);
        &:hover:not(:disabled) {
          border-color: #38bdf8;
          background: rgba(56, 189, 248, 0.12);
        }
        .quick-icon { background: rgba(56, 189, 248, 0.2); }
        .quick-role { color: #7dd3fc; }
      }

      &.badge-cashier {
        border-color: rgba(52, 211, 153, 0.25);
        &:hover:not(:disabled) {
          border-color: #34d399;
          background: rgba(52, 211, 153, 0.12);
        }
        .quick-icon { background: rgba(52, 211, 153, 0.2); }
        .quick-role { color: #6ee7b7; }
      }

      &.badge-waiter {
        border-color: rgba(251, 191, 36, 0.25);
        &:hover:not(:disabled) {
          border-color: #fbbf24;
          background: rgba(251, 191, 36, 0.12);
        }
        .quick-icon { background: rgba(251, 191, 36, 0.2); }
        .quick-role { color: #fcd34d; }
      }

      &.badge-kitchen {
        border-color: rgba(248, 113, 113, 0.25);
        &:hover:not(:disabled) {
          border-color: #f87171;
          background: rgba(248, 113, 113, 0.12);
        }
        .quick-icon { background: rgba(248, 113, 113, 0.2); }
        .quick-role { color: #fca5a5; }
      }
    }

    .quick-icon {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      border-radius: var(--radius-sm);
      flex-shrink: 0;
    }

    .quick-text {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    .quick-role-row {
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .quick-role {
      font-size: 12.5px;
      font-weight: 700;
      line-height: 1.2;
      letter-spacing: -0.2px;
    }

    .quick-user {
      font-size: 11px;
      color: var(--text-muted);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      margin-top: 1px;
    }

    .quick-badge-arrow {
      font-size: 11px;
      color: var(--text-muted);
      opacity: 0.6;
      transition: all 0.2s;
    }

    .quick-account-btn:hover .quick-badge-arrow {
      opacity: 1;
      transform: scale(1.25);
    }

    .quick-spinner {
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255, 255, 255, 0.2);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    .login-footer {
      text-align: center;
      margin-top: 20px;
    }

    .offline-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      background: rgba(99,102,241,0.1);
      border: 1px solid rgba(99,102,241,0.2);
      border-radius: 100px;
      padding: 8px 16px;
      font-size: 12px;
      color: var(--primary-light);
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class LoginComponent {
  loginForm: FormGroup;
  loading = signal(false);
  showPassword = signal(false);
  errorMessage = signal('');
  activeQuickUser = signal<string | null>(null);

  readonly quickAccounts: QuickAccount[] = [
    {
      role: 'ADMIN',
      title: 'Admin',
      name: 'Oybek Rustamov',
      username: 'admin',
      password: 'admin123',
      icon: '👑',
      badgeClass: 'badge-admin',
      description: 'Super Administrator'
    },
    {
      role: 'WAITER',
      title: 'Ofitsiant 1',
      name: 'waiter1',
      username: 'waiter1',
      password: 'waiter123',
      icon: '🍽️',
      badgeClass: 'badge-waiter',
      description: 'Ofitsiant 1 (waiter1)'
    },
    {
      role: 'WAITER',
      title: 'Ofitsiant 2',
      name: 'waiter2',
      username: 'waiter2',
      password: 'waiter123',
      icon: '🍽️',
      badgeClass: 'badge-waiter',
      description: 'Ofitsiant 2 (waiter2)'
    },
    {
      role: 'KITCHEN',
      title: 'Pitsaxona',
      name: 'pizza',
      username: 'pizza',
      password: 'pizza123',
      icon: '🍕',
      badgeClass: 'badge-kitchen',
      description: 'Pitsaxona stansiyasi'
    },
    {
      role: 'KITCHEN',
      title: 'Somsapaz',
      name: 'somsa',
      username: 'somsa',
      password: 'somsa123',
      icon: '🥟',
      badgeClass: 'badge-kitchen',
      description: 'Somsapaz stansiyasi'
    }
  ];

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notify: NotificationService
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(4)]]
    });
  }

  quickLogin(acc: QuickAccount): void {
    this.activeQuickUser.set(acc.username);
    this.loginForm.patchValue({
      username: acc.username,
      password: acc.password
    });
    this.onSubmit();
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.activeQuickUser.set(null);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.activeQuickUser.set(null);
        if (response.success) {
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || this.authService.getDefaultRoute();
          this.router.navigateByUrl(returnUrl);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.activeQuickUser.set(null);
        const msg = err.error?.message || 'Invalid credentials. Please try again.';
        this.errorMessage.set(msg);
      }
    });
  }
}
