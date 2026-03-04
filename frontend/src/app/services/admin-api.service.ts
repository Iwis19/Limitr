import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class AdminApiService {
  constructor(private http: HttpClient) {}

  getStats(): Observable<any> {
    return this.http.get('/admin/stats');
  }

  getLogs(filters: { principalId?: string; statusCode?: string }): Observable<any> {
    let params = new HttpParams();
    if (filters.principalId) {
      params = params.set('principalId', filters.principalId);
    }
    if (filters.statusCode) {
      params = params.set('statusCode', filters.statusCode);
    }
    return this.http.get('/admin/logs', { params });
  }

  getIncidents(activeOnly: boolean): Observable<any> {
    return this.http.get('/admin/incidents', { params: { activeOnly } });
  }

  updateRules(payload: any): Observable<any> {
    return this.http.put('/admin/rules', payload);
  }

  banPrincipal(principalId: string, minutes: number): Observable<any> {
    return this.http.post('/admin/actions/ban', { principalId, minutes });
  }

  unbanPrincipal(principalId: string): Observable<any> {
    return this.http.post('/admin/actions/unban', { principalId });
  }
}
