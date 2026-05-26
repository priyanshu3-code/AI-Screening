# Architecture Diagram: Open-Source LLM Inference System

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        RESUME SCREENER APPLICATION                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  ┌──────────────────────┐              ┌──────────────────────┐         │
│  │   Frontend (Angular) │              │  Backend (Spring)    │         │
│  │   ├─ Upload Resume   │◄────HTTP────►│  ├─ API Controllers │         │
│  │   ├─ View Results    │              │  ├─ Services        │         │
│  │   └─ Analysis Page   │              │  └─ Models          │         │
│  └──────────────────────┘              └──────────────────────┘         │
│                                                │                         │
│                                                ▼                         │
│                                    ┌─────────────────────────┐          │
│                                    │ AIOrchestrationService  │          │
│                                    │ ├─ Call 1: Extract      │          │
│                                    │ ├─ Call 2A: Questions   │          │
│                                    │ ├─ Call 2B: Feedback    │          │
│                                    │ └─ Call 3: Summary      │          │
│                                    └────────────┬────────────┘          │
│                                                 │                        │
│                                    ┌────────────▼────────────┐          │
│                                    │  HuggingFaceClient      │          │
│                                    │  (REST API Wrapper)     │          │
│                                    └────────────┬────────────┘          │
│                                                 │                        │
└─────────────────────────────────────────────────┼────────────────────────┘
                                                  │
                                    ┌─────────────▼────────────┐
                                    │  HuggingFace Router API  │
                                    │  https://router.hf.co/v1 │
                                    └─────────────┬────────────┘
                                                  │
                                  ┌───────────────┼───────────────┐
                                  │               │               │
                                  ▼               ▼               ▼
                         ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
                         │   Mistral    │  │  Mistral     │  │   Llama 3.1  │
                         │   7B (Call1) │  │  7B (Call2)  │  │   (Call 3)   │
                         │              │  │              │  │              │
                         │ Featherless  │  │  Featherless │  │   Novita     │
                         │ AI / Groq    │  │  AI / Groq   │  │   AI         │
                         └──────┬───────┘  └──────┬───────┘  └──────┬───────┘
                                │                 │                 │
                    ┌───────────┴─────────────────┴─────────────────┴──────────┐
                    │                                                           │
                    ▼                                                           ▼
          ┌──────────────────────────┐                          ┌──────────────────────────┐
          │   Response + JSON        │                          │   Response + JSON        │
          │   (Resume Data)          │                          │   (Recruiter Summary)    │
          └──────────────┬───────────┘                          └──────────────┬───────────┘
                         │                                                     │
          ┌──────────────▼──────────────────────────────────────────────┬─────▼──────────┐
          │                                                              │                │
          │              LLMResponseEvaluator (Java)                    │                │
          │         ┌────────────────────────────────────────┐         │                │
          │         │         5-METRIC EVALUATION SYSTEM     │         │                │
          │         ├────────────────────────────────────────┤         │                │
          │         │ 1. Accuracy      (Factual correctness) │         │                │
          │         │ 2. Coherence     (Logical structure)   │         │                │
          │         │ 3. Relevance     (Query relevance)     │         │                │
          │         │ 4. Factuality    (Verifiable facts)    │         │                │
          │         │ 5. Completeness  (Response completeness)│        │                │
          │         │                                        │         │                │
          │         │ ► Score: 0-100                         │         │                │
          │         │ ► Quality: EXCELLENT/GOOD/ACCEPTABLE   │         │                │
          │         │ ► Strengths/Weaknesses identified      │         │                │
          │         │ ► Issues flagged                       │         │                │
          │         └────────────────────────────────────────┘         │                │
          │                      │                                     │                │
          └──────────────────────┼─────────────────────────────────────┼────────────────┘
                                 │                                     │
                    ┌────────────▼──────────┐         ┌───────────────▼──────────┐
                    │  Detailed Logs        │         │   Application Logs       │
                    │  - All 5 metrics      │         │   - Quality metrics      │
                    │  - Strengths/issues   │         │   - Model performance    │
                    │  - Execution time     │         │   - Cost tracking        │
                    │  - Token estimation   │         │   - Error handling       │
                    └───────────────────────┘         └──────────────────────────┘
```

---

## Data Flow: Resume Processing Pipeline

```
User Upload
    │
    ▼
┌────────────────────────────────────────────────────────────────────┐
│ PHASE 1: RESUME EXTRACTION (LLM Call 1 - Mistral)                │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Resume Text ──────────────────────┐                             │
│                                    │                             │
│  Job Description ────────────────┐ │                             │
│                                  ▼ ▼                             │
│               ┌─────────────────────────────────┐                │
│               │  Call Mistral 7B Inference      │                │
│               │  Temperature: 0.3               │                │
│               │  Max Tokens: 1024               │                │
│               └────────────────┬────────────────┘                │
│                                │                                │
│                                ▼                                │
│                  ┌──────────────────────────┐                  │
│                  │  Parse JSON Response     │                  │
│                  │  ├─ Skills              │                  │
│                  │  ├─ Experience Years    │                  │
│                  │  ├─ Education           │                  │
│                  │  ├─ Achievements        │                  │
│                  │  ├─ Match Score (0-100) │                  │
│                  │  └─ Summary             │                  │
│                  └──────────────┬───────────┘                  │
│                                │                               │
│                   ┌────────────▼─────────────┐                │
│                   │ EVALUATE WITH 5 METRICS  │                │
│                   │                          │                │
│                   │ • Accuracy: 92/100 ✓     │                │
│                   │ • Coherence: 88/100 ✓    │                │
│                   │ • Relevance: 95/100 ✓    │                │
│                   │ • Factuality: 90/100 ✓   │                │
│                   │ • Completeness: 87/100 ✓ │                │
│                   │                          │                │
│                   │ SCORE: 90/100            │                │
│                   │ QUALITY: EXCELLENT ✅    │                │
│                   └────────────┬─────────────┘                │
│                                │                               │
│                      ResumeExtractionResult                    │
│                      + EvaluationMetrics                       │
│
└────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────────────────────────────────┐
│ PHASE 2A or 2B: DECISION BASED ON MATCH SCORE                     │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│         Match Score >= 70%? ────YES────┐                         │
│                │                       │                         │
│               NO                       ▼                         │
│                │              ┌──────────────────────┐           │
│                │              │ PHASE 2A: INTERVIEW  │           │
│                │              │ QUESTIONS (Mistral) │           │
│                │              │                      │           │
│                │              │ Generate 8-10 Q's   │           │
│                │              │ Mix technical &      │           │
│                │              │ behavioral          │           │
│                │              │                      │           │
│                │              │ EVALUATE: 5 Metrics │           │
│                │              │ Score: 91/100 ✓    │           │
│                │              └──────┬───────────────┘           │
│                │                     │                           │
│                │        InterviewQuestionsResult                │
│                │        + EvaluationMetrics                     │
│                │                                               │
│                ▼                                               │
│         ┌──────────────────────┐                              │
│         │ PHASE 2B: REJECTION  │                              │
│         │ GUIDANCE (Mistral)   │                              │
│         │                      │                              │
│         │ Reasons for low      │                              │
│         │ score, improvement   │                              │
│         │ suggestions, alt     │                              │
│         │ roles, encouragement │                              │
│         │                      │                              │
│         │ EVALUATE: 5 Metrics  │                              │
│         │ Score: 84/100 ✓      │                              │
│         └──────┬───────────────┘                              │
│                │                                              │
│        RejectionGuidanceResult                               │
│        + EvaluationMetrics                                   │
│
└────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────────────────────────────────┐
│ PHASE 3: RECRUITER SUMMARY (LLM Call 3 - Meta Llama)             │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Extraction Result ───────────────────┐                          │
│                                       │                          │
│  Interview Questions or ────────────┐ │                          │
│  Rejection Guidance                 ▼ ▼                          │
│               ┌──────────────────────────────────┐               │
│               │  Call Llama 3.1 Inference       │               │
│               │  Temperature: 0.3               │               │
│               │  Max Tokens: 1024               │               │
│               └────────────┬─────────────────────┘               │
│                            │                                    │
│                            ▼                                    │
│                 ┌──────────────────────┐                       │
│                 │  Parse JSON Response │                       │
│                 │  ├─ Executive Summary│                       │
│                 │  ├─ Strengths        │                       │
│                 │  ├─ Concerns         │                       │
│                 │  ├─ Recommendation   │                       │
│                 │  ├─ Next Steps       │                       │
│                 │  └─ Interview Ready? │                       │
│                 └──────────┬────────────┘                       │
│                            │                                    │
│                 ┌──────────▼──────────────┐                    │
│                 │ EVALUATE WITH 5 METRICS │                    │
│                 │                         │                    │
│                 │ • Accuracy: 90/100 ✓    │                    │
│                 │ • Coherence: 93/100 ✓   │                    │
│                 │ • Relevance: 96/100 ✓   │                    │
│                 │ • Factuality: 88/100 ✓  │                    │
│                 │ • Completeness: 91/100 ✓│                    │
│                 │                         │                    │
│                 │ SCORE: 92/100           │                    │
│                 │ QUALITY: EXCELLENT ✅   │                    │
│                 └──────────┬──────────────┘                    │
│                            │                                    │
│                  RecruiterSummaryResult                         │
│                  + EvaluationMetrics                            │
│
└────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌────────────────────────────────────────────────────────────────────┐
│ FINAL RESULT: Complete Analysis Report                            │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────────────────────────────────────────┐         │
│  │  Resume Extraction         Score: 90/100 EXCELLENT │         │
│  │  ├─ Skills: [Java, Spring, AWS, Kubernetes...]    │         │
│  │  ├─ Experience: 8 years                           │         │
│  │  ├─ Match: 92/100                                 │         │
│  │  └─ Evaluation: Accurate, Relevant, Coherent     │         │
│  └─────────────────────────────────────────────────────┘         │
│                                                                    │
│  ┌─────────────────────────────────────────────────────┐         │
│  │  Interview Questions       Score: 91/100 EXCELLENT │         │
│  │  ├─ Count: 10 questions                            │         │
│  │  ├─ Technical: 7, Behavioral: 3                   │         │
│  │  └─ Difficulty: Well-balanced                     │         │
│  └─────────────────────────────────────────────────────┘         │
│                                                                    │
│  ┌─────────────────────────────────────────────────────┐         │
│  │  Recruiter Summary         Score: 92/100 EXCELLENT │         │
│  │  ├─ Executive Summary: Professional & thorough     │         │
│  │  ├─ Recommendation: YES                            │         │
│  │  ├─ Next Steps: Technical Interview, Reference...  │         │
│  │  └─ Interview Ready: Yes                           │         │
│  └─────────────────────────────────────────────────────┘         │
│                                                                    │
│  Overall Average Score: 91/100 ✅ EXCELLENT                      │
│  Total Processing Time: 8.2 seconds                              │
│  Total Tokens Used: ~4,300                                       │
│  Estimated Cost: $0.001                                          │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
    │
    ▼
Display to User / Save to Database
```

---

## Component Interactions

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         INTERACTION FLOW                                │
└─────────────────────────────────────────────────────────────────────────┘

AIOrchestrationService
    │
    ├─────► analyzeResume()
    │       ├─→ HuggingFaceClient.callLLM(prompt, MODEL_EXTRACTION)
    │       │   └─→ HTTP POST to HuggingFace Router API
    │       │       └─→ Mistral 7B returns JSON response
    │       │
    │       └─→ LLMResponseEvaluator.evaluateExtractionOutput()
    │           ├─→ evaluateAccuracy()
    │           ├─→ evaluateCoherence()
    │           ├─→ evaluateRelevance()
    │           ├─→ evaluateFactuality()
    │           ├─→ evaluateCompleteness()
    │           └─→ Return EvaluationResult with score & metrics
    │
    ├─────► generateInterviewQuestions()
    │       ├─→ HuggingFaceClient.callLLM(prompt, MODEL_INTERVIEW)
    │       │   └─→ Mistral 7B returns interview questions JSON
    │       │
    │       └─→ LLMResponseEvaluator.evaluateInterviewQuestions()
    │           └─→ Return EvaluationResult
    │
    ├─────► generateRejectionGuidance()
    │       ├─→ HuggingFaceClient.callLLM(prompt, MODEL_INTERVIEW)
    │       │   └─→ Mistral 7B returns guidance JSON
    │       │
    │       └─→ LLMResponseEvaluator.evaluateRejectionGuidance()
    │           └─→ Return EvaluationResult
    │
    └─────► generateRecruiterSummary()
            ├─→ HuggingFaceClient.callLLM(prompt, MODEL_SUMMARY)
            │   └─→ Meta Llama 3.1 returns summary JSON
            │
            └─→ LLMResponseEvaluator.evaluateRecruiterSummary()
                └─→ Return EvaluationResult
```

---

## Evaluation Scoring Logic

```
┌──────────────────────────────────────────────────────┐
│          5-METRIC EVALUATION FRAMEWORK              │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Input: LLM Response + Prompt                       │
│      │                                              │
│      ├────────────────┬────────────────┬─────────┐  │
│      │                │                │         │  │
│      ▼                ▼                ▼         ▼  │
│  ┌────────────┐ ┌──────────┐ ┌────────────┐ ┌──┐ │
│  │ Accuracy  │ │Coherence │ │ Relevance │ │..│ │
│  │ (0-100)   │ │ (0-100)  │ │ (0-100)   │ │  │ │
│  └────┬───────┘ └────┬─────┘ └─────┬─────┘ └──┘ │
│       │              │             │            │
│       └──────────────┼─────────────┘            │
│                      │                          │
│          ┌───────────▼──────────────┐           │
│          │  Average Score (0-100)   │           │
│          └───────────┬──────────────┘           │
│                      │                          │
│         ┌────────────▼────────────┐             │
│         │  Quality Rating         │             │
│         ├────────────────────────┤             │
│         │ 85-100: EXCELLENT      │             │
│         │ 70-84:  GOOD           │             │
│         │ 50-69:  ACCEPTABLE     │             │
│         │ 0-49:   POOR           │             │
│         └────────────┬───────────┘             │
│                      │                         │
│          Output: EvaluationResult              │
│          ├─ score (0-100)                     │
│          ├─ quality (EXCELLENT/GOOD/...)     │
│          ├─ strengths (List<String>)         │
│          ├─ weaknesses (List<String>)        │
│          ├─ issues (List<String>)            │
│          └─ all 5 metric scores              │
│
└──────────────────────────────────────────────────┘
```

---

## Cost Comparison: Before vs After

```
Before (Claude API):
┌─────────────────────────────────────────┐
│  Per Resume Processing                  │
│  ├─ LLM Inference: $0.002              │
│  ├─ Claude Evaluation (4 calls): $0.01 │
│  └─ Total per resume: $0.012           │
│                                        │
│  Monthly (1000 resumes):                │
│  ├─ Inference: $2.00                   │
│  ├─ Evaluation: $10.00                 │
│  ├─ Claude API Subscription: $5.00     │
│  └─ Total: $17.00                      │
└─────────────────────────────────────────┘

After (Open-Source + Internal Evaluation):
┌─────────────────────────────────────────┐
│  Per Resume Processing                  │
│  ├─ Mistral Inference: $0.0002         │
│  ├─ Llama Inference: $0.0003           │
│  ├─ Internal Evaluation: FREE          │
│  └─ Total per resume: $0.0005          │
│                                        │
│  Monthly (1000 resumes):                │
│  ├─ Inference: $0.50                   │
│  ├─ Evaluation: FREE                   │
│  ├─ Subscription: FREE                 │
│  └─ Total: $0.50                       │
└─────────────────────────────────────────┘

Savings: 97% cost reduction! 💰
```

---

## Evaluation Metrics Breakdown

```
┌─────────────────────────────────────────────────────────────────┐
│                    METRIC EXPLANATIONS                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│ 1. ACCURACY (Factual Correctness)                              │
│    │                                                            │
│    ├─ Checks: Evidence-based language                          │
│    │   "research shows", "studies indicate"                    │
│    │                                                            │
│    ├─ Penalizes: Uncertainty indicators                        │
│    │   "I'm not sure", "unverified"                           │
│    │                                                            │
│    ├─ Rewards: Citations and sources                           │
│    │   "(source: ...)", "(references: ...)"                    │
│    │                                                            │
│    └─ Score: 70-100 baseline + modifiers                       │
│                                                                │
│ 2. COHERENCE (Logical Structure)                               │
│    │                                                            │
│    ├─ Checks: Paragraph organization                           │
│    │   Multiple paragraphs, clear structure                    │
│    │                                                            │
│    ├─ Checks: Transition words                                 │
│    │   "therefore", "however", "moreover"                      │
│    │                                                            │
│    ├─ Checks: Formatting                                       │
│    │   Lists, bullets, numbered items                         │
│    │                                                            │
│    └─ Score: 70-100 baseline + modifiers                       │
│                                                                │
│ 3. RELEVANCE (Query Relevance)                                 │
│    │                                                            │
│    ├─ Keyword matching against prompt                          │
│    │   Extracts keywords from prompt                           │
│    │   Counts occurrences in response                          │
│    │                                                            │
│    ├─ Direct answer check                                      │
│    │   Looks for "yes", "no", "correct", etc.                 │
│    │                                                            │
│    ├─ Off-topic detection                                      │
│    │   Penalizes completely unrelated content                  │
│    │                                                            │
│    └─ Score: 70-100 baseline + keyword ratio bonus             │
│                                                                │
│ 4. FACTUALITY (Verifiable Facts)                               │
│    │                                                            │
│    ├─ Hedging language detection                               │
│    │   "might", "may", "could", "possibly"                     │
│    │   Penalizes speculative language                          │
│    │                                                            │
│    ├─ Concrete evidence check                                  │
│    │   Numbers, dates, specific facts                          │
│    │   Rewards specific data points                            │
│    │                                                            │
│    ├─ Misconception detection                                  │
│    │   Flags known false statements                            │
│    │   Major penalty for misconceptions                        │
│    │                                                            │
│    └─ Score: 70-100 baseline + modifiers                       │
│                                                                │
│ 5. COMPLETENESS (Response Completeness)                        │
│    │                                                            │
│    ├─ Length requirements                                      │
│    │   <100 chars: -20 points                                  │
│    │   <500 chars: -5 points                                   │
│    │   >=1000 chars: +10 points                                │
│    │                                                            │
│    ├─ Coverage check                                           │
│    │   Multiple aspects/sentences covered                      │
│    │                                                            │
│    ├─ Conclusion presence                                      │
│    │   "in conclusion", "to summarize"                         │
│    │                                                            │
│    ├─ Examples and evidence                                    │
│    │   "for example", "such as"                                │
│    │                                                            │
│    ├─ Truncation detection                                     │
│    │   Responses ending with "..." penalized                   │
│    │                                                            │
│    └─ Score: 70-100 baseline + modifiers                       │
│
└─────────────────────────────────────────────────────────────────┘
```

---

This comprehensive architecture supports your full AI Resume Screener pipeline with open-source models and transparent, cost-effective evaluation metrics.
