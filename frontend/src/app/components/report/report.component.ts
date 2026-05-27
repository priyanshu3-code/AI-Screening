import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-report',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="report-container">
      <!-- Header -->
      <div class="header">
        <button (click)="goBack()" class="back-button" title="Back to analysis">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="19" y1="12" x2="5" y2="12"></line>
            <polyline points="12 19 5 12 12 5"></polyline>
          </svg>
          Back
        </button>
        <div class="header-content">
          <h1 class="page-title">Recruiter Summary Report</h1>
          <p class="page-subtitle">Complete analysis of your resume and candidacy</p>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p class="loading-text">Generating comprehensive report...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error && !isLoading" class="error-state">
        <div class="error-card">
          <div class="error-icon">⚠️</div>
          <h2 class="error-title">Unable to Load Report</h2>
          <p class="error-message">{{ error }}</p>
          <button (click)="uploadAnother()" class="btn btn-primary">Try Again</button>
        </div>
      </div>

      <!-- Success State -->
      <div *ngIf="report && !isLoading" class="content">
        <!-- Recommendation Banner -->
        <div class="recommendation-banner" [class]="'status-' + getRecommendationStatus(report.recommendation)">
          <div class="banner-icon">{{ getRecommendationIcon(report.recommendation) }}</div>
          <div class="banner-content">
            <h2 class="banner-title">{{ report.recommendation }}</h2>
            <p class="banner-subtitle">Recommendation from AI Analysis</p>
          </div>
        </div>

        <!-- Executive Summary -->
        <section class="report-section">
          <div class="section-header">
            <h2 class="section-title">Executive Summary</h2>
          </div>
          <p class="summary-text" *ngIf="report.executive_summary; else noSummary">{{ report.executive_summary }}</p>
          <ng-template #noSummary>
            <p class="summary-text">No summary available</p>
          </ng-template>
        </section>

        <!-- Key Strengths -->
        <section class="report-section" *ngIf="report.strengths?.length">
          <div class="section-header">
            <div class="header-icon">💪</div>
            <h2 class="section-title">Key Strengths</h2>
          </div>
          <div class="strengths-list">
            <div *ngFor="let strength of report.strengths" class="strength-item">
              <span class="strength-icon">✓</span>
              <p class="strength-text">{{ strength }}</p>
            </div>
          </div>
        </section>

        <!-- Concerns -->
        <section class="report-section" *ngIf="report.concerns?.length">
          <div class="section-header">
            <div class="header-icon">⚠️</div>
            <h2 class="section-title">Concerns & Gaps</h2>
          </div>
          <div class="concerns-list">
            <div *ngFor="let concern of report.concerns" class="concern-item">
              <span class="concern-icon">!</span>
              <p class="concern-text">{{ concern }}</p>
            </div>
          </div>
        </section>

        <!-- Interview Readiness -->
        <section class="report-section" *ngIf="report.interview_readiness">
          <div class="section-header">
            <div class="header-icon">🎯</div>
            <h2 class="section-title">Interview Readiness</h2>
          </div>
          <p class="readiness-text">{{ report.interview_readiness }}</p>
        </section>

        <!-- Next Steps -->
        <section class="report-section" *ngIf="report.next_steps?.length">
          <div class="section-header">
            <div class="header-icon">📋</div>
            <h2 class="section-title">Recommended Next Steps</h2>
          </div>
          <ol class="next-steps-list">
            <li *ngFor="let step of report.next_steps" class="step-item">
              <span class="step-text">{{ step }}</span>
            </li>
          </ol>
        </section>

        <!-- Report Metadata -->
        <section class="report-section metadata-section">
          <div class="metadata-grid">
            <div class="metadata-item">
              <span class="metadata-label">Analysis ID</span>
              <span class="metadata-value">{{ sessionId }}</span>
            </div>
            <div class="metadata-item">
              <span class="metadata-label">Processing Time</span>
              <span class="metadata-value">{{ processingTime }}ms</span>
            </div>
            <div class="metadata-item">
              <span class="metadata-label">Generated</span>
              <span class="metadata-value">{{ getCurrentDate() }}</span>
            </div>
          </div>
        </section>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button (click)="printReport()" class="btn btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9"></polyline>
              <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
            </svg>
            Print Report
          </button>

          <button (click)="downloadJSON()" class="btn btn-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            Download JSON
          </button>

          <button (click)="uploadAnother()" class="btn btn-outline">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"></path>
            </svg>
            Analyze Another
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .report-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      padding: 2rem 1rem;
    }

    /* Header */
    .header {
      max-width: 1000px;
      margin: 0 auto 2rem;
      background: white;
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      display: flex;
      align-items: center;
      gap: 1.5rem;
    }

    .back-button {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: #f5f5f5;
      border: none;
      border-radius: 6px;
      padding: 0.75rem 1rem;
      font-size: 0.9rem;
      font-weight: 500;
      color: #666;
      cursor: pointer;
      transition: all 0.2s ease;
      white-space: nowrap;
      flex-shrink: 0;
    }

    .back-button:hover {
      background: #e8e8e8;
      color: #333;
    }

    .back-button:active {
      transform: scale(0.98);
    }

    .page-title {
      margin: 0 0 0.5rem;
      font-size: 2rem;
      font-weight: 700;
      color: #1a1a1a;
    }

    .page-subtitle {
      margin: 0;
      color: #666;
      font-size: 1rem;
    }

    /* Loading State */
    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 400px;
      gap: 1rem;
    }

    .loading-spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #f3f3f3;
      border-top: 3px solid #667eea;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .loading-text {
      margin: 0;
      color: #666;
      font-size: 0.95rem;
    }

    /* Error State */
    .error-state {
      max-width: 1000px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 400px;
    }

    .error-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      text-align: center;
      max-width: 400px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .error-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .error-title {
      margin: 0 0 0.5rem;
      font-size: 1.25rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .error-message {
      margin: 0 0 1.5rem;
      color: #666;
      font-size: 0.95rem;
    }

    /* Content */
    .content {
      max-width: 1000px;
      margin: 0 auto;
    }

    /* Recommendation Banner */
    .recommendation-banner {
      display: flex;
      gap: 1.5rem;
      align-items: center;
      padding: 2rem;
      border-radius: 12px;
      margin-bottom: 2rem;
      color: white;
    }

    .recommendation-banner.status-strong_yes {
      background: linear-gradient(135deg, #22a745 0%, #1e7e34 100%);
    }

    .recommendation-banner.status-yes {
      background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
    }

    .recommendation-banner.status-maybe {
      background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
    }

    .recommendation-banner.status-no {
      background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
    }

    .banner-icon {
      font-size: 2.5rem;
      flex-shrink: 0;
    }

    .banner-title {
      margin: 0 0 0.25rem;
      font-size: 1.5rem;
      font-weight: 700;
    }

    .banner-subtitle {
      margin: 0;
      opacity: 0.9;
      font-size: 0.9rem;
    }

    /* Report Sections */
    .report-section {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .section-header {
      display: flex;
      gap: 1rem;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .header-icon {
      font-size: 1.75rem;
      flex-shrink: 0;
    }

    .section-title {
      margin: 0;
      font-size: 1.35rem;
      font-weight: 700;
      color: #1a1a1a;
    }

    /* Summary Text */
    .summary-text,
    .readiness-text {
      margin: 0;
      color: #333;
      font-size: 0.95rem;
      line-height: 1.7;
    }

    /* Strengths */
    .strengths-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .strength-item {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 1rem;
      background: #d4edda;
      border-left: 4px solid #22a745;
      border-radius: 6px;
    }

    .strength-icon {
      color: #22a745;
      font-weight: bold;
      font-size: 1.2rem;
      flex-shrink: 0;
    }

    .strength-text {
      margin: 0;
      color: #155724;
      font-size: 0.95rem;
    }

    /* Concerns */
    .concerns-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .concern-item {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 1rem;
      background: #fff3cd;
      border-left: 4px solid #ff9800;
      border-radius: 6px;
    }

    .concern-icon {
      color: #ff9800;
      font-weight: bold;
      font-size: 1.2rem;
      flex-shrink: 0;
    }

    .concern-text {
      margin: 0;
      color: #856404;
      font-size: 0.95rem;
    }

    /* Next Steps */
    .next-steps-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .step-item {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 1rem;
      background: #f5f7ff;
      border-left: 4px solid #667eea;
      border-radius: 6px;
      counter-increment: step-counter;
    }

    .step-item::before {
      content: counter(step-counter);
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      background: #667eea;
      color: white;
      border-radius: 50%;
      font-weight: 700;
      flex-shrink: 0;
    }

    .next-steps-list {
      counter-reset: step-counter;
    }

    .step-text {
      margin: 0;
      color: #333;
      font-size: 0.95rem;
      line-height: 1.5;
    }

    /* Metadata */
    .metadata-section {
      background: #f9f9f9;
      border: 1px solid #e5e5e5;
    }

    .metadata-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
    }

    .metadata-item {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .metadata-label {
      font-size: 0.75rem;
      font-weight: 700;
      color: #999;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .metadata-value {
      font-size: 0.95rem;
      color: #333;
      font-family: 'Courier New', monospace;
      word-break: break-all;
    }

    /* Action Buttons */
    .action-buttons {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .btn {
      border: none;
      border-radius: 6px;
      padding: 1rem;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
    }

    .btn-secondary {
      background: white;
      color: #667eea;
      border: 2px solid #667eea;
    }

    .btn-secondary:hover {
      background: #f5f7ff;
    }

    .btn-outline {
      background: white;
      color: #999;
      border: 2px solid #ddd;
    }

    .btn-outline:hover {
      border-color: #667eea;
      color: #667eea;
    }

    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        gap: 1rem;
        align-items: flex-start;
      }

      .recommendation-banner {
        flex-direction: column;
        text-align: center;
        gap: 1rem;
      }

      .banner-icon {
        font-size: 2rem;
      }

      .metadata-grid {
        grid-template-columns: 1fr;
      }

      .action-buttons {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 640px) {
      .report-container {
        padding: 1rem;
      }

      .header {
        padding: 1.5rem;
        flex-direction: column;
        gap: 1rem;
        align-items: flex-start;
      }

      .page-title {
        font-size: 1.5rem;
      }

      .report-section {
        padding: 1.25rem;
      }

      .section-title {
        font-size: 1.1rem;
      }
    }
  `]
})
export class ReportComponent implements OnInit {
  sessionId: string = '';
  report: any = null;
  processingTime: number = 0;
  isLoading = true;
  error: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit() {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId') || '';
    this.loadReport();
  }

  loadReport() {
    this.apiService.getResults(this.sessionId).subscribe({
      next: (data) => {
        // Use recruiter summary if available, otherwise create default report
        if (data.recruiterSummary && data.recruiterSummary.executive_summary) {
          this.report = data.recruiterSummary;
        } else {
          // Create default report if empty
          this.report = this.createDefaultReport(data);
        }
        this.processingTime = data.processingTimeMs;
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load report: ' + (err.error?.message || err.message);
        this.isLoading = false;
      }
    });
  }

  private createDefaultReport(data: any): any {
    return {
      executive_summary: `Comprehensive analysis completed for your resume against the job description. Your overall match score is ${data.extractedData?.match_score || 0}%. Review the breakdown of your skills, experience, and qualifications below.`,
      recommendation: data.extractedData?.match_score >= 70 ? 'YES' : 'NO',
      strengths: data.extractedData?.strengths || ['Strong technical foundation', 'Relevant experience'],
      concerns: data.extractedData?.missing_requirements || ['Room for growth in specific areas'],
      next_steps: data.extractedData?.match_score >= 70
        ? ['Prepare for technical interview', 'Review system design concepts', 'Practice coding problems']
        : ['Develop missing skills', 'Gain relevant experience', 'Consider related opportunities'],
      interview_readiness: data.extractedData?.match_score >= 70
        ? 'Ready for interview. Good match with the role requirements.'
        : 'Not yet ready. Focus on developing key skills mentioned above.'
    };
  }

  getRecommendationStatus(recommendation: string): string {
    return recommendation?.toLowerCase().replace(' ', '_') || 'no';
  }

  getRecommendationIcon(recommendation: string): string {
    const rec = recommendation?.toUpperCase();
    if (rec === 'STRONG_YES' || rec === 'YES') return '✓';
    if (rec === 'MAYBE') return '?';
    return '✕';
  }

  printReport() {
    window.print();
  }

  downloadJSON() {
    const dataStr = JSON.stringify(this.report, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `report-${this.sessionId}.json`;
    link.click();
  }

  getCurrentDate(): string {
    return new Date().toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  goBack() {
    this.router.navigate(['/analysis', this.sessionId]);
  }

  uploadAnother() {
    this.router.navigate(['/upload']);
  }
}
