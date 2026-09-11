import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface HourlySales {
  hour: number;
  orders: number;
  revenue: number;
}

export interface DailyTrend {
  date: string;
  orders: number;
  revenue: number;
}

export interface SalesSummary {
  totalSales: number;
  totalOrders: number;
  totalPayments: number;
  avgCheck: number;
  cashTotal: number;
  cardTotal: number;
  otherTotal: number;
  cancelledOrdersCount: number;
  refundedOrdersCount: number;
  refundedAmount: number;
  hourlySales: HourlySales[];
  dailyTrend: DailyTrend[];
}

export interface ProductSaleItem {
  productId: string;
  productName: string;
  categoryName: string;
  quantity: number;
  revenue: number;
  cost: number;
  profit: number;
  profitMargin: number;
}

export interface ProfitLoss {
  totalRevenue: number;
  totalProductCost: number;
  totalDiscounts: number;
  totalRefunds: number;
  grossProfit: number;
  profitMargin: number;
}

export interface CashierSummary {
  cashierId: string;
  cashierName: string;
  ordersCount: number;
  cashSales: number;
  cardSales: number;
  onlineSales: number;
  otherSales: number;
  totalSales: number;
  totalRefunds: number;
}

export interface WaiterPerformance {
  waiterId: string;
  waiterName: string;
  ordersCount: number;
  totalSales: number;
  avgCheck: number;
  deliveredCount: number;
  cancelledCount: number;
}

export interface KitchenPerformance {
  kitchenId: string;
  kitchenName: string;
  ordersCount: number;
  itemsPrepared: number;
  itemsCancelled: number;
  revenue: number;
}

export interface StockReportItem {
  itemId: string;
  itemName: string;
  unit: string;
  category: string;
  warehouseName: string;
  openingStock: number;
  incoming: number;
  outgoing: number;
  salesConsumption: number;
  waste: number;
  adjustment: number;
  closingStock: number;
  unitCost: number;
  totalValuation: number;
}

export interface ReportFilter {
  dateFrom?: string;
  dateTo?: string;
  waiterId?: string;
  kitchenId?: string;
  categoryId?: string;
  warehouseId?: string;
}

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly API = `${environment.apiUrl}/reports`;

  constructor(private http: HttpClient) {}

  private buildParams(filter?: ReportFilter): HttpParams {
    let params = new HttpParams();
    if (!filter) return params;
    if (filter.dateFrom) params = params.set('dateFrom', filter.dateFrom);
    if (filter.dateTo) params = params.set('dateTo', filter.dateTo);
    if (filter.waiterId) params = params.set('waiterId', filter.waiterId);
    if (filter.kitchenId) params = params.set('kitchenId', filter.kitchenId);
    if (filter.categoryId) params = params.set('categoryId', filter.categoryId);
    if (filter.warehouseId) params = params.set('warehouseId', filter.warehouseId);
    return params;
  }

  getSalesSummary(filter?: ReportFilter): Observable<ApiResponse<SalesSummary>> {
    return this.http.get<ApiResponse<SalesSummary>>(`${this.API}/sales`, { params: this.buildParams(filter) });
  }

  getProductSales(filter?: ReportFilter): Observable<ApiResponse<ProductSaleItem[]>> {
    return this.http.get<ApiResponse<ProductSaleItem[]>>(`${this.API}/products`, { params: this.buildParams(filter) });
  }

  getProfitLoss(filter?: ReportFilter): Observable<ApiResponse<ProfitLoss>> {
    return this.http.get<ApiResponse<ProfitLoss>>(`${this.API}/profit`, { params: this.buildParams(filter) });
  }

  getCashierReport(filter?: ReportFilter): Observable<ApiResponse<CashierSummary[]>> {
    return this.http.get<ApiResponse<CashierSummary[]>>(`${this.API}/cashier`, { params: this.buildParams(filter) });
  }

  getWaiterReport(filter?: ReportFilter): Observable<ApiResponse<WaiterPerformance[]>> {
    return this.http.get<ApiResponse<WaiterPerformance[]>>(`${this.API}/waiters`, { params: this.buildParams(filter) });
  }

  getKitchenReport(filter?: ReportFilter): Observable<ApiResponse<KitchenPerformance[]>> {
    return this.http.get<ApiResponse<KitchenPerformance[]>>(`${this.API}/kitchens`, { params: this.buildParams(filter) });
  }

  getStockReport(filter?: ReportFilter): Observable<ApiResponse<StockReportItem[]>> {
    return this.http.get<ApiResponse<StockReportItem[]>>(`${this.API}/stock`, { params: this.buildParams(filter) });
  }

  downloadCsv(reportType: string, filter?: ReportFilter): Observable<Blob> {
    let params = this.buildParams(filter).set('reportType', reportType);
    return this.http.get(`${this.API}/export/csv`, { params, responseType: 'blob' });
  }
}
