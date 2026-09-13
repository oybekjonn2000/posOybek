import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, catchError, of } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface LoginRequest {
  username: string;
  password: string;
  deviceId?: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface UserInfo {
  id: string;
  username: string;
  fullName: string;
  tenantId: string;
  role?: string;
  kitchenId?: string;
  permissions: string[];
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  page?: PageMeta;
  message: string | null;
  errorCode: string | null;
  timestamp: string;
}

/**
 * Core authentication service using Angular Signals.
 * Manages JWT tokens, user state, and permissions.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = `${environment.apiUrl}/auth`;

  // Signals for reactive state
  private _user = signal<UserInfo | null>(this.loadUser());
  private _accessToken = signal<string | null>(this.loadToken());

  readonly user = this._user.asReadonly();
  readonly accessToken = this._accessToken.asReadonly();
  readonly isAuthenticated = computed(() => !!this._user() && !!this._accessToken());
  readonly permissions = computed(() => new Set(this._user()?.permissions ?? []));
  readonly isAdmin = computed(() => {
    const role = (this._user()?.role || '').toUpperCase();
    return role === 'ADMIN' || this._user()?.username === 'admin';
  });

  isAdminUser(): boolean {
    return this.isAdmin();
  }

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<ApiResponse<TokenResponse>> {
    return this.http.post<ApiResponse<TokenResponse>>(`${this.API}/login`, request).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      })
    );
  }

  refreshToken(): Observable<ApiResponse<TokenResponse> | null> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return of(null);

    return this.http.post<ApiResponse<TokenResponse>>(`${this.API}/refresh`, { refreshToken }).pipe(
      tap(response => {
        if (response.success && response.data) {
          this.storeTokens(response.data);
        }
      }),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    this._user.set(null);
    this._accessToken.set(null);
    this.router.navigate(['/auth/login']);
  }

  hasPermission(permission: string): boolean {
    if (this.isAdmin()) return true;
    return this.permissions().has(permission);
  }

  hasAnyPermission(perms: string[]): boolean {
    if (this.isAdmin()) return true;
    return perms.some(p => this.permissions().has(p));
  }

  getCurrentUser(): UserInfo | null {
    return this._user();
  }

  getDefaultRoute(): string {
    const role = (this._user()?.role || '').toUpperCase();
    if (role === 'WAITER') return '/tables';
    if (role === 'KITCHEN') return '/kitchen';
    if (role === 'CASHIER') return '/orders';
    return '/dashboard';
  }

  private storeTokens(data: TokenResponse): void {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data.user));
    this._accessToken.set(data.accessToken);
    this._user.set(data.user);
  }

  private loadToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  private loadUser(): UserInfo | null {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  }
}
