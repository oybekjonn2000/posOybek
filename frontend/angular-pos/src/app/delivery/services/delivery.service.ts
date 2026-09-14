import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../core/services/auth.service';
import {
  DeliveryProvider,
  SaveProviderRequest,
  DeliveryOrder,
  ProductMapping,
  CategoryMapping,
  DeliveryDashboardMetrics,
  DeliveryIntegrationLog,
  ConnectionTestResult
} from '../models/delivery.models';

@Injectable({ providedIn: 'root' })
export class DeliveryService {
  private readonly API = `${environment.apiUrl}/delivery`;

  constructor(private http: HttpClient) {}

  // ===================== PROVIDERS =====================

  getProviders(): Observable<ApiResponse<DeliveryProvider[]>> {
    return this.http.get<ApiResponse<DeliveryProvider[]>>(`${this.API}/providers`);
  }

  getProvider(id: string): Observable<ApiResponse<DeliveryProvider>> {
    return this.http.get<ApiResponse<DeliveryProvider>>(`${this.API}/providers/${id}`);
  }

  createProvider(request: SaveProviderRequest): Observable<ApiResponse<DeliveryProvider>> {
    return this.http.post<ApiResponse<DeliveryProvider>>(`${this.API}/providers`, request);
  }

  updateProvider(id: string, request: Partial<SaveProviderRequest>): Observable<ApiResponse<DeliveryProvider>> {
    return this.http.put<ApiResponse<DeliveryProvider>>(`${this.API}/providers/${id}`, request);
  }

  deleteProvider(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/providers/${id}`);
  }

  testConnection(id: string): Observable<ApiResponse<ConnectionTestResult>> {
    return this.http.post<ApiResponse<ConnectionTestResult>>(`${this.API}/providers/${id}/test`, {});
  }

  connectProvider(id: string): Observable<ApiResponse<DeliveryProvider>> {
    return this.http.post<ApiResponse<DeliveryProvider>>(`${this.API}/providers/${id}/connect`, {});
  }

  disconnectProvider(id: string): Observable<ApiResponse<DeliveryProvider>> {
    return this.http.post<ApiResponse<DeliveryProvider>>(`${this.API}/providers/${id}/disconnect`, {});
  }

  syncProducts(id: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.API}/providers/${id}/sync`, {});
  }

  // ===================== ORDERS =====================

  getOrders(params: {
    providerId?: string;
    status?: string;
    paymentType?: string;
    search?: string;
    page?: number;
    size?: number;
  }): Observable<ApiResponse<DeliveryOrder[]>> {
    let httpParams = new HttpParams();
    if (params.providerId) httpParams = httpParams.set('providerId', params.providerId);
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.paymentType) httpParams = httpParams.set('paymentType', params.paymentType);
    if (params.search) httpParams = httpParams.set('search', params.search);
    if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
    if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());

    return this.http.get<ApiResponse<DeliveryOrder[]>>(`${this.API}/orders`, { params: httpParams });
  }

  getOrder(id: string): Observable<ApiResponse<DeliveryOrder>> {
    return this.http.get<ApiResponse<DeliveryOrder>>(`${this.API}/orders/${id}`);
  }

  acceptOrder(id: string, preparationMinutes = 20): Observable<ApiResponse<DeliveryOrder>> {
    return this.http.post<ApiResponse<DeliveryOrder>>(`${this.API}/orders/${id}/accept`, { preparationMinutes });
  }

  rejectOrder(id: string, reason: string): Observable<ApiResponse<DeliveryOrder>> {
    return this.http.post<ApiResponse<DeliveryOrder>>(`${this.API}/orders/${id}/reject`, { reason });
  }

  cancelOrder(id: string, reason: string): Observable<ApiResponse<DeliveryOrder>> {
    return this.http.post<ApiResponse<DeliveryOrder>>(`${this.API}/orders/${id}/cancel`, { reason });
  }

  mapOrderProduct(orderId: string, externalProductId: string, posProductId: string): Observable<ApiResponse<DeliveryOrder>> {
    return this.http.post<ApiResponse<DeliveryOrder>>(`${this.API}/orders/${orderId}/map-item`, {
      externalProductId,
      posProductId
    });
  }

  // ===================== MAPPINGS =====================

  getProductMappings(page = 0, size = 50): Observable<ApiResponse<ProductMapping[]>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<ProductMapping[]>>(`${this.API}/mappings/products`, { params });
  }

  saveProductMapping(request: {
    providerId: string;
    externalProductId: string;
    externalProductName?: string;
    externalProductPrice?: number;
    posProductId: string;
  }): Observable<ApiResponse<ProductMapping>> {
    return this.http.post<ApiResponse<ProductMapping>>(`${this.API}/mappings/products`, request);
  }

  deleteProductMapping(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/mappings/products/${id}`);
  }

  getCategoryMappings(page = 0, size = 50): Observable<ApiResponse<CategoryMapping[]>> {
    const params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    return this.http.get<ApiResponse<CategoryMapping[]>>(`${this.API}/mappings/categories`, { params });
  }

  saveCategoryMapping(request: {
    providerId: string;
    externalCategoryId: string;
    externalCategoryName?: string;
    posCategoryId: string;
  }): Observable<ApiResponse<CategoryMapping>> {
    return this.http.post<ApiResponse<CategoryMapping>>(`${this.API}/mappings/categories`, request);
  }

  deleteCategoryMapping(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/mappings/categories/${id}`);
  }

  // ===================== DASHBOARD & LOGS =====================

  getDashboard(): Observable<ApiResponse<DeliveryDashboardMetrics>> {
    return this.http.get<ApiResponse<DeliveryDashboardMetrics>>(`${this.API}/dashboard`);
  }

  getLogs(page = 0, size = 25, providerId?: string): Observable<ApiResponse<DeliveryIntegrationLog[]>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (providerId) params = params.set('providerId', providerId);
    return this.http.get<ApiResponse<DeliveryIntegrationLog[]>>(`${this.API}/logs`, { params });
  }
}
