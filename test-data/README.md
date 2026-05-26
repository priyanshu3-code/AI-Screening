# Test Data for Resume Screener

## Overview

Two pairs of resume and job description files for testing the resume screener with different scenarios.

---

## Test Scenarios

### 1. High Match (Positive Scenario) ✅

**Resume**: `sample_resume_high_match.txt`
**JD**: `sample_jd_high_match.txt`

**Profile**:
- Candidate: Senior Full-Stack Engineer
- Experience: 8 years (exceeds 7+ requirement)
- Skills: Java, Spring Boot, Angular, Microservices, REST API, PostgreSQL, Docker, Kubernetes (8 major skills)
- Education: BS Computer Science
- Achievements: 12+ strong accomplishments
- Leadership: Mentored 5 junior engineers

**Expected Result**:
- Match Score: **85-90** (≥70 threshold)
- Route: **Interview Questions** (positive path)
- Questions: 8-10 technical questions generated
- Recruiter Summary: Positive recommendation

**Test Steps**:
1. Upload: `sample_resume_high_match.txt`
2. Paste: `sample_jd_high_match.txt`
3. Check logs: `Match Score: 87`, `SOURCE: LLM`
4. Verify: 8+ interview questions in response

---

### 2. Low Match (Negative Scenario) ❌

**Resume**: `sample_resume_low_match.txt`
**JD**: `sample_jd_low_match.txt`

**Profile**:
- Candidate: Junior Frontend Developer
- Experience: 2 years (way below 10+ requirement)
- Skills: JavaScript, HTML, CSS, jQuery, Bootstrap (only 5 skills, mostly frontend)
- Missing: Spring Boot, Java, Kubernetes, microservices, backend experience
- Education: High School Diploma (no CS degree)
- Achievements: 5 static websites, WordPress maintenance

**Expected Result**:
- Match Score: **15-30** (<70 threshold)
- Route: **Rejection Guidance** (negative path)
- Rejection Reasons: Experience mismatch, skill gaps
- Improvements: Suggestions for skill development
- Alternative Roles: Junior frontend engineer positions

**Test Steps**:
1. Upload: `sample_resume_low_match.txt`
2. Paste: `sample_jd_low_match.txt`
3. Check logs: `Match Score: 20`, `SOURCE: LLM`
4. Verify: Rejection guidance (not interview questions)
5. Check: Improvement suggestions in response

---

## File Descriptions

| File | Description | Use Case |
|------|-------------|----------|
| `sample_resume_high_match.txt` | Senior engineer, 8 years exp, all required skills | Positive scenario (interview path) |
| `sample_jd_high_match.txt` | Senior role, 7+ years, matching tech stack | Positive scenario |
| `sample_resume_low_match.txt` | Junior dev, 2 years exp, missing key skills | Negative scenario (rejection path) |
| `sample_jd_low_match.txt` | Senior role, 10+ years, extensive requirements | Negative scenario |

---

## Quick Testing Guide

### Test 1: Positive Path (Interview Questions)
```
Resume: sample_resume_high_match.txt
JD: sample_jd_high_match.txt
Expected Score: 87+
Expected Path: Interview Questions → Recruiter Summary
```

### Test 2: Negative Path (Rejection Guidance)
```
Resume: sample_resume_low_match.txt
JD: sample_jd_low_match.txt
Expected Score: 20-30
Expected Path: Rejection Guidance → Recruiter Summary (optional)
```

---

## How Scoring Works

### Match Score Calculation

```
0-40 points: Skills match (required skills present)
0-30 points: Experience match (years of experience vs requirement)
0-20 points: Tech stack match (technology overlap)
0-10 points: Education + Achievements (degree level + accomplishments)
```

**High Match (87)**:
- Skills: +40 (8/8 present)
- Experience: +30 (8 years > 7 required)
- Tech stack: +20 (exact match)
- Education: +10 (BS degree + achievements)
- **Total: 100 (capped at 87 for realistic scoring)**

**Low Match (25)**:
- Skills: +10 (2/12 present, no backend skills)
- Experience: +0 (2 years < 10 required)
- Tech stack: +5 (minimal overlap)
- Education: +0 (high school diploma, no CS)
- **Total: 15-25**

---

## Routing Logic

```
Match Score >= 70
├─ YES → Interview Questions (LLM Call 2A)
│        └─ Then: Recruiter Summary (LLM Call 3)
│
└─ NO → Rejection Guidance (LLM Call 2B)
        └─ Then: Recruiter Summary (LLM Call 3)
```

---

## Expected Log Output

### High Match (Positive)
```
LLM Call 1 completed in 23s | Match Score: 87 | SOURCE: LLM
Score 87 >= 70%, generating interview questions (LLM Call 2A)
LLM Call 2A completed in 36s | Questions: 8 | SOURCE: LLM
LLM Call 3 completed in 4s | SOURCE: LLM
```

### Low Match (Negative)
```
LLM Call 1 completed in 23s | Match Score: 25 | SOURCE: LLM
Score 25 < 70%, generating rejection guidance (LLM Call 2B)
LLM Call 2B completed in 10s | SOURCE: LLM
LLM Call 3 completed in 4s | SOURCE: LLM
```

---

## Testing Checklist

- [ ] High-match resume parses without JsonSyntaxException
- [ ] High-match score is 85+, routes to interview questions
- [ ] Low-match resume parses without JsonSyntaxException
- [ ] Low-match score is 25-35, routes to rejection guidance
- [ ] Both responses show "SOURCE: LLM" (not mock data fallback)
- [ ] Interview questions array has 8+ items for high-match
- [ ] Rejection guidance array has improvements for low-match
- [ ] Recruiter summary generated for both scenarios
