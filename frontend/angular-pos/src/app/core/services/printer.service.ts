import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface Printer {
  id: string;
  name: string;
  model?: string;
  connectionType: 'WINDOWS' | 'USB' | 'NETWORK' | 'TCPIP';
  ipAddress?: string;
  port?: number;
  windowsPrinterName?: string;
  paperWidth: number;
  characterEncoding: string;
  purpose: 'KITCHEN' | 'CASHIER' | 'RECEIPT' | 'BAR' | 'OTHER';
  status: 'ONLINE' | 'OFFLINE' | 'ERROR' | 'UNKNOWN';
  active: boolean;
  isDefault: boolean;
  autoPrint: boolean;
  fallbackPrinterId?: string;
  fallbackPrinterName?: string;
  assignedKitchenId?: string;
  assignedKitchenName?: string;
  isPrimaryForKitchen?: boolean;
  lastCheckedAt?: string;
  lastSuccessfulPrintAt?: string;
  lastError?: string;
  createdAt: string;
}

export interface CreatePrinterRequest {
  name: string;
  model?: string;
  connectionType: string;
  ipAddress?: string;
  port?: number;
  windowsPrinterName?: string;
  paperWidth?: number;
  characterEncoding?: string;
  purpose: string;
  autoPrint?: boolean;
  isDefault?: boolean;
  isPrimary?: boolean;
  fallbackPrinterId?: string;
  kitchenId?: string;
}

export interface UpdatePrinterRequest {
  name?: string;
  model?: string;
  connectionType?: string;
  ipAddress?: string;
  port?: number;
  windowsPrinterName?: string;
  paperWidth?: number;
  characterEncoding?: string;
  purpose?: string;
  status?: string;
  active?: boolean;
  autoPrint?: boolean;
  isDefault?: boolean;
  isPrimary?: boolean;
  fallbackPrinterId?: string;
  kitchenId?: string;
}

export interface TestPrintResult {
  success: boolean;
  message: string;
  printerName: string;
  connectionType: string;
  target: string;
  errorDetails?: string;
  testedAt: string;
}

export interface ExtendedKitchenStation {
  id: string;
  name: string;
  code: string;
  description?: string;
  sortOrder: number;
  active: boolean;
  color?: string;
  autoPrint?: boolean;
  soundNotification?: boolean;
  preparationTimeMinutes?: number;
  printerId?: string;
  printerName?: string;
  printerStatus?: string;
}

@Injectable({ providedIn: 'root' })
export class PrinterService {
  private readonly API = `${environment.apiUrl}/printers`;

  constructor(private http: HttpClient) {}

  getPrinters(): Observable<ApiResponse<Printer[]>> {
    return this.http.get<ApiResponse<Printer[]>>(this.API);
  }

  getPrinterById(id: string): Observable<ApiResponse<Printer>> {
    return this.http.get<ApiResponse<Printer>>(`${this.API}/${id}`);
  }

  createPrinter(request: CreatePrinterRequest): Observable<ApiResponse<Printer>> {
    return this.http.post<ApiResponse<Printer>>(this.API, request);
  }

  updatePrinter(id: string, request: UpdatePrinterRequest): Observable<ApiResponse<Printer>> {
    return this.http.put<ApiResponse<Printer>>(`${this.API}/${id}`, request);
  }

  deletePrinter(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }

  testPrint(id: string): Observable<ApiResponse<TestPrintResult>> {
    return this.http.post<ApiResponse<TestPrintResult>>(`${this.API}/${id}/test`, {});
  }

  reprintKitchenTicket(ticketId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API}/reprint/kitchen-ticket/${ticketId}`, {});
  }

  reprintOrderReceipt(orderId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.API}/reprint/order-receipt/${orderId}`, {});
  }

  getKitchenStations(): Observable<ApiResponse<ExtendedKitchenStation[]>> {
    return this.http.get<ApiResponse<ExtendedKitchenStation[]>>(`${environment.apiUrl}/kitchens`);
  }

  updateKitchenStation(id: string, data: any): Observable<ApiResponse<ExtendedKitchenStation>> {
    return this.http.put<ApiResponse<ExtendedKitchenStation>>(`${environment.apiUrl}/kitchens/${id}`, data);
  }
}
