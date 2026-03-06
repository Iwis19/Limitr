import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { LoginPageComponent } from './pages/login/login-page.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { LogsPageComponent } from './pages/logs/logs-page.component';
import { IncidentsPageComponent } from './pages/incidents/incidents-page.component';
import { RulesPageComponent } from './pages/rules/rules-page.component';
import { GuardianPageComponent } from './pages/guardian/guardian-page.component';
import { ShellComponent } from './shell/shell.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardPageComponent },
      { path: 'guardian', component: GuardianPageComponent },
      { path: 'logs', component: LogsPageComponent },
      { path: 'incidents', component: IncidentsPageComponent },
      { path: 'rules', component: RulesPageComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
