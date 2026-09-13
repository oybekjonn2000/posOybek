import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';
import { Order } from './order.service';

export interface KitchenStation {
  id: string;
  name: string;
  code: string;
  description?: string;
  sortOrder?: number;
  active: boolean;
  color?: string;
  autoPrint?: boolean;
  soundNotification?: boolean;
  preparationTimeMinutes?: number;
  printerId?: string;
  printerName?: string;
  printerStatus?: string;
  assignedEmployeesCount?: number;
  assignedCategoriesCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateKitchenRequest {
  name: string;
  code?: string;
  description?: string;
  sortOrder?: number;
  active?: boolean;
  color?: string;
  autoPrint?: boolean;
  soundNotification?: boolean;
  preparationTimeMinutes?: number;
  printerId?: string;
}

export interface UpdateKitchenRequest {
  name?: string;
  code?: string;
  description?: string;
  sortOrder?: number;
  active?: boolean;
  color?: string;
  autoPrint?: boolean;
  soundNotification?: boolean;
  preparationTimeMinutes?: number;
  printerId?: string;
}

export interface AssignedEmployee {
  id: string;
  username: string;
  firstName: string;
  lastName?: string;
  fullName?: string;
  role?: string;
}

export interface AssignedCategory {
  id: string;
  name: string;
  nameUz?: string;
  nameRu?: string;
  active: boolean;
}

@Injectable({ providedIn: 'root' })
export class KitchenService {
  private readonly API = `${environment.apiUrl}/kitchen`;

  constructor(private http: HttpClient) {}

  getKitchens(search?: string, status?: string, page?: number, size?: number): Observable<ApiResponse<KitchenStation[]>> {
    let params = new HttpParams();
    if (search && search.trim()) params = params.set('search', search.trim());
    if (status && status !== 'ALL') params = params.set('status', status);
    if (page !== undefined && page !== null) params = params.set('page', page.toString());
    if (size !== undefined && size !== null) params = params.set('size', size.toString());
    return this.http.get<ApiResponse<KitchenStation[]>>(`${environment.apiUrl}/kitchens`, { params });
  }

  getActiveKitchens(): Observable<ApiResponse<KitchenStation[]>> {
    return this.http.get<ApiResponse<KitchenStation[]>>(`${environment.apiUrl}/kitchens/active`);
  }

  getKitchenById(id: string): Observable<ApiResponse<KitchenStation>> {
    return this.http.get<ApiResponse<KitchenStation>>(`${environment.apiUrl}/kitchens/${id}`);
  }

  createKitchen(request: CreateKitchenRequest): Observable<ApiResponse<KitchenStation>> {
    return this.http.post<ApiResponse<KitchenStation>>(`${environment.apiUrl}/kitchens`, request);
  }

  updateKitchen(id: string, request: UpdateKitchenRequest): Observable<ApiResponse<KitchenStation>> {
    return this.http.put<ApiResponse<KitchenStation>>(`${environment.apiUrl}/kitchens/${id}`, request);
  }

  toggleKitchenStatus(id: string, active: boolean): Observable<ApiResponse<KitchenStation>> {
    return this.http.patch<ApiResponse<KitchenStation>>(`${environment.apiUrl}/kitchens/${id}/status`, { active });
  }

  getKitchenEmployees(id: string): Observable<ApiResponse<AssignedEmployee[]>> {
    return this.http.get<ApiResponse<AssignedEmployee[]>>(`${environment.apiUrl}/kitchens/${id}/employees`);
  }

  assignKitchenEmployees(id: string, employeeIds: string[]): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${environment.apiUrl}/kitchens/${id}/employees`, { employeeIds });
  }

  getKitchenCategories(id: string): Observable<ApiResponse<AssignedCategory[]>> {
    return this.http.get<ApiResponse<AssignedCategory[]>>(`${environment.apiUrl}/kitchens/${id}/categories`);
  }

  getKitchenOrders(kitchenId?: string): Observable<ApiResponse<Order[]>> {
    let params = new HttpParams();
    if (kitchenId) {
      params = params.set('kitchenId', kitchenId);
    }
    return this.http.get<ApiResponse<Order[]>>(`${this.API}/orders`, { params });
  }

  updateItemStatus(itemId: string, status: 'NEW' | 'ACCEPTED' | 'COOKING' | 'READY' | 'SERVED'): Observable<ApiResponse<void>> {
    const params = new HttpParams().set('status', status);
    return this.http.put<ApiResponse<void>>(`${this.API}/items/${itemId}/status`, null, { params });
  }

  deleteKitchen(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${environment.apiUrl}/kitchens/${id}`);
  }
}

