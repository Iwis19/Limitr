import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-logs-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './logs-page.component.html',
  styleUrls: ['./logs-page.component.css']
})
export class LogsPageComponent implements OnInit {
  loading = false;
  rows: any[] = [];

  filterForm = this.formBuilder.group({
    principalId: [''],
    statusCode: ['']
  });

  constructor(
    private formBuilder: FormBuilder,
    private adminApiService: AdminApiService
  ) {}

  ngOnInit(): void {
    this.search();
  }

  search(): void {
    this.loading = true;
    const values = this.filterForm.getRawValue();
    this.adminApiService.getLogs({
      principalId: values.principalId || undefined,
      statusCode: values.statusCode || undefined
    }).subscribe({
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
