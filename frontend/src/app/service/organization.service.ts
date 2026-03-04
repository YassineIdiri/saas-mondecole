import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface RegisterOrganizationRequest {
  name: string;
  email: string;
  phone?: string;
  address?: string;
  city?: string;
  postalCode?: string;
  country?: string;
  website?: string;
  type: OrganizationType;
  description?: string;
  adminUsername: string;
  adminEmail: string;
  adminPassword: string;
  adminFirstName: string;
  adminLastName: string;
}

export interface RegisterOrganizationResponse {
  organizationId: number;
  organizationName: string;
  slug: string;
  invitationCode: string;
  adminUser: UserResponse;
  createdAt: string;
  message: string;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export type OrganizationType = 'TRAINING_CENTER' | 'UNIVERSITY' | 'SCHOOL' | 'COMPANY' | 'OTHER';
export type UserRole = 'ADMIN' | 'TEACHER' | 'STUDENT';

export interface VerifyInvitationCodeRequest {
  invitationCode: string;
}

export interface VerifyInvitationCodeResponse {
  valid: boolean;
  organization: OrganizationBasicInfo | null;
  message: string;
}

export interface OrganizationBasicInfo {
  id: number;
  name: string;
  slug: string;
  logoUrl: string | null;
}

export interface JoinOrganizationRequest {
  invitationCode: string;
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  role: 'TEACHER' | 'STUDENT';
}

export interface JoinOrganizationResponse {
  userId: number;
  username: string;
  email: string;
  role: UserRole;
  organization: OrganizationBasicInfo;
  createdAt: string;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class OrganizationService {
  private apiUrl = `${environment.apiUrl}/api/public/organizations`;

  constructor(private http: HttpClient) {}

  registerOrganization(request: RegisterOrganizationRequest): Observable<RegisterOrganizationResponse> {
    return this.http.post<RegisterOrganizationResponse>(`${this.apiUrl}/register`, request);
  }

  verifyInvitationCode(code: string): Observable<VerifyInvitationCodeResponse> {
    return this.http.post<VerifyInvitationCodeResponse>(`${this.apiUrl}/verify-invitation`, {
      invitationCode: code
    });
  }

  joinOrganization(request: JoinOrganizationRequest): Observable<JoinOrganizationResponse> {
    return this.http.post<JoinOrganizationResponse>(`${this.apiUrl}/join`, request);
  }
}
