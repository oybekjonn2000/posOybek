import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface InventoryDashboardStats {
  totalProducts: number;
  totalStockQuantity: number;
  lowStockCount: number;
  outOfStockCount: number;
  todayIncomingAmount: number;
  todayOutgoingAmount: number;
  todaySalesConsumptionAmount: number;
  warehouseValuation: number;
  recentDiscrepancyAmount: number;
}

export interface InventoryItem {
  id: string;
  name: string;
  sku?: string;
  unit: string;
  quantity: number;
  minQuantity: number;
  maxQuantity?: number;
  costPrice?: number;
  sellingPrice?: number;
  category?: string;
  lowStock: boolean;
  outOfStock: boolean;
  warehouseId?: string;
  warehouseName?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateInventoryItemRequest {
  name: string;
  sku?: string;
  unit: string;
  quantity?: number;
  minQuantity?: number;
  maxQuantity?: number;
  costPrice?: number;
  sellingPrice?: number;
  category?: string;
  notes?: string;
  warehouseId?: string;
}

export interface AdjustStockRequest {
  quantity: number;
  type: string; // ADJUSTMENT, WASTE, OUT, IN
  notes?: string;
  unitCost?: number;
  warehouseId?: string;
}

export interface OutboundRequest {
  itemId: string;
  warehouseId?: string;
  quantity: number;
  reason: string; // Oshxona uchun, Buzilgan, Yaroqlilik muddati tugagan, Ichki foydalanish, Manual
  type: string;   // OUT or WASTE
  notes?: string;
}

export interface InventoryTransaction {
  id: string;
  itemId: string;
  itemName: string;
  unit: string;
  warehouseId?: string;
  warehouseName?: string;
  type: string; // PURCHASE, IN, OUT, SALE, ADJUSTMENT, WASTE, TRANSFER, RETURN
  quantity: number;
  quantityBefore: number;
  quantityAfter: number;
  unitCost?: number;
  totalCost?: number;
  referenceType?: string;
  referenceId?: string;
  referenceNumber?: string;
  notes?: string;
  userName?: string;
  createdAt: string;
}

export interface PurchaseItemRequest {
  itemId: string;
  quantity: number;
  unitCost?: number;
  notes?: string;
}

export interface PurchaseCreateRequest {
  supplierId: string;
  warehouseId?: string;
  invoiceNumber?: string;
  purchaseDate?: string;
  notes?: string;
  paidAmount?: number;
  items: PurchaseItemRequest[];
}

export interface PurchaseResponse {
  id: string;
  purchaseNumber: string;
  invoiceNumber?: string;
  supplierId?: string;
  supplierName?: string;
  warehouseId?: string;
  warehouseName?: string;
  purchaseDate?: string;
  status: string;
  totalAmount: number;
  paidAmount: number;
  balanceDue: number;
  notes?: string;
  items?: any[];
  createdAt: string;
}

export interface AuditItemResponse {
  id: string;
  itemId: string;
  itemName: string;
  sku?: string;
  unit: string;
  systemQuantity: number;
  actualQuantity: number;
  difference: number;
  unitCost: number;
  totalDifferenceCost: number;
  notes?: string;
}

export interface AuditResponse {
  id: string;
  auditNumber: string;
  warehouseId?: string;
  warehouseName?: string;
  title: string;
  status: string;
  totalDiscrepancyCost: number;
  conductedByName?: string;
  startedAt: string;
  completedAt?: string;
  notes?: string;
  items?: AuditItemResponse[];
}

export interface RecipeItem {
  id?: string;
  inventoryItemId: string;
  inventoryItemName?: string;
  quantity: number;
  unit: string;
  costPrice?: number;
  totalCost?: number;
}

export interface ProductRecipe {
  productId: string;
  productName: string;
  categoryName?: string;
  price?: number;
  recipeItems: RecipeItem[];
  totalCostPrice: number;
}

export interface Warehouse {
  id: string;
  name: string;
  description?: string;
  address?: string;
  active: boolean;
  createdAt: string;
}

export interface Supplier {
  id: string;
  name: string;
  contactPerson?: string;
  phone?: string;
  email?: string;
  address?: string;
  totalPurchases?: number;
  totalPaid?: number;
  balanceDue?: number;
  active: boolean;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly API = `${environment.apiUrl}/inventory`;

  constructor(private http: HttpClient) {}

  getDashboard(): Observable<ApiResponse<InventoryDashboardStats>> {
    return this.http.get<ApiResponse<InventoryDashboardStats>>(`${this.API}/dashboard`);
  }

  getItems(warehouseId?: string, category?: string, lowStock?: boolean): Observable<ApiResponse<InventoryItem[]>> {
    let params = new HttpParams();
    if (warehouseId) params = params.set('warehouseId', warehouseId);
    if (category) params = params.set('category', category);
    if (lowStock !== undefined) params = params.set('lowStock', lowStock.toString());
    return this.http.get<ApiResponse<InventoryItem[]>>(this.API, { params });
  }

  getItem(id: string): Observable<ApiResponse<InventoryItem>> {
    return this.http.get<ApiResponse<InventoryItem>>(`${this.API}/${id}`);
  }

  createItem(data: CreateInventoryItemRequest): Observable<ApiResponse<InventoryItem>> {
    return this.http.post<ApiResponse<InventoryItem>>(this.API, data);
  }

  updateItem(id: string, data: Partial<CreateInventoryItemRequest>): Observable<ApiResponse<InventoryItem>> {
    return this.http.put<ApiResponse<InventoryItem>>(`${this.API}/${id}`, data);
  }

  deleteItem(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }

  adjustStock(id: string, data: AdjustStockRequest): Observable<ApiResponse<InventoryItem>> {
    return this.http.patch<ApiResponse<InventoryItem>>(`${this.API}/${id}/adjust`, data);
  }

  recordOutbound(data: OutboundRequest): Observable<ApiResponse<InventoryItem>> {
    return this.http.post<ApiResponse<InventoryItem>>(`${this.API}/outbound`, data);
  }

  getTransactions(params?: { itemId?: string; type?: string; warehouseId?: string; dateFrom?: string; dateTo?: string; page?: number; size?: number }): Observable<ApiResponse<InventoryTransaction[]>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.itemId) httpParams = httpParams.set('itemId', params.itemId);
      if (params.type && params.type !== 'ALL') httpParams = httpParams.set('type', params.type);
      if (params.warehouseId) httpParams = httpParams.set('warehouseId', params.warehouseId);
      if (params.dateFrom) httpParams = httpParams.set('dateFrom', params.dateFrom);
      if (params.dateTo) httpParams = httpParams.set('dateTo', params.dateTo);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page.toString());
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size.toString());
    }
    return this.http.get<ApiResponse<InventoryTransaction[]>>(`${this.API}/transactions`, { params: httpParams });
  }

  getPurchases(): Observable<ApiResponse<PurchaseResponse[]>> {
    return this.http.get<ApiResponse<PurchaseResponse[]>>(`${this.API}/purchases`);
  }

  createPurchase(data: PurchaseCreateRequest): Observable<ApiResponse<PurchaseResponse>> {
    return this.http.post<ApiResponse<PurchaseResponse>>(`${this.API}/purchases`, data);
  }

  getAudits(): Observable<ApiResponse<AuditResponse[]>> {
    return this.http.get<ApiResponse<AuditResponse[]>>(`${this.API}/audits`);
  }

  getAudit(id: string): Observable<ApiResponse<AuditResponse>> {
    return this.http.get<ApiResponse<AuditResponse>>(`${this.API}/audits/${id}`);
  }

  startAudit(data?: { warehouseId?: string; title?: string; notes?: string }): Observable<ApiResponse<AuditResponse>> {
    return this.http.post<ApiResponse<AuditResponse>>(`${this.API}/audits/start`, data || {});
  }

  submitAudit(id: string, data: { items: { itemId: string; actualQuantity: number; notes?: string }[]; notes?: string }): Observable<ApiResponse<AuditResponse>> {
    return this.http.post<ApiResponse<AuditResponse>>(`${this.API}/audits/${id}/submit`, data);
  }

  getRecipes(): Observable<ApiResponse<ProductRecipe[]>> {
    return this.http.get<ApiResponse<ProductRecipe[]>>(`${this.API}/recipes`);
  }

  getRecipe(productId: string): Observable<ApiResponse<ProductRecipe>> {
    return this.http.get<ApiResponse<ProductRecipe>>(`${this.API}/recipes/product/${productId}`);
  }

  saveRecipe(data: { productId: string; items: { inventoryItemId: string; quantity: number; unit?: string }[] }): Observable<ApiResponse<ProductRecipe>> {
    return this.http.post<ApiResponse<ProductRecipe>>(`${this.API}/recipes`, data);
  }

  getWarehouses(): Observable<ApiResponse<Warehouse[]>> {
    return this.http.get<ApiResponse<Warehouse[]>>(`${this.API}/warehouses`);
  }

  createWarehouse(data: { name: string; description?: string }): Observable<ApiResponse<Warehouse>> {
    return this.http.post<ApiResponse<Warehouse>>(`${this.API}/warehouses`, data);
  }

  getSuppliers(): Observable<ApiResponse<Supplier[]>> {
    return this.http.get<ApiResponse<Supplier[]>>(`${this.API}/suppliers`);
  }

  createSupplier(data: { name: string; contactPerson?: string; phone?: string; email?: string; address?: string }): Observable<ApiResponse<Supplier>> {
    return this.http.post<ApiResponse<Supplier>>(`${this.API}/suppliers`, data);
  }
}
