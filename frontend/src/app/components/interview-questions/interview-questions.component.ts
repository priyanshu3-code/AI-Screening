import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-interview-questions',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="interview-container">
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
          <h1 class="page-title">Interview Preparation</h1>
          <p class="page-subtitle">Master these questions to ace your interview</p>
        </div>
        <div class="header-stats">
          <div class="stat" *ngIf="questions && questions.length > 0">
            <span class="stat-value">{{ questions.length }}</span>
            <span class="stat-label">Questions</span>
          </div>
          <div class="stat" *ngIf="questions">
            <span class="stat-value">{{ calculateTotalTime() }}</span>
            <span class="stat-label">Min Total Time</span>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p class="loading-text">Loading interview questions...</p>
      </div>

      <!-- Error State -->
      <div *ngIf="error && !isLoading" class="error-state">
        <div class="error-card">
          <div class="error-icon">⚠️</div>
          <h2 class="error-title">Unable to Load Questions</h2>
          <p class="error-message">{{ error }}</p>
          <button (click)="uploadAnother()" class="btn btn-primary">Try Again</button>
        </div>
      </div>

      <!-- Success State -->
      <div *ngIf="questions && !isLoading" class="content">
        <!-- Empty State -->
        <div *ngIf="questions.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <h2 class="empty-title">No Questions Available</h2>
          <p class="empty-message">Interview questions will be generated based on your resume and the job description</p>
          <button (click)="uploadAnother()" class="btn btn-secondary">Analyze Another Resume</button>
        </div>

        <!-- Questions List -->
        <div *ngIf="questions.length > 0" class="questions-list">
          <div *ngFor="let question of questions; let i = index" class="question-card">
            <div class="question-header">
              <div class="question-number">{{ i + 1 }}</div>
              <h3 class="question-title">{{ question.question }}</h3>
            </div>

            <div class="question-meta">
              <div class="meta-item" *ngIf="question.category">
                <span class="meta-icon">📂</span>
                <span class="meta-badge">{{ question.category }}</span>
              </div>
              <div class="meta-item" *ngIf="question.difficulty">
                <span class="meta-icon">📊</span>
                <span class="meta-badge" [class]="'difficulty-' + question.difficulty.toLowerCase()">
                  {{ question.difficulty }}
                </span>
              </div>
              <div class="meta-item" *ngIf="question.time_estimate_minutes">
                <span class="meta-icon">⏱️</span>
                <span class="meta-badge">{{ question.time_estimate_minutes }}min</span>
              </div>
            </div>

            <div *ngIf="question.tip" class="question-tip">
              <span class="tip-icon">💡</span>
              <p class="tip-text">{{ question.tip }}</p>
            </div>

            <button
              (click)="toggleAnswer(i)"
              class="btn-reveal"
              *ngIf="question.suggested_answer">
              {{ expandedQuestions[i] ? '▼ Hide' : '▶ Show' }} Suggested Answer
            </button>

            <div *ngIf="expandedQuestions[i] && question.suggested_answer" class="question-answer">
              <div class="answer-header">
                <span class="answer-label">Suggested Answer</span>
              </div>
              <p class="answer-text">{{ question.suggested_answer }}</p>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button (click)="printQuestions()" class="btn btn-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9"></polyline>
              <path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"></path>
            </svg>
            Print Questions
          </button>

          <button (click)="goToReport()" class="btn btn-secondary">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
              <polyline points="13 2 13 9 20 9"></polyline>
            </svg>
            View Full Report
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

    .interview-container {
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
      display: flex;
      justify-content: space-between;
      align-items: start;
      gap: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
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

    .header-content {
      flex: 1;
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

    .header-stats {
      display: flex;
      gap: 2rem;
    }

    .stat {
      text-align: center;
    }

    .stat-value {
      display: block;
      font-size: 1.75rem;
      font-weight: 700;
      color: #667eea;
    }

    .stat-label {
      display: block;
      font-size: 0.75rem;
      color: #999;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-top: 0.5rem;
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

    /* Empty State */
    .empty-state {
      max-width: 1000px;
      margin: 0 auto;
      background: white;
      border-radius: 12px;
      padding: 3rem 2rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .empty-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    .empty-title {
      margin: 0 0 0.5rem;
      font-size: 1.25rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .empty-message {
      margin: 0 0 2rem;
      color: #666;
      font-size: 0.95rem;
    }

    /* Content */
    .content {
      max-width: 1000px;
      margin: 0 auto;
    }

    .questions-list {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      margin-bottom: 2rem;
    }

    .question-card {
      background: white;
      border-radius: 12px;
      padding: 1.75rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      transition: all 0.3s ease;
    }

    .question-card:hover {
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
    }

    .question-header {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
      align-items: flex-start;
    }

    .question-number {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      flex-shrink: 0;
    }

    .question-title {
      margin: 0;
      font-size: 1.1rem;
      font-weight: 600;
      color: #1a1a1a;
      flex: 1;
    }

    .question-meta {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
      flex-wrap: wrap;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.85rem;
    }

    .meta-icon {
      font-size: 1rem;
    }

    .meta-badge {
      background: #f5f5f5;
      padding: 0.4rem 0.8rem;
      border-radius: 16px;
      color: #666;
      font-weight: 500;
    }

    .difficulty-easy {
      background: #d4edda;
      color: #155724;
    }

    .difficulty-medium {
      background: #fff3cd;
      color: #856404;
    }

    .difficulty-hard {
      background: #f8d7da;
      color: #721c24;
    }

    .question-tip {
      background: #e7f3ff;
      border-left: 4px solid #667eea;
      padding: 1rem;
      border-radius: 6px;
      margin-bottom: 1rem;
      display: flex;
      gap: 0.75rem;
    }

    .tip-icon {
      font-size: 1.2rem;
      flex-shrink: 0;
    }

    .tip-text {
      margin: 0;
      color: #333;
      font-size: 0.9rem;
    }

    .btn-reveal {
      background: white;
      border: 2px solid #667eea;
      color: #667eea;
      padding: 0.6rem 1rem;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      font-size: 0.85rem;
      transition: all 0.2s ease;
      margin-bottom: 1rem;
    }

    .btn-reveal:hover {
      background: #f5f7ff;
    }

    .question-answer {
      background: #f9f9f9;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      padding: 1rem;
      margin-top: 1rem;
    }

    .answer-header {
      display: flex;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }

    .answer-label {
      font-size: 0.75rem;
      font-weight: 700;
      color: #667eea;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .answer-text {
      margin: 0;
      color: #333;
      font-size: 0.9rem;
      line-height: 1.6;
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
      }

      .header-stats {
        width: 100%;
        justify-content: space-around;
      }

      .question-header {
        flex-direction: column;
        gap: 0.5rem;
      }

      .question-meta {
        gap: 0.75rem;
      }

      .action-buttons {
        grid-template-columns: 1fr;
      }
    }

    @media (max-width: 640px) {
      .interview-container {
        padding: 1rem;
      }

      .page-title {
        font-size: 1.5rem;
      }

      .question-card {
        padding: 1.25rem;
      }

      .meta-badge {
        font-size: 0.75rem;
      }
    }
  `]
})
export class InterviewQuestionsComponent implements OnInit {
  sessionId: string = '';
  questions: any[] = [];
  isLoading = true;
  error: string = '';
  expandedQuestions: { [key: number]: boolean } = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit() {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId') || '';
    this.loadQuestions();
  }

  loadQuestions() {
    this.apiService.getResults(this.sessionId).subscribe({
      next: (data) => {
        this.questions = data.interviewQuestions || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load questions: ' + (err.error?.message || err.message);
        this.isLoading = false;
      }
    });
  }

  toggleAnswer(index: number) {
    this.expandedQuestions[index] = !this.expandedQuestions[index];
  }

  calculateTotalTime(): number {
    return this.questions.reduce((total, q) => total + (q.time_estimate_minutes || 0), 0);
  }

  printQuestions() {
    window.print();
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
