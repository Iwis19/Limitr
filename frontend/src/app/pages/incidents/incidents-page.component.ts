import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-incidents-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './incidents-page.component.html',
  styleUrls: ['./incidents-page.component.css']
})
export class IncidentsPageComponent implements OnInit {
  loading = false;
  activeOnly = false;
  rows: any[] = [];

  constructor(private adminApiService: AdminApiService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.adminApiService.getIncidents(this.activeOnly).subscribe({
      next: (response) => {
        this.rows = response.items || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
