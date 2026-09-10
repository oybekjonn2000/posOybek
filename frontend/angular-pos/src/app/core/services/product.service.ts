import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface Product {
  id: string;
  categoryId?: string;
  categoryName?: string;
  kitchenId?: string;
  kitchenName?: string;
  sku: string;
  barcode?: string;
  name: string;
  nameUz?: string;
  nameRu?: string;
  unit: string;
  purchasePrice?: number;
  salePrice: number;
  price?: number;
  active: boolean;
  available: boolean;
  trackStock: boolean;
  currentStock?: number;
  minStockLevel?: number;
  imageUrl?: string;
  sortOrder?: number;
}

export interface CreateProductRequest {
  categoryId?: string;
  kitchenId?: string;
  name: string;
  sku?: string;
  barcode?: string;
  unit?: string;
  purchasePrice?: number;
  salePrice: number;
  active?: boolean;
  available?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly API = `${environment.apiUrl}/products`;

  constructor(private http: HttpClient) {}

  getProducts(categoryId?: string, query?: string, activeOnly: boolean = false): Observable<ApiResponse<Product[]>> {
    let params = new HttpParams().set('activeOnly', activeOnly.toString());
    if (categoryId) params = params.set('categoryId', categoryId);
    if (query) params = params.set('query', query);
    return this.http.get<ApiResponse<Product[]>>(this.API, { params });
  }

  getProductById(id: string): Observable<ApiResponse<Product>> {
    return this.http.get<ApiResponse<Product>>(`${this.API}/${id}`);
  }

  createProduct(request: CreateProductRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(this.API, request);
  }

  updateProduct(id: string, request: Partial<CreateProductRequest>): Observable<ApiResponse<Product>> {
    return this.http.put<ApiResponse<Product>>(`${this.API}/${id}`, request);
  }

  deleteProduct(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }
}
