import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { Subject, interval } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-analysis-result',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="analysis-container">
      <!-- Loading State -->
      <div *ngIf="isLoading" class="loading-state">
        <div class="loading-card">
          <div class="loading-animation">
            <div class="pulse"></div>
          </div>
          <h2 class="loading-title">Analyzing Your Resume</h2>
          <p class="loading-text">Our AI is comparing your resume with the job description...</p>

          <!-- Progress Steps -->
          <div class="progress-steps">
            <div class="step" [class.active]="currentStep >= 1" [class.completed]="currentStep > 1">
              <div class="step-indicator">
                <span *ngIf="currentStep <= 1">1</span>
                <span *ngIf="currentStep > 1">✓</span>
              </div>
              <div class="step-content">
                <p class="step-label">Extracting Resume</p>
                <p class="step-duration">~25 seconds</p>
              </div>
            </div>

            <div class="step" [class.active]="currentStep >= 2" [class.completed]="currentStep > 2">
              <div class="step-indicator">
                <span *ngIf="currentStep <= 2">2</span>
                <span *ngIf="currentStep > 2">✓</span>
              </div>
              <div class="step-content">
                <p class="step-label">Generating Questions</p>
                <p class="step-duration">~40 seconds</p>
              </div>
            </div>

            <div class="step" [class.active]="currentStep >= 3" [class.completed]="currentStep > 3">
              <div class="step-indicator">
                <span *ngIf="currentStep <= 3">3</span>
                <span *ngIf="currentStep > 3">✓</span>
              </div>
              <div class="step-content">
                <p class="step-label">Creating Summary</p>
                <p class="step-duration">~5 seconds</p>
              </div>
            </div>
          </div>

          <div class="progress-bar-container">
            <div class="progress-bar">
              <div class="progress-fill" [style.width.%]="progress"></div>
            </div>
            <p class="progress-text">{{ Math.round(progress) }}% Complete</p>
          </div>

          <p class="loading-estimate">Estimated time remaining: {{ estimatedTime }}s</p>
        </div>
      </div>

      <!-- Error State -->
      <div *ngIf="error && !isLoading" class="error-state">
        <div class="error-card">
          <div class="error-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="12" y1="8" x2="12" y2="12"></line>
              <line x1="12" y1="16" x2="12.01" y2="16"></line>
            </svg>
          </div>
          <h2 class="error-title">Analysis Failed</h2>
          <p class="error-message">{{ error }}</p>
          <button (click)="uploadAnother()" class="btn btn-primary">Try Again</button>
        </div>
      </div>

      <!-- Success State -->
      <div *ngIf="analysisData && !isLoading" class="results-view">
        <!-- Header -->
        <div class="results-header">
          <button (click)="goBack()" class="back-button" title="Back to upload">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"></line>
              <polyline points="12 19 5 12 12 5"></polyline>
            </svg>
            Back
          </button>
          <div class="header-content">
            <h1 class="results-title">Resume Analysis Complete</h1>
            <p class="results-subtitle">Here's how your resume matches the job description</p>
          </div>
        </div>

        <!-- Main Results -->
        <div class="results-content">
          <!-- Match Score Card -->
          <div class="score-section">
            <div class="score-card">
              <div class="score-header">
                <h2 class="score-label">Match Score</h2>
                <p class="score-hint">Based on skills, experience, and qualifications</p>
              </div>

              <div class="score-display">
                <div class="score-circle" [class]="getScoreClass(analysisData.extractedData.match_score)">
                  <span class="score-number">{{ analysisData.extractedData.match_score }}</span>
                  <span class="score-percent">%</span>
                </div>
              </div>

              <div class="score-breakdown">
                <div class="breakdown-item">
                  <span class="breakdown-label">Skills Match</span>
                  <span class="breakdown-value">{{ analysisData.matchScoreBreakdown?.skills_match_percentage || 'N/A' }}%</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">Experience Match</span>
                  <span class="breakdown-value">{{ analysisData.matchScoreBreakdown?.experience_match_percentage || 'N/A' }}%</span>
                </div>
                <div class="breakdown-item">
                  <span class="breakdown-label">Tech Stack Match</span>
                  <span class="breakdown-value">{{ analysisData.matchScoreBreakdown?.tech_stack_match_percentage || 'N/A' }}%</span>
                </div>
              </div>

              <!-- Recommendation -->
              <div class="recommendation" [class]="getRecommendationClass(analysisData.extractedData.match_score)">
                <p *ngIf="analysisData.extractedData.match_score >= 70" class="recommendation-text">
                  <strong>Great Match!</strong> You qualify for an interview. Proceed to view interview questions and preparation tips.
                </p>
                <p *ngIf="analysisData.extractedData.match_score < 70" class="recommendation-text">
                  <strong>Fair Match.</strong> Review the feedback section for improvement areas and alternative opportunities.
                </p>
              </div>
            </div>
          </div>

          <!-- Extracted Data Cards -->
          <div class="info-grid">
            <!-- Experience Card -->
            <div class="info-card">
              <h3 class="info-card-title">Experience</h3>
              <p class="info-card-value">{{ analysisData.extractedData.experience_years }} years</p>
              <p class="info-card-hint">Professional experience detected</p>
            </div>

            <!-- Education Card -->
            <div class="info-card">
              <h3 class="info-card-title">Education</h3>
              <p class="info-card-value">{{ analysisData.extractedData.education || 'Not Specified' }}</p>
              <p class="info-card-hint">Highest level detected</p>
            </div>

            <!-- Skills Count Card -->
            <div class="info-card">
              <h3 class="info-card-title">Key Skills</h3>
              <p class="info-card-value">{{ analysisData.extractedData.skills?.length || 0 }}</p>
              <p class="info-card-hint">Skills matched with JD</p>
            </div>
          </div>

          <!-- Skills Section -->
          <div class="section-card">
            <div class="section-header">
              <h2 class="section-title">Key Skills</h2>
              <p class="section-subtitle">Skills we found in your resume</p>
            </div>
            <div class="skills-container">
              <span *ngFor="let skill of analysisData.extractedData.skills" class="skill-badge">
                {{ skill }}
              </span>
              <p *ngIf="!analysisData.extractedData.skills || analysisData.extractedData.skills.length === 0" class="empty-state-text">
                No specific skills were detected in your resume
              </p>
            </div>
          </div>

          <!-- Strengths Section -->
          <div class="section-card" *ngIf="analysisData.extractedData.strengths?.length">
            <div class="section-header">
              <h2 class="section-title">Strengths</h2>
              <p class="section-subtitle">What makes you a strong candidate</p>
            </div>
            <ul class="strength-list">
              <li *ngFor="let strength of analysisData.extractedData.strengths" class="strength-item">
                <span class="strength-icon">✓</span>
                <span class="strength-text">{{ strength }}</span>
              </li>
            </ul>
          </div>

          <!-- Missing Requirements Section -->
          <div class="section-card" *ngIf="analysisData.extractedData.missing_requirements?.length">
            <div class="section-header">
              <h2 class="section-title">Missing Requirements</h2>
              <p class="section-subtitle">What you may need to develop</p>
            </div>
            <ul class="missing-list">
              <li *ngFor="let missing of analysisData.extractedData.missing_requirements" class="missing-item">
                <span class="missing-icon">⚠️</span>
                <span class="missing-text">{{ missing }}</span>
              </li>
            </ul>
          </div>

          <!-- Action Buttons -->
          <div class="action-buttons">
            <button
              *ngIf="analysisData.extractedData.match_score >= 70"
              (click)="goToInterview()"
              class="btn btn-primary btn-large">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                <polyline points="13 2 13 9 20 9"></polyline>
              </svg>
              View Interview Questions
            </button>

            <button
              *ngIf="analysisData.extractedData.match_score < 70"
              (click)="goToFeedback()"
              class="btn btn-warning btn-large">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              View Feedback & Suggestions
            </button>

            <button (click)="goToReport()" class="btn btn-secondary btn-large">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                <polyline points="13 2 13 9 20 9"></polyline>
              </svg>
              View Full Report
            </button>

            <button (click)="uploadAnother()" class="btn btn-outline btn-large">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2v20M2 12h20"></path>
              </svg>
              Analyze Another
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .analysis-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      padding: 2rem 1rem;
    }

    /* Loading State */
    .loading-state {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 2rem;
    }

    .loading-card {
      background: white;
      border-radius: 12px;
      padding: 3rem 2rem;
      max-width: 500px;
      width: 100%;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      text-align: center;
    }

    .loading-animation {
      margin-bottom: 2rem;
    }

    .pulse {
      width: 80px;
      height: 80px;
      margin: 0 auto;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 50%;
      animation: pulse 2s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% {
        transform: scale(1);
        opacity: 1;
      }
      50% {
        transform: scale(1.1);
        opacity: 0.8;
      }
    }

    .loading-title {
      font-size: 1.5rem;
      font-weight: 700;
      margin: 0 0 0.5rem;
      color: #1a1a1a;
    }

    .loading-text {
      color: #666;
      margin: 0 0 2rem;
      font-size: 0.95rem;
    }

    .progress-steps {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      margin-bottom: 2rem;
      text-align: left;
    }

    .step {
      display: flex;
      gap: 1rem;
      align-items: flex-start;
      opacity: 0.4;
      transition: opacity 0.3s;
    }

    .step.active,
    .step.completed {
      opacity: 1;
    }

    .step-indicator {
      width: 32px;
      height: 32px;
      background: #eee;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 600;
      flex-shrink: 0;
      color: #999;
    }

    .step.active .step-indicator {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      animation: pulse-indicator 2s ease-in-out infinite;
    }

    .step.completed .step-indicator {
      background: #22a745;
      color: white;
    }

    @keyframes pulse-indicator {
      0%, 100% { transform: scale(1); }
      50% { transform: scale(1.1); }
    }

    .step-content {
      flex: 1;
    }

    .step-label {
      margin: 0 0 0.25rem;
      font-weight: 600;
      font-size: 0.95rem;
      color: #1a1a1a;
    }

    .step-duration {
      margin: 0;
      font-size: 0.8rem;
      color: #999;
    }

    .progress-bar-container {
      margin-bottom: 1rem;
    }

    .progress-bar {
      width: 100%;
      height: 6px;
      background: #eee;
      border-radius: 3px;
      overflow: hidden;
      margin-bottom: 0.5rem;
    }

    .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
      transition: width 0.3s ease;
    }

    .progress-text {
      margin: 0;
      font-size: 0.8rem;
      color: #999;
      text-align: right;
    }

    .loading-estimate {
      margin: 0;
      font-size: 0.85rem;
      color: #666;
    }

    /* Error State */
    .error-state {
      display: flex;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      padding: 2rem;
    }

    .error-card {
      background: white;
      border-radius: 12px;
      padding: 3rem 2rem;
      max-width: 500px;
      width: 100%;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      text-align: center;
    }

    .error-icon {
      color: #dc3545;
      margin-bottom: 1rem;
      display: flex;
      justify-content: center;
    }

    .error-title {
      font-size: 1.5rem;
      font-weight: 700;
      margin: 0 0 0.5rem;
      color: #1a1a1a;
    }

    .error-message {
      color: #666;
      margin: 0 0 2rem;
      font-size: 0.95rem;
    }

    /* Results View */
    .results-header {
      background: white;
      padding: 2rem;
      border-radius: 12px 12px 0 0;
      margin-bottom: 2rem;
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
    }

    .back-button:hover {
      background: #e8e8e8;
      color: #333;
    }

    .back-button:active {
      transform: scale(0.98);
    }

    .results-title {
      font-size: 2rem;
      font-weight: 700;
      margin: 0 0 0.5rem;
      color: #1a1a1a;
    }

    .results-subtitle {
      margin: 0;
      color: #666;
      font-size: 1rem;
    }

    .results-content {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    /* Score Section */
    .score-section {
      display: flex;
      gap: 2rem;
    }

    .score-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      flex: 1;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .score-header {
      margin-bottom: 2rem;
    }

    .score-label {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .score-hint {
      margin: 0.5rem 0 0;
      font-size: 0.85rem;
      color: #999;
    }

    .score-display {
      display: flex;
      justify-content: center;
      margin-bottom: 2rem;
    }

    .score-circle {
      width: 160px;
      height: 160px;
      border-radius: 50%;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      color: white;
      position: relative;
    }

    .score-circle.excellent {
      background: linear-gradient(135deg, #22a745 0%, #1e7e34 100%);
    }

    .score-circle.good {
      background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
    }

    .score-circle.poor {
      background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
    }

    .score-number {
      font-size: 3rem;
      line-height: 1;
    }

    .score-percent {
      font-size: 1.5rem;
      margin-top: 0.25rem;
    }

    .score-breakdown {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 1rem;
      margin-bottom: 1.5rem;
      padding: 1rem 0;
      border-top: 1px solid #eee;
      border-bottom: 1px solid #eee;
    }

    .breakdown-item {
      text-align: center;
    }

    .breakdown-label {
      display: block;
      font-size: 0.75rem;
      color: #999;
      font-weight: 600;
      margin-bottom: 0.5rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .breakdown-value {
      display: block;
      font-size: 1.5rem;
      font-weight: 700;
      color: #1a1a1a;
    }

    .recommendation {
      padding: 1rem;
      border-radius: 8px;
      text-align: center;
    }

    .recommendation.excellent {
      background: #d4edda;
      color: #155724;
      border: 1px solid #c3e6cb;
    }

    .recommendation.poor {
      background: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
    }

    .recommendation-text {
      margin: 0;
      font-size: 0.95rem;
    }

    /* Info Grid */
    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
    }

    .info-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .info-card-title {
      margin: 0 0 0.75rem;
      font-size: 0.85rem;
      color: #999;
      text-transform: uppercase;
      font-weight: 600;
      letter-spacing: 0.5px;
    }

    .info-card-value {
      margin: 0 0 0.5rem;
      font-size: 1.75rem;
      font-weight: 700;
      color: #1a1a1a;
    }

    .info-card-hint {
      margin: 0;
      font-size: 0.8rem;
      color: #999;
    }

    /* Section Cards */
    .section-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .section-header {
      margin-bottom: 1.5rem;
    }

    .section-title {
      margin: 0;
      font-size: 1.25rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .section-subtitle {
      margin: 0.5rem 0 0;
      font-size: 0.85rem;
      color: #999;
    }

    /* Skills */
    .skills-container {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .skill-badge {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 0.5rem 1rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    /* Lists */
    .strength-list,
    .missing-list {
      list-style: none;
      padding: 0;
      margin: 0;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .strength-item,
    .missing-item {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
      padding: 0.75rem;
      border-radius: 6px;
      background: #fafafa;
    }

    .strength-icon {
      color: #22a745;
      font-weight: bold;
      flex-shrink: 0;
    }

    .missing-icon {
      font-size: 1rem;
      flex-shrink: 0;
    }

    .strength-text,
    .missing-text {
      margin: 0;
      color: #1a1a1a;
      font-size: 0.95rem;
    }

    /* Action Buttons */
    .action-buttons {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }

    .btn {
      border: none;
      border-radius: 6px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      padding: 1rem;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      transition: all 0.2s ease;
      text-decoration: none;
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
      background: #f0f4ff;
    }

    .btn-warning {
      background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
      color: white;
    }

    .btn-warning:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(255, 152, 0, 0.3);
    }

    .btn-outline {
      background: white;
      color: #999;
      border: 2px solid #ddd;
    }

    .btn-outline:hover {
      border-color: #999;
      color: #666;
    }

    .btn-large {
      padding: 1rem 1.5rem;
    }

    .empty-state-text {
      text-align: center;
      color: #999;
      font-size: 0.95rem;
      margin: 1rem 0 0;
    }

    @media (max-width: 768px) {
      .results-header {
        padding: 1.5rem;
        margin-bottom: 1rem;
      }

      .results-title {
        font-size: 1.5rem;
      }

      .score-breakdown {
        grid-template-columns: 1fr;
      }

      .info-grid {
        grid-template-columns: 1fr;
      }

      .action-buttons {
        grid-template-columns: 1fr;
      }

      .score-circle {
        width: 120px;
        height: 120px;
      }

      .score-number {
        font-size: 2rem;
      }

      .score-percent {
        font-size: 1.2rem;
      }
    }

    @media (max-width: 640px) {
      .analysis-container {
        padding: 1rem;
      }

      .loading-card {
        padding: 2rem 1.5rem;
      }

      .results-header {
        padding: 1.25rem;
        border-radius: 12px;
        margin-bottom: 1rem;
      }
    }

    .Math {
      display: none;
    }
  `]
})
export class AnalysisResultComponent implements OnInit, OnDestroy {
  sessionId: string = '';
  analysisData: any = null;
  isLoading = true;
  error: string = '';
  currentStep = 1;
  progress = 0;
  estimatedTime = 70;

  private destroy$ = new Subject<void>();
  Math = Math;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService
  ) {}

  ngOnInit() {
    this.sessionId = this.route.snapshot.paramMap.get('sessionId') || '';

    if (!this.sessionId) {
      this.error = 'Invalid session ID';
      return;
    }

    this.startProgressSimulation();
    this.performAnalysis();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startProgressSimulation() {
    interval(1000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.isLoading) {
          this.progress = Math.min(this.progress + Math.random() * 15, 95);
          this.estimatedTime = Math.max(1, Math.round((100 - this.progress) / 2));

          if (this.progress > 15 && this.currentStep === 1) this.currentStep = 2;
          if (this.progress > 50 && this.currentStep === 2) this.currentStep = 3;
        }
      });
  }

  performAnalysis() {
    this.apiService.getResumePreview(this.sessionId).subscribe({
      next: (preview) => {
        this.apiService.analyzeResume(
          this.sessionId,
          preview.resumeTextPreview,
          preview.jobDescription
        ).subscribe({
          next: (data) => {
            this.analysisData = data;
            this.isLoading = false;
            this.progress = 100;
            this.currentStep = 4;
          },
          error: (err) => {
            this.error = 'Analysis failed: ' + (err.error?.message || err.message);
            this.isLoading = false;
          }
        });
      },
      error: (err) => {
        this.error = 'Failed to retrieve resume: ' + (err.error?.message || err.message);
        this.isLoading = false;
      }
    });
  }

  getScoreClass(score: number): string {
    if (score >= 70) return 'excellent';
    if (score >= 50) return 'good';
    return 'poor';
  }

  getRecommendationClass(score: number): string {
    return score >= 70 ? 'excellent' : 'poor';
  }

  goToInterview() {
    this.router.navigate(['/interview', this.sessionId]);
  }

  goToFeedback() {
    this.router.navigate(['/feedback', this.sessionId]);
  }

  goToReport() {
    this.router.navigate(['/report', this.sessionId]);
  }

  goBack() {
    this.router.navigate(['/upload']);
  }

  uploadAnother() {
    this.router.navigate(['/upload']);
  }
}
