import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';

interface LoginResponse {
  accessToken: string;
  tokenType: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'limitr_token';

  constructor(private http: HttpClient) {}

  login(username: string, password: string): Observable<void> {
    return this.http.post<LoginResponse>('/auth/login', { username, password }).pipe(
      tap((response) => localStorage.setItem(this.tokenKey, response.accessToken)),
      map(() => void 0)
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
  }

  isAuthenticated(): boolean {
    return this.getValidToken() !== null;
  }

  getValidToken(): string | null {
    const token = localStorage.getItem(this.tokenKey);
    if (!token) {
      return null;
    }

    if (!this.isTokenUsable(token)) {
      this.logout();
      return null;
    }

    return token;
  }

  private isTokenUsable(token: string): boolean {
    try {
      const payload = this.parseJwtPayload(token);
      return typeof payload.exp === 'number' && payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }

  private parseJwtPayload(token: string): { exp?: number } {
    const parts = token.split('.');
    if (parts.length !== 3) {
      throw new Error('Invalid JWT format.');
    }

    const payload = parts[1]
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(Math.ceil(parts[1].length / 4) * 4, '=');

    return JSON.parse(atob(payload)) as { exp?: number };
  }
}
