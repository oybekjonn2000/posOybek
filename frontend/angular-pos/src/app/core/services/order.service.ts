import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface OrderItem {
  id?: string;
  productId: string;
  kitchenId?: string;
  kitchenName?: string;
  productName: string;
  unitPrice?: number;
  productPrice?: number;
  quantity: number;
  subtotal: number;
  notes?: string;
  kitchenStatus?: string;
  status?: string;
  voided?: boolean;
  voidReason?: string;
  voidedAt?: string;
  voidedByName?: string;
}

export interface Order {
  id: string;
  orderNumber: string;
  orderType?: string;
  status: string;
  tableId?: string;
  tableNumber?: string;
  tableName?: string;
  waiterId?: string;
  waiterName?: string;
  guestCount?: number;
  subtotal: number;
  taxAmount?: number;
  serviceCharge?: number;
  discountAmount?: number;
  discountPercent?: number;
  total: number;
  totalAmount?: number;
  paidAmount?: number;
  notes?: string;
  kitchenNotes?: string;
  openedAt?: string;
  createdAt?: string;
  sentToKitchenAt?: string;
  readyAt?: string;
  paidAt?: string;
  items: OrderItem[];
}

export interface CreateOrderItemRequest {
  productId: string;
  quantity: number;
  notes?: string;
}

export interface CreateOrderRequest {
  tableId?: string;
  orderType?: string;
  guestCount?: number;
  notes?: string;
  items: CreateOrderItemRequest[];
}

export interface AddItemsRequest {
  items: CreateOrderItemRequest[];
}

export interface CancelItemRequest {
  reason: string;
  quantity?: number;
}

export interface CancelOrderRequest {
  reason: string;
}

export interface CancellationReceipt {
  id: string;
  receiptNumber: string;
  orderId: string;
  orderNumber: string;
  tableId?: string;
  tableName?: string;
  cancelledById?: string;
  cancelledByName?: string;
  reason: string;
  itemId?: string;
  itemName?: string;
  cancelledQuantity?: number;
  unitPrice?: number;
  totalAmount: number;
  fullOrder: boolean;
  createdAt: string;
}

export interface CancellationResult {
  order: Order;
  receipt: CancellationReceipt;
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly API = `${environment.apiUrl}/orders`;

  constructor(private http: HttpClient) {}

  getActiveOrders(): Observable<ApiResponse<Order[]>> {
    return this.http.get<ApiResponse<Order[]>>(this.API);
  }

  getOrderById(id: string): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${this.API}/${id}`);
  }

  createOrder(request: CreateOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(this.API, request);
  }

  addItems(orderId: string, request: AddItemsRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${this.API}/${orderId}/items`, request);
  }

  voidItem(orderId: string, itemId: string, reason: string): Observable<ApiResponse<Order>> {
    return this.http.request<ApiResponse<Order>>('delete', `${this.API}/${orderId}/items/${itemId}`, {
      body: { reason }
    });
  }

  cancelItem(orderId: string, itemId: string, request: CancelItemRequest): Observable<ApiResponse<CancellationResult>> {
    return this.http.post<ApiResponse<CancellationResult>>(`${this.API}/${orderId}/items/${itemId}/cancel`, request);
  }

  cancelOrder(orderId: string, request: CancelOrderRequest): Observable<ApiResponse<CancellationResult>> {
    return this.http.post<ApiResponse<CancellationResult>>(`${this.API}/${orderId}/cancel`, request);
  }

  getCancellationReceipts(orderId: string): Observable<ApiResponse<CancellationReceipt[]>> {
    return this.http.get<ApiResponse<CancellationReceipt[]>>(`${this.API}/${orderId}/cancellation-receipts`);
  }

  applyDiscount(orderId: string, discountType: string, discountValue: number, reason?: string): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.API}/${orderId}/discount`, {
      discountType,
      discountValue,
      reason
    });
  }

  updateStatus(orderId: string, status: string): Observable<ApiResponse<Order>> {
    return this.http.put<ApiResponse<Order>>(`${this.API}/${orderId}/status`, { status });
  }
}
