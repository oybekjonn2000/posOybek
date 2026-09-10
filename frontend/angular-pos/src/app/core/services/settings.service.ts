import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface RestaurantSettings {
  name: string;
  logoUrl?: string;
  address?: string;
  city?: string;
  country?: string;
  phone?: string;
  email?: string;
  website?: string;
  taxNumber?: string;
  currency: string;
  currencySymbol?: string;
  timezone: string;
  language?: string;
  workingHours?: string;
  description?: string;
}

export interface GeneralSettings {
  defaultCurrency: string;
  dateFormat: string;
  timeFormat: string;
  language: string;
  theme: string;
  autoSave: boolean;
  autoRefresh: boolean;
  realTimeUpdates: boolean;
  confirmBeforeDelete: boolean;
  confirmBeforeCancelOrder: boolean;
  confirmBeforePayment: boolean;
  soundNotifications: boolean;
}

export interface ReceiptSettings {
  restaurantName?: string;
  logoUrl?: string;
  address?: string;
  phone?: string;
  header: string;
  footer: string;
  showWaiter: boolean;
  showCashier: boolean;
  showTable: boolean;
  showOrderNumber: boolean;
  showDateTime: boolean;
  showPaymentMethod: boolean;
  showDiscount: boolean;
  showServiceCharge: boolean;
  showTax: boolean;
  paperWidth: number;
  numberOfCopies: number;
}

export interface PaymentSettings {
  cashEnabled: boolean;
  cardEnabled: boolean;
  clickEnabled: boolean;
  paymeEnabled: boolean;
  otherEnabled: boolean;
  defaultPaymentMethod: string;
  allowMixedPayment: boolean;
  requirePaymentConfirmation: boolean;
  autoPrintReceiptAfterPayment: boolean;
}

export interface TaxServiceSettings {
  serviceChargeEnabled: boolean;
  serviceChargePercent: number;
  taxEnabled: boolean;
  taxPercent: number;
}

export interface OrderSettings {
  autoOrderNumber: boolean;
  orderNumberPrefix: string;
  allowOrderEditing: boolean;
  allowItemCancellation: boolean;
  allowQuantityEditing: boolean;
  requireCancellationReason: boolean;
  requireManagerApproval: boolean;
  autoSendToKitchen: boolean;
  allowSplitBill: boolean;
  allowMergeOrders: boolean;
  allowReopenOrder: boolean;
}

export interface KitchenSettings {
  autoPrintKitchenOrder: boolean;
  soundNotificationOnNewTicket: boolean;
  autoAcceptOrders: boolean;
  ticketFontSize: string;
  showWaiterOnTicket: boolean;
  showTableOnTicket: boolean;
  showNotesOnTicket: boolean;
}

export interface NotificationSettings {
  soundEnabled: boolean;
  soundVolume: number;
  newOrderSound: string;
  itemReadySound: string;
  lowStockAlert: boolean;
}

export interface SecuritySettings {
  requirePinForCashier: boolean;
  autoLogoutMinutes: number;
  sessionTimeoutMinutes: number;
  maxLoginAttempts: number;
}

export interface BackupSettings {
  backupLocation: string;
  autoBackupEnabled: boolean;
  backupFrequency: string;
  lastBackupTime: string;
  dbStatus: string;
  dbSize: string;
}

export interface SystemInfoDto {
  appName: string;
  appVersion: string;
  backendStatus: string;
  databaseStatus: string;
  postgresVersion: string;
  serverTime: string;
  frontendVersion: string;
  webSocketStatus: string;
  printerServiceStatus: string;
  totalMemoryMb: number;
  freeMemoryMb: number;
  totalPrinters: number;
  onlinePrinters: number;
}

export interface AuditLogEntry {
  id: string;
  action: string;
  entityType: string;
  entityId?: string;
  oldValue?: string;
  newValue?: string;
  notes?: string;
  userName?: string;
  createdAt: string;
}

export interface AllSettingsResponse {
  restaurant: RestaurantSettings;
  general: GeneralSettings;
  receipt: ReceiptSettings;
  payments: PaymentSettings;
  taxService: TaxServiceSettings;
  orders: OrderSettings;
  kitchen: KitchenSettings;
  notifications: NotificationSettings;
  security: SecuritySettings;
  backup: BackupSettings;
  rawSettings: Record<string, any>;
}

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly API = `${environment.apiUrl}/settings`;

  constructor(private http: HttpClient) {}

  getAllSettings(): Observable<ApiResponse<AllSettingsResponse>> {
    return this.http.get<ApiResponse<AllSettingsResponse>>(this.API);
  }

  updateRestaurantSettings(settings: RestaurantSettings): Observable<ApiResponse<RestaurantSettings>> {
    return this.http.put<ApiResponse<RestaurantSettings>>(`${this.API}/restaurant`, settings);
  }

  updateCategorySettings(category: string, values: Record<string, any>): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.API}/${category.toLowerCase()}`, values);
  }

  getSystemInfo(): Observable<ApiResponse<SystemInfoDto>> {
    return this.http.get<ApiResponse<SystemInfoDto>>(`${this.API}/system/info`);
  }

  getAuditLogs(limit: number = 20): Observable<ApiResponse<AuditLogEntry[]>> {
    return this.http.get<ApiResponse<AuditLogEntry[]>>(`${this.API}/audit-logs?limit=${limit}`);
  }

  triggerBackup(): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.API}/backup/trigger`, {});
  }
}
