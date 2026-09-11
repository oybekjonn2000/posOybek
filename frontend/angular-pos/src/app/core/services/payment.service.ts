import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface PaymentProcessRequest {
  orderId: string;
  paymentMethod: 'CASH' | 'CARD' | 'TRANSFER' | 'MIXED' | string;
  amount: number;
  cashAmount?: number;
  cardAmount?: number;
  changeAmount?: number;
  cashReceived?: number;
  changeGiven?: number;
  referenceNumber?: string;
  notes?: string;
}

export interface PaymentResponse {
  id: string;
  orderId: string;
  paymentNumber: string;
  paymentMethod: string;
  status: string;
  amount: number;
  cashReceived?: number;
  changeGiven?: number;
  processedAt: string;
  receiptPrintStatus?: string;
  receiptPrintError?: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly API = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) {}

  processPayment(request: PaymentProcessRequest): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(this.API, request);
  }

  getPaymentsByOrder(orderId: string): Observable<ApiResponse<PaymentResponse[]>> {
    return this.http.get<ApiResponse<PaymentResponse[]>>(`${this.API}/order/${orderId}`);
  }

  refundPayment(paymentId: string, reason: string): Observable<ApiResponse<PaymentResponse>> {
    return this.http.post<ApiResponse<PaymentResponse>>(`${this.API}/${paymentId}/refund`, { reason });
  }
}
