import { Component, signal } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

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
              @if (loading()) {
                <span class="spinner"></span>
                Signing in...
              } @else {
                Sign In
              }
            </button>
          </form>
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
      max-width: 420px;
      padding: 24px;
      position: relative;
      z-index: 1;
    }

    .login-header {
      text-align: center;
      margin-bottom: 32px;
    }

    .login-logo {
      font-size: 56px;
      margin-bottom: 12px;
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
      padding: 32px;
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
        margin-bottom: 28px;
      }
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
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

    .login-footer {
      text-align: center;
      margin-top: 24px;
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

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.login(this.loginForm.value).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.success) {
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || this.authService.getDefaultRoute();
          this.router.navigateByUrl(returnUrl);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err.error?.message || 'Invalid credentials. Please try again.';
        this.errorMessage.set(msg);
      }
    });
  }
}
