export type ProviderStatus = 'ACTIVE' | 'INACTIVE' | 'ERROR' | 'DISABLED';
export type ConnectionStatus = 'CONNECTED' | 'DISCONNECTED' | 'ERROR' | 'OFFLINE';
export type CommissionType = 'PERCENTAGE' | 'FIXED' | 'PROVIDER_REPORTED';

export type DeliveryOrderStatus = 
  | 'NEW'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'PREPARING'
  | 'READY'
  | 'COURIER_ASSIGNED'
  | 'PICKED_UP'
  | 'DELIVERING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'FAILED'
  | 'MAPPING_REQUIRED';

export type MappingStatus = 'MAPPED' | 'UNMAPPED' | 'AUTO_MAPPED';

export interface DeliveryProvider {
  id: string;
  name: string;
  code: string;
  type: string;
  status: ProviderStatus;
  connectionStatus: ConnectionStatus;
  apiBaseUrl?: string;
  restaurantId?: string;
  hasApiKey?: boolean;
  hasClientId?: boolean;
  hasSecret?: boolean;
  hasWebhookSecret?: boolean;
  apiKeyMasked?: string;
  webhookSecretMasked?: string;
  webhookUrl?: string;
  autoAcceptOrders?: boolean;
  autoPrintKitchenReceipt?: boolean;
  autoPrintCustomerReceipt?: boolean;
  soundNotification?: boolean;
  autoSync?: boolean;
  syncIntervalMinutes?: number;
  orderTimeoutMinutes?: number;
  defaultPaymentType?: string;
  defaultOrderSource?: string;
  commissionType?: CommissionType;
  commissionValue?: number;
  lastSyncAt?: string;
  lastConnectionTestAt?: string;
  lastErrorMessage?: string;
  totalOrdersCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface SaveProviderRequest {
  name: string;
  code: string;
  type: string;
  status?: ProviderStatus;
  apiBaseUrl?: string;
  restaurantId?: string;
  apiKey?: string;
  clientId?: string;
  secret?: string;
  webhookSecret?: string;
  autoAcceptOrders?: boolean;
  autoPrintKitchenReceipt?: boolean;
  autoPrintCustomerReceipt?: boolean;
  soundNotification?: boolean;
  autoSync?: boolean;
  syncIntervalMinutes?: number;
  orderTimeoutMinutes?: number;
  defaultPaymentType?: string;
  defaultOrderSource?: string;
  commissionType?: CommissionType;
  commissionValue?: number;
}

export interface DeliveryOrderItem {
  id: string;
  externalProductId: string;
  posProductId?: string;
  name: string;
  quantity: number;
  unitPrice: number;
  total: number;
  mappingStatus: MappingStatus;
  notes?: string;
  modifiersJson?: string;
}

export interface DeliveryOrder {
  id: string;
  providerId: string;
  providerName?: string;
  providerCode?: string;
  externalOrderId: string;
  posOrderId?: string;
  posOrderNumber?: string;
  status: DeliveryOrderStatus;
  providerStatus?: string;
  customerName?: string;
  customerPhone?: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  subtotal: number;
  deliveryFee: number;
  commission: number;
  discount: number;
  total: number;
  paymentType?: string;
  paymentStatus?: string;
  courierName?: string;
  courierPhone?: string;
  courierVehicle?: string;
  courierStatus?: string;
  estimatedDeliveryTime?: string;
  cancellationReason?: string;
  errorMessage?: string;
  retryCount?: number;
  items?: DeliveryOrderItem[];
  createdAt: string;
  updatedAt?: string;
}

export interface ProductMapping {
  id: string;
  providerId: string;
  providerName?: string;
  externalProductId: string;
  externalProductName: string;
  externalProductPrice?: number;
  posProductId: string;
  posProductName?: string;
  posProductPrice?: number;
  autoMapped: boolean;
  lastSyncedAt?: string;
}

export interface CategoryMapping {
  id: string;
  providerId: string;
  providerName?: string;
  externalCategoryId: string;
  externalCategoryName: string;
  posCategoryId: string;
  posCategoryName?: string;
  lastSyncedAt?: string;
}

export interface DeliveryDashboardMetrics {
  todayOrders: number;
  activeOrders: number;
  deliveredOrders: number;
  cancelledOrders: number;
  failedOrders: number;
  mappingRequiredOrders: number;
  grossSales: number;
  deliveryFees: number;
  commission: number;
  netRevenue: number;
}

export interface DeliveryIntegrationLog {
  id: string;
  providerId?: string;
  providerName?: string;
  action: string;
  externalId?: string;
  status: string;
  errorMessage?: string;
  durationMs?: number;
  createdAt: string;
}

export interface ConnectionTestResult {
  success: boolean;
  message: string;
  statusCode?: number;
  latencyMs?: number;
}
