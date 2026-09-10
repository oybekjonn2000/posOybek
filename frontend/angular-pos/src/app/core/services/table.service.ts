import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface RestaurantTable {
  id: string;
  zoneId?: string;
  zoneName?: string;
  tableNumber: string;
  name: string;
  capacity: number;
  shape: string;
  posX: number;
  posY: number;
  width: number;
  height: number;
  status: 'FREE' | 'OCCUPIED' | 'RESERVED' | 'BILL_REQUESTED' | 'CLEANING';
  currentOrderId?: string;
  activeOrderNumber?: string;
  itemCount?: number;
  totalAmount?: number;
  active: boolean;
}

export interface TableZone {
  id: string;
  name: string;
  description?: string;
  sortOrder: number;
  active: boolean;
}

export interface CreateTableRequest {
  zoneId?: string;
  zoneName?: string;
  tableNumber: string;
  name?: string;
  capacity: number;
  shape?: string;
  posX?: number;
  posY?: number;
  width?: number;
  height?: number;
}

@Injectable({ providedIn: 'root' })
export class TableService {
  private readonly API = `${environment.apiUrl}/tables`;

  constructor(private http: HttpClient) {}

  getTables(zoneId?: string): Observable<ApiResponse<RestaurantTable[]>> {
    let params = new HttpParams();
    if (zoneId) params = params.set('zoneId', zoneId);
    return this.http.get<ApiResponse<RestaurantTable[]>>(this.API, { params });
  }

  getZones(): Observable<ApiResponse<TableZone[]>> {
    return this.http.get<ApiResponse<TableZone[]>>(`${this.API}/zones`);
  }

  createZone(request: { name: string; description?: string; sortOrder?: number }): Observable<ApiResponse<TableZone>> {
    return this.http.post<ApiResponse<TableZone>>(`${this.API}/zones`, request);
  }

  getTableById(id: string): Observable<ApiResponse<RestaurantTable>> {
    return this.http.get<ApiResponse<RestaurantTable>>(`${this.API}/${id}`);
  }

  updateTableStatus(id: string, status: string, currentOrderId?: string): Observable<ApiResponse<RestaurantTable>> {
    return this.http.put<ApiResponse<RestaurantTable>>(`${this.API}/${id}/status`, { status, currentOrderId });
  }

  createTable(request: CreateTableRequest): Observable<ApiResponse<RestaurantTable>> {
    return this.http.post<ApiResponse<RestaurantTable>>(this.API, request);
  }

  updateTable(id: string, request: Partial<CreateTableRequest>): Observable<ApiResponse<RestaurantTable>> {
    return this.http.put<ApiResponse<RestaurantTable>>(`${this.API}/${id}`, request);
  }

  deleteTable(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }
}
