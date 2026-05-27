import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="upload-container">
      <!-- Header Section -->
      <div class="header-section">
        <div class="header-content">
          <h1 class="page-title">Resume Screener</h1>
          <p class="page-subtitle">Upload your resume and job description for instant AI-powered analysis</p>
        </div>
      </div>

      <!-- Main Content -->
      <div class="content-wrapper">
        <div class="upload-card">
          <!-- Form Section -->
          <form (ngSubmit)="onSubmit()" class="upload-form">

            <!-- Resume Upload Section -->
            <div class="form-section">
              <div class="form-header">
                <label class="form-label">
                  <span class="label-text">Resume File</span>
                  <span class="label-required">*</span>
                </label>
                <p class="form-hint">PDF, TXT, DOC, or DOCX • Max 10MB</p>
              </div>

              <div
                class="upload-area"
                [class.dragover]="isDragOver"
                (dragover)="onDragOver($event)"
                (dragleave)="onDragLeave($event)"
                (drop)="onDrop($event)"
                (click)="fileInput.click()">

                <div class="upload-icon" *ngIf="!selectedFile">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                </div>

                <div class="upload-content">
                  <p *ngIf="!selectedFile" class="upload-text">
                    <strong>Drag and drop your resume</strong> or <span class="upload-link">click to browse</span>
                  </p>
                  <p *ngIf="selectedFile" class="selected-file">
                    <span class="file-icon">📄</span>
                    <span class="file-name">{{ selectedFile.name }}</span>
                    <button type="button" (click)="clearFile($event)" class="clear-btn">✕</button>
                  </p>
                </div>

                <input
                  #fileInput
                  type="file"
                  class="file-input"
                  (change)="onFileSelected($event)"
                  accept=".pdf,.txt,.doc,.docx"
                  [disabled]="isLoading">
              </div>

              <div *ngIf="fileError" class="form-error">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                {{ fileError }}
              </div>
            </div>

            <!-- Job Description Section -->
            <div class="form-section">
              <div class="form-header">
                <label class="form-label">
                  <span class="label-text">Job Description</span>
                  <span class="label-required">*</span>
                </label>
                <p class="form-hint">50-50,000 characters</p>
              </div>

              <textarea
                class="form-textarea"
                [(ngModel)]="jobDescription"
                name="jobDescription"
                placeholder="Paste the job description here. Include requirements, responsibilities, and preferred qualifications..."
                rows="8"
                [disabled]="isLoading"
                (blur)="validateJobDescription()">
              </textarea>

              <div class="char-counter" [class.warning]="jobDescription.length > 40000">
                {{ jobDescription.length }} / 50,000 characters
              </div>

              <div *ngIf="jobDescriptionError" class="form-error">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
                {{ jobDescriptionError }}
              </div>
            </div>

            <!-- Submit Button -->
            <button
              type="submit"
              class="btn btn-primary btn-large"
              [disabled]="!isFormValid() || isLoading">
              <span *ngIf="!isLoading" class="btn-content">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5c0-1.105.895-2 2-2s2 .895 2 2m0 0h4"></path>
                </svg>
                Analyze Resume
              </span>
              <span *ngIf="isLoading" class="btn-content">
                <span class="spinner"></span>
                Analyzing...
              </span>
            </button>

            <!-- Error Message -->
            <div *ngIf="errorMessage" class="alert alert-error">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
              </svg>
              <div>
                <strong>Error</strong>
                <p>{{ errorMessage }}</p>
              </div>
            </div>
          </form>

          <!-- Info Section -->
          <div class="info-section">
            <h3 class="info-title">How it works</h3>
            <div class="info-grid">
              <div class="info-item">
                <div class="info-number">1</div>
                <h4>Upload & Compare</h4>
                <p>Our AI analyzes your resume against the job description</p>
              </div>
              <div class="info-item">
                <div class="info-number">2</div>
                <h4>Get Instant Score</h4>
                <p>See your match percentage and key insights</p>
              </div>
              <div class="info-item">
                <div class="info-number">3</div>
                <h4>Interview Ready</h4>
                <p>Get tailored questions or improvement suggestions</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }

    .upload-container {
      min-height: 100vh;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      display: flex;
      flex-direction: column;
    }

    .header-section {
      padding: 3rem 1rem;
      text-align: center;
      color: white;
    }

    .header-content {
      max-width: 800px;
      margin: 0 auto;
    }

    .page-title {
      font-size: 2.5rem;
      font-weight: 700;
      margin: 0 0 0.5rem;
      letter-spacing: -0.02em;
    }

    .page-subtitle {
      font-size: 1.125rem;
      margin: 0;
      opacity: 0.95;
      font-weight: 500;
    }

    .content-wrapper {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem 3rem;
    }

    .upload-card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
      max-width: 700px;
      width: 100%;
      padding: 2.5rem;
    }

    .upload-form {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .form-section {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .form-header {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .form-label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-weight: 600;
      font-size: 0.95rem;
      color: #1a1a1a;
    }

    .label-required {
      color: #dc3545;
    }

    .form-hint {
      margin: 0;
      font-size: 0.8rem;
      color: #666;
      font-weight: 400;
    }

    .upload-area {
      border: 2px dashed #ddd;
      border-radius: 8px;
      padding: 2rem;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s ease;
      background: #fafafa;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      position: relative;
    }

    .upload-area:hover {
      border-color: #667eea;
      background: #f5f7ff;
    }

    .upload-area.dragover {
      border-color: #667eea;
      background: #f0f4ff;
      transform: scale(1.02);
    }

    .upload-icon {
      color: #667eea;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .upload-content {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .upload-text {
      margin: 0;
      font-size: 0.95rem;
      color: #1a1a1a;
    }

    .upload-link {
      color: #667eea;
      font-weight: 600;
      text-decoration: underline;
    }

    .selected-file {
      margin: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      font-size: 0.95rem;
      color: #22a745;
      font-weight: 500;
    }

    .file-icon {
      font-size: 1.5rem;
    }

    .file-name {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 400px;
    }

    .clear-btn {
      background: none;
      border: none;
      color: #dc3545;
      cursor: pointer;
      font-size: 1.25rem;
      padding: 0;
      line-height: 1;
      flex-shrink: 0;
    }

    .file-input {
      display: none;
    }

    .form-textarea {
      border: 1px solid #ddd;
      border-radius: 6px;
      padding: 0.75rem;
      font-size: 0.95rem;
      font-family: inherit;
      resize: vertical;
      transition: border-color 0.2s, box-shadow 0.2s;
    }

    .form-textarea:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .form-textarea:disabled {
      background: #f5f5f5;
      cursor: not-allowed;
    }

    .char-counter {
      font-size: 0.8rem;
      color: #666;
      text-align: right;
    }

    .char-counter.warning {
      color: #ff9800;
      font-weight: 600;
    }

    .form-error {
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      padding: 0.75rem;
      background: #ffebee;
      border-radius: 6px;
      color: #c62828;
      font-size: 0.85rem;
    }

    .form-error svg {
      flex-shrink: 0;
      margin-top: 0.1rem;
    }

    .btn {
      border: none;
      border-radius: 6px;
      font-size: 0.95rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      padding: 0.75rem 1.5rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-large {
      padding: 1rem 2rem;
      font-size: 1rem;
      width: 100%;
    }

    .btn-content {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
    }

    .spinner {
      width: 18px;
      height: 18px;
      border: 3px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .alert {
      padding: 1rem;
      border-radius: 6px;
      display: flex;
      gap: 0.75rem;
    }

    .alert-error {
      background: #ffebee;
      color: #c62828;
      border: 1px solid #ef5350;
    }

    .alert-error strong {
      display: block;
      font-weight: 600;
      margin-bottom: 0.25rem;
    }

    .alert-error p {
      margin: 0;
      font-size: 0.9rem;
    }

    .info-section {
      margin-top: 2rem;
      padding-top: 2rem;
      border-top: 1px solid #eee;
    }

    .info-title {
      margin: 0 0 1.5rem;
      font-size: 1rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1.5rem;
    }

    .info-item {
      text-align: center;
    }

    .info-number {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      margin: 0 auto 0.75rem;
    }

    .info-item h4 {
      margin: 0 0 0.5rem;
      font-size: 0.95rem;
      font-weight: 600;
      color: #1a1a1a;
    }

    .info-item p {
      margin: 0;
      font-size: 0.8rem;
      color: #666;
      line-height: 1.5;
    }

    @media (max-width: 640px) {
      .upload-container {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      .header-section {
        padding: 2rem 1rem;
      }

      .page-title {
        font-size: 1.75rem;
      }

      .page-subtitle {
        font-size: 0.95rem;
      }

      .upload-card {
        padding: 1.5rem;
      }

      .upload-area {
        padding: 1.5rem;
      }

      .form-section {
        gap: 0.5rem;
      }

      .info-grid {
        grid-template-columns: 1fr;
      }

      .file-name {
        max-width: 200px;
      }
    }
  `]
})
export class UploadComponent {
  selectedFile: File | null = null;
  jobDescription = '';
  isLoading = false;
  errorMessage = '';
  fileError = '';
  jobDescriptionError = '';
  isDragOver = false;

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  onFileSelected(event: any) {
    this.selectedFile = event.target.files[0];
    this.fileError = '';
    this.errorMessage = '';
    this.validateFile();
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.selectedFile = files[0];
      this.fileError = '';
      this.errorMessage = '';
      this.validateFile();
    }
  }

  clearFile(event: Event) {
    event.stopPropagation();
    this.selectedFile = null;
    this.fileError = '';
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }

  validateFile() {
    if (!this.selectedFile) return;

    const maxSize = 10 * 1024 * 1024; // 10MB
    const allowedTypes = ['application/pdf', 'text/plain', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];

    if (this.selectedFile.size > maxSize) {
      this.fileError = 'File size exceeds 10MB limit';
      this.selectedFile = null;
      return;
    }

    if (!allowedTypes.includes(this.selectedFile.type)) {
      this.fileError = 'Only PDF, TXT, DOC, and DOCX files are allowed';
      this.selectedFile = null;
      return;
    }

    this.fileError = '';
  }

  validateJobDescription() {
    if (this.jobDescription.length < 50) {
      this.jobDescriptionError = 'Job description must be at least 50 characters';
    } else if (this.jobDescription.length > 50000) {
      this.jobDescriptionError = 'Job description cannot exceed 50,000 characters';
    } else {
      this.jobDescriptionError = '';
    }
  }

  isFormValid(): boolean {
    const hasFile = this.selectedFile !== null;
    const hasValidJobDescription = this.jobDescription.length >= 50 && this.jobDescription.length <= 50000;
    return hasFile && hasValidJobDescription && !this.fileError && !this.jobDescriptionError;
  }

  onSubmit() {
    if (!this.isFormValid()) {
      this.errorMessage = 'Please fill in all required fields correctly';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.apiService.uploadResume(this.selectedFile!, this.jobDescription).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.router.navigate(['/analysis', response.sessionId]);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'Upload failed. Please try again.';
        console.error('Upload error:', error);
      }
    });
  }
}
