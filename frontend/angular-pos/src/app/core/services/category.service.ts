import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface Category {
  id: string;
  name: string;
  nameUz?: string;
  nameRu?: string;
  icon?: string;
  color?: string;
  sortOrder: number;
  active: boolean;
  productCount?: number;
}

export interface CreateCategoryRequest {
  name: string;
  nameUz?: string;
  nameRu?: string;
  icon?: string;
  color?: string;
  sortOrder?: number;
}

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly API = `${environment.apiUrl}/categories`;

  constructor(private http: HttpClient) {}

  getCategories(activeOnly: boolean = false): Observable<ApiResponse<Category[]>> {
    const params = new HttpParams().set('activeOnly', activeOnly.toString());
    return this.http.get<ApiResponse<Category[]>>(this.API, { params });
  }

  createCategory(request: CreateCategoryRequest): Observable<ApiResponse<Category>> {
    return this.http.post<ApiResponse<Category>>(this.API, request);
  }

  updateCategory(id: string, request: Partial<CreateCategoryRequest>): Observable<ApiResponse<Category>> {
    return this.http.put<ApiResponse<Category>>(`${this.API}/${id}`, request);
  }

  deleteCategory(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }
}
