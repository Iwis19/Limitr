import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AdminApiService } from '../../services/admin-api.service';

@Component({
  selector: 'app-rules-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rules-page.component.html',
  styleUrls: ['./rules-page.component.css']
})
export class RulesPageComponent implements OnInit {
  message = '';
  error = '';

  ruleForm = this.formBuilder.group({
    baseLimitPerMinute: [60, [Validators.required, Validators.min(1)]],
    throttledLimitPerMinute: [20, [Validators.required, Validators.min(1)]],
    warnThreshold: [2, [Validators.required, Validators.min(0)]],
    throttleThreshold: [4, [Validators.required, Validators.min(1)]],
    banThreshold: [7, [Validators.required, Validators.min(1)]],
    banMinutes: [15, [Validators.required, Validators.min(1)]]
  });

  banForm = this.formBuilder.group({
    principalId: ['', Validators.required],
    minutes: [15, [Validators.required, Validators.min(1)]]
  });

  unbanForm = this.formBuilder.group({
    principalId: ['', Validators.required]
  });

  constructor(
    private formBuilder: FormBuilder,
    private adminApiService: AdminApiService
  ) {}

  ngOnInit(): void {
    this.loadRules();
  }

  loadRules(): void {
    this.adminApiService.getStats().subscribe({
      next: (response) => {
        this.ruleForm.patchValue(response.rules);
      }
    });
  }

  saveRules(): void {
    this.message = '';
    this.error = '';
    if (this.ruleForm.invalid) {
      this.ruleForm.markAllAsTouched();
      return;
    }

    this.adminApiService.updateRules(this.ruleForm.getRawValue()).subscribe({
      next: () => {
        this.message = 'Rules updated successfully.';
      },
      error: () => {
        this.error = 'Unable to update rules.';
      }
    });
  }

  banPrincipal(): void {
    this.message = '';
    this.error = '';
    if (this.banForm.invalid) {
      this.banForm.markAllAsTouched();
      return;
    }

    const values = this.banForm.getRawValue();
    this.adminApiService.banPrincipal(values.principalId || '', Number(values.minutes || 15)).subscribe({
      next: () => {
        this.message = 'Principal banned.';
      },
      error: () => {
        this.error = 'Unable to ban principal.';
      }
    });
  }

  unbanPrincipal(): void {
    this.message = '';
    this.error = '';
    if (this.unbanForm.invalid) {
      this.unbanForm.markAllAsTouched();
      return;
    }

    const values = this.unbanForm.getRawValue();
    this.adminApiService.unbanPrincipal(values.principalId || '').subscribe({
      next: () => {
        this.message = 'Principal unbanned.';
      },
      error: () => {
        this.error = 'Unable to unban principal.';
      }
    });
  }
}
