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
}

export interface CreateKitchenRequest {
  name: string;
  code: string;
  description?: string;
  sortOrder?: number;
}

@Injectable({ providedIn: 'root' })
export class KitchenService {
  private readonly API = `${environment.apiUrl}/kitchen`;

  constructor(private http: HttpClient) {}

  getKitchens(): Observable<ApiResponse<KitchenStation[]>> {
    return this.http.get<ApiResponse<KitchenStation[]>>(`${environment.apiUrl}/kitchens`);
  }

  createKitchen(request: CreateKitchenRequest): Observable<ApiResponse<KitchenStation>> {
    return this.http.post<ApiResponse<KitchenStation>>(`${environment.apiUrl}/kitchens`, request);
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
