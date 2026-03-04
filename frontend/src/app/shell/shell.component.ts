import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrls: ['./shell.component.css']
})
export class ShellComponent implements OnInit {
  darkMode = localStorage.getItem('limitr_dark') === '1';

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.applyDarkMode();
  }

  toggleTheme(): void {
    this.darkMode = !this.darkMode;
    localStorage.setItem('limitr_dark', this.darkMode ? '1' : '0');
    this.applyDarkMode();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  private applyDarkMode(): void {
    document.body.classList.toggle('theme-dark', this.darkMode);
  }
}
