import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from './auth.service';

export interface KitchenSummary {
  id: string;
  name: string;
  code: string;
}

export interface Employee {
  id: string;
  username: string;
  firstName: string;
  lastName?: string;
  fullName: string;
  email?: string;
  phone?: string;
  active: boolean;
  role: string;
  roleId?: string;
  permissions: string[];
  kitchenIds?: string[];
  kitchens?: KitchenSummary[];
  createdAt: string;
}

export interface Role {
  id: string;
  name: string;
  description?: string;
}

export interface CreateEmployeeRequest {
  username: string;
  password: string;
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  pin?: string;
  role: string;
  kitchenIds?: string[];
}

export interface UpdateEmployeeRequest {
  firstName: string;
  lastName?: string;
  email?: string;
  phone?: string;
  pin?: string;
  role: string;
  active?: boolean;
  kitchenIds?: string[];
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly API = `${environment.apiUrl}/users`;

  constructor(private http: HttpClient) {}

  getUsers(page?: number, size?: number): Observable<ApiResponse<Employee[]>> {
    let params = new HttpParams();
    if (page !== undefined && page !== null) params = params.set('page', page.toString());
    if (size !== undefined && size !== null) params = params.set('size', size.toString());
    return this.http.get<ApiResponse<Employee[]>>(this.API, { params });
  }

  getUserById(id: string): Observable<ApiResponse<Employee>> {
    return this.http.get<ApiResponse<Employee>>(`${this.API}/${id}`);
  }

  createUser(request: CreateEmployeeRequest): Observable<ApiResponse<Employee>> {
    return this.http.post<ApiResponse<Employee>>(this.API, request);
  }

  updateUser(id: string, request: UpdateEmployeeRequest): Observable<ApiResponse<Employee>> {
    return this.http.put<ApiResponse<Employee>>(`${this.API}/${id}`, request);
  }

  deactivateUser(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.API}/${id}`);
  }

  resetPassword(id: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.API}/${id}/password`, { newPassword });
  }

  getRoles(): Observable<ApiResponse<Role[]>> {
    return this.http.get<ApiResponse<Role[]>>(`${this.API}/roles`);
  }
}
