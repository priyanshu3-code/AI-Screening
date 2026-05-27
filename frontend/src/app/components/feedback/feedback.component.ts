import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="feedback-container">
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
          <h1 class="page-title">Growth Recommendations</h1>
          <p class="page-subtitle">Actionable feedback to improve your candidacy</p>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p class="loading-text">Generating personalized feedback...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error && !isLoading" class="error-state">
        <div class="error-card">
          <div class="error-icon">⚠️</div>
          <h2 class="error-title">Unable to Load Feedback</h2>
          <p class="error-message">{{ error }}</p>
          <button (click)="uploadAnother()" class="btn btn-primary">Try Again</button>
        </div>
      </div>

      <!-- Success State -->
      <div *ngIf="feedback && !isLoading" class="content">
        <!-- Why Weren't You Selected -->
        <section class="feedback-section">
          <div class="section-header">
            <div class="header-icon">❌</div>
            <div class="header-content">
              <h2 class="section-title">Why You Weren't Selected</h2>
              <p class="section-subtitle">Understanding the gaps in your application</p>
            </div>
          </div>

          <div *ngIf="feedback.rejection_reasons && feedback.rejection_reasons.length > 0" class="reasons-list">
            <div *ngFor="let reason of feedback.rejection_reasons" class="reason-item">
              <span class="reason-icon">•</span>
              <p class="reason-text">{{ reason }}</p>
            </div>
          </div>

          <div *ngIf="!feedback.rejection_reasons || feedback.rejection_reasons.length === 0" class="empty-state-message">
            <p>No specific rejection reasons documented</p>
          </div>
        </section>

        <!-- Skills to Improve -->
        <section class="feedback-section" *ngIf="feedback.improvements && feedback.improvements.length > 0">
          <div class="section-header">
            <div class="header-icon">📈</div>
            <div class="header-content">
              <h2 class="section-title">Skills to Develop</h2>
              <p class="section-subtitle">Recommended improvements with learning resources</p>
            </div>
          </div>

          <div class="improvements-grid">
            <div *ngFor="let improvement of feedback.improvements" class="improvement-card">
              <div class="improvement-header">
                <h3 class="improvement-skill">{{ improvement.skill }}</h3>
                <span class="improvement-level" [class]="'level-' + getLevelClass(improvement.current_level)">
                  {{ improvement.current_level }}
                </span>
              </div>

              <div class="improvement-timeline">
                <span class="timeline-label">Learning Timeline</span>
                <span class="timeline-value">~{{ improvement.estimated_months }} months</span>
              </div>

              <div class="resources-section">
                <h4 class="resources-title">Recommended Resources</h4>
                <ul class="resources-list">
                  <li *ngFor="let resource of improvement.recommended_resources" class="resource-item">
                    {{ resource }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </section>

        <!-- Alternative Roles -->
        <section class="feedback-section" *ngIf="feedback.alternative_roles && feedback.alternative_roles.length > 0">
          <div class="section-header">
            <div class="header-icon">🎯</div>
            <div class="header-content">
              <h2 class="section-title">Alternative Opportunities</h2>
              <p class="section-subtitle">Roles that match your current skillset</p>
            </div>
          </div>

          <div class="roles-grid">
            <div *ngFor="let role of feedback.alternative_roles" class="role-card">
              <div class="role-checkmark">✓</div>
              <p class="role-title">{{ role }}</p>
            </div>
          </div>
        </section>

        <!-- Encouragement -->
        <section class="feedback-section encouragement-section" *ngIf="feedback.encouragement">
          <div class="encouragement-card">
            <div class="encouragement-icon">⭐</div>
            <h2 class="encouragement-title">Keep Going!</h2>
            <p class="encouragement-message">{{ feedback.encouragement }}</p>
          </div>
        </section>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button (click)="goToReport()" class="btn btn-primary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
              <polyline points="13 2 13 9 20 9"></polyline>
            </svg>
            View Full Report
          </button>

          <button (click)="uploadAnother()" class="btn btn-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"></path>
            </svg>
            Analyze Another Resume
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .feedback-container {
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

    /* Feedback Sections */
    .feedback-section {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .section-header {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      margin-bottom: 1.5rem;
    }

    .header-icon {
      font-size: 2rem;
      flex-shrink: 0;
    }

    .header-content {
      flex: 1;
    }

    .section-title {
      margin: 0;
      font-size: 1.35rem;
      font-weight: 700;
      color: #1a1a1a;
    }

    .section-subtitle {
      margin: 0.5rem 0 0;
      color: #666;
      font-size: 0.95rem;
    }

    /* Reasons List */
    .reasons-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .reason-item {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      padding: 0.75rem;
      background: #f9f9f9;
      border-radius: 8px;
    }

    .reason-icon {
      color: #dc3545;
      font-weight: bold;
      font-size: 1.2rem;
      flex-shrink: 0;
    }

    .reason-text {
      margin: 0;
      color: #333;
      font-size: 0.95rem;
    }

    /* Improvements Grid */
    .improvements-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1.5rem;
    }

    .improvement-card {
      background: #f9f9f9;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      padding: 1.5rem;
      transition: all 0.3s ease;
    }

    .improvement-card:hover {
      border-color: #667eea;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
    }

    .improvement-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .improvement-skill {
      margin: 0;
      font-size: 1rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .improvement-level {
      background: white;
      padding: 0.4rem 0.8rem;
      border-radius: 20px;
      font-size: 0.75rem;
      font-weight: 600;
      white-space: nowrap;
    }

    .level-beginner {
      background: #ffebee;
      color: #c62828;
    }

    .level-intermediate {
      background: #fff3cd;
      color: #856404;
    }

    .level-advanced {
      background: #d4edda;
      color: #155724;
    }

    .improvement-timeline {
      display: flex;
      justify-content: space-between;
      padding: 0.75rem;
      background: white;
      border-radius: 6px;
      margin-bottom: 1rem;
      font-size: 0.85rem;
    }

    .timeline-label {
      color: #999;
      font-weight: 600;
    }

    .timeline-value {
      color: #667eea;
      font-weight: 600;
    }

    .resources-section {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .resources-title {
      margin: 0 0 0.5rem;
      font-size: 0.85rem;
      font-weight: 600;
      color: #1a1a1a;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .resources-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .resource-item {
      font-size: 0.85rem;
      color: #666;
      padding-left: 1.25rem;
      position: relative;
    }

    .resource-item::before {
      content: '→';
      position: absolute;
      left: 0;
      color: #667eea;
      font-weight: bold;
    }

    /* Roles Grid */
    .roles-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 1rem;
    }

    .role-card {
      background: linear-gradient(135deg, #d4edda 0%, #c3e6cb 100%);
      border: 1px solid #c3e6cb;
      border-radius: 8px;
      padding: 1.25rem;
      text-align: center;
      position: relative;
      transition: all 0.3s ease;
    }

    .role-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(34, 167, 69, 0.2);
    }

    .role-checkmark {
      font-size: 1.5rem;
      color: #155724;
      margin-bottom: 0.5rem;
    }

    .role-title {
      margin: 0;
      font-weight: 600;
      color: #155724;
      font-size: 0.95rem;
    }

    /* Encouragement Section */
    .encouragement-section {
      border: none;
      padding: 0;
    }

    .encouragement-card {
      background: linear-gradient(135deg, #fff9e6 0%, #ffe082 100%);
      border: 2px solid #ffd54f;
      border-radius: 12px;
      padding: 2rem;
      text-align: center;
    }

    .encouragement-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .encouragement-title {
      margin: 0 0 0.75rem;
      font-size: 1.5rem;
      font-weight: 700;
      color: #856404;
    }

    .encouragement-message {
      margin: 0;
      color: #856404;
      font-size: 1rem;
      line-height: 1.6;
    }

    /* Empty State */
    .empty-state-message {
      text-align: center;
      padding: 2rem;
      color: #999;
      font-size: 0.95rem;
    }

    /* Action Buttons */
    .action-buttons {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
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

    @media (max-width: 768px) {
      .section-header {
        gap: 0.75rem;
      }

      .improvement-header {
        flex-direction: column;
        align-items: flex-start;
      }

      .improvements-grid {
        grid-template-columns: 1fr;
      }

      .roles-grid {
        grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
      }

      .action-buttons {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 640px) {
      .feedback-container {
        padding: 1rem;
      }

      .header {
        padding: 1.5rem;
      }

      .page-title {
        font-size: 1.5rem;
      }

      .feedback-section {
        padding: 1.25rem;
      }

      .header-icon {
        font-size: 1.5rem;
      }
    }
  `]
})
export class FeedbackComponent implements OnInit {
  sessionId: string = '';
  feedback: any = null;
  isLoading = true;
  error: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit() {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId') || '';
    this.loadFeedback();
  }

  loadFeedback() {
    this.apiService.getResults(this.sessionId).subscribe({
      next: (data) => {
        // Handle both high-match (rejection guidance) and low-match (interview questions) scenarios
        if (data.rejectionGuidance) {
          this.feedback = data.rejectionGuidance;
        } else if (data.extractedData && data.extractedData.match_score >= 70) {
          // High match - show success feedback instead
          this.feedback = this.createSuccessFeedback(data);
        } else {
          this.error = 'No feedback available for this session';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load feedback: ' + (err.error?.message || err.message);
        this.isLoading = false;
      }
    });
  }

  private createSuccessFeedback(data: any): any {
    return {
      rejection_reasons: [],
      improvements: [],
      alternative_roles: [],
      encouragement: `Congratulations! You achieved a ${data.extractedData?.match_score || 0}% match score. You qualify for an interview. Review the interview questions to prepare for your upcoming interview.`
    };
  }

  getLevelClass(level: string): string {
    const lowerLevel = level?.toLowerCase() || '';
    if (lowerLevel.includes('beginner') || lowerLevel.includes('basic')) return 'beginner';
    if (lowerLevel.includes('intermediate') || lowerLevel.includes('mid')) return 'intermediate';
    return 'advanced';
  }

  goToReport() {
    this.router.navigate(['/report', this.sessionId]);
  }

  goBack() {
    this.router.navigate(['/analysis', this.sessionId]);
  }

  uploadAnother() {
    this.router.navigate(['/upload']);
  }
}
