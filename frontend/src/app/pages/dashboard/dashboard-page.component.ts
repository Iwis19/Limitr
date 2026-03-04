import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard-page.component.html',
  styleUrls: ['./dashboard-page.component.css']
})
export class DashboardPageComponent implements OnInit {
  loading = true;
  stats: any = null;

  constructor(private adminApiService: AdminApiService) {}

  ngOnInit(): void {
    this.refresh();
  }

  refresh(): void {
    this.loading = true;
    this.adminApiService.getStats().subscribe({
      next: (response) => {
        this.stats = response;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
