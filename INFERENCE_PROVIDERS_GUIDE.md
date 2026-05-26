# Inference Providers Configuration Guide

## Overview

This project now uses **HuggingFace Router** to call multiple open-source LLM providers instead of relying on Claude (Anthropic). All responses are evaluated using an internal quality scoring system based on multiple metrics.

## Supported Models & Providers

### Current Configuration

**Resume Extraction (LLM Call 1):**
- Model: `mistralai/Mistral-7B-Instruct-v0.2:featherless-ai` (via HuggingFace Router)
- Provider: Featherless AI / Groq
- Temperature: 0.3 (low - for consistency)
- Max Tokens: 1024

**Interview Questions (LLM Call 2A):**
- Model: `mistralai/Mistral-7B-Instruct-v0.2:featherless-ai` (via HuggingFace Router)
- Provider: Featherless AI / Groq
- Temperature: 0.3
- Max Tokens: 1024

**Rejection Guidance (LLM Call 2B):**
- Model: `mistralai/Mistral-7B-Instruct-v0.2:featherless-ai` (via HuggingFace Router)
- Provider: Featherless AI / Groq
- Temperature: 0.3
- Max Tokens: 1024

**Recruiter Summary (LLM Call 3):**
- Model: `meta-llama/Llama-3.1-8B-Instruct:novita` (via HuggingFace Router)
- Provider: Novita AI
- Temperature: 0.3
- Max Tokens: 1024

## Setup Instructions

### 1. Backend Configuration (Java/Spring Boot)

#### application.yml
```yaml
huggingface:
  api:
    url: https://router.huggingface.co/v1
    key: ${HF_TOKEN}  # Set as environment variable
```

#### Environment Variables
```bash
# Required
export HF_TOKEN="your_huggingface_api_key_here"

# Optional (defaults provided)
export LLM_TEMPERATURE=0.3
export LLM_MAX_TOKENS=1024
```

#### Build & Run
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 2. Frontend Configuration (TypeScript/Node.js)

#### Install Dependencies
```bash
npm install openai
```

#### Environment Setup
```bash
export HF_TOKEN="your_huggingface_api_key_here"
```

#### Usage Example
```typescript
import { InferenceProvider } from './inference-providers';

const provider = new InferenceProvider();

// Call Mistral for resume extraction
const result = await provider.callInference(
  "Analyze this resume: ...",
  {
    model: "mistral",
    temperature: 0.3,
    maxTokens: 1024
  }
);

console.log(`Score: ${result.evaluation.score}/100`);
console.log(`Quality: ${result.evaluation.quality}`);
console.log(`Accuracy: ${result.evaluation.accuracy}/100`);
```

## Response Evaluation Metrics

All LLM responses are automatically evaluated on **5 key metrics**:

### 1. **Accuracy** (0-100)
- Measures factual correctness
- Checks for evidence-based language
- Penalizes unsupported claims
- Looks for citations/sources
- **Target: 85+**

### 2. **Coherence** (0-100)
- Measures logical structure and flow
- Checks for paragraph organization
- Looks for transition words
- Evaluates sentence connectivity
- **Target: 80+**

### 3. **Relevance** (0-100)
- Measures how well response answers the query
- Keyword matching against prompt
- Checks for on-topic content
- Evaluates directness of answer
- **Target: 85+**

### 4. **Factuality** (0-100)
- Measures verifiable facts vs speculation
- Detects hedging language
- Looks for concrete numbers/dates
- Flags unsupported claims
- **Target: 80+**

### 5. **Completeness** (0-100)
- Measures if response fully addresses topic
- Checks minimum length requirements
- Looks for examples and evidence
- Detects truncated responses
- **Target: 80+**

## Quality Ratings

| Score Range | Rating | Meaning |
|------------|--------|---------|
| 85-100 | EXCELLENT | High-quality, reliable response |
| 70-84 | GOOD | Solid response with minor issues |
| 50-69 | ACCEPTABLE | Usable but with notable gaps |
| 0-49 | POOR | Problematic - requires review |

## Log Output Example

```
================================================================================
📊 RESPONSE EVALUATION STARTED
================================================================================
Model: mistralai/Mistral-7B-Instruct-v0.2:featherless-ai
Response Length: 1247 characters
Execution Time: 2341ms
================================================================================

✓ Accuracy: 88/100
✓ Coherence: 82/100
✓ Relevance: 91/100
✓ Factuality: 85/100
✓ Completeness: 79/100

================================================================================
🎯 FINAL EVALUATION RESULTS
================================================================================
Overall Score: 85/100
Quality Rating: EXCELLENT
Model: mistralai/Mistral-7B-Instruct-v0.2:featherless-ai | Tokens: ~1623 | Time: 2341ms

Metric Breakdown:
  • Accuracy:     88/100
  • Coherence:    82/100
  • Relevance:    91/100
  • Factuality:   85/100
  • Completeness: 79/100

💪 STRENGTHS:
  ✓ Highly accurate factual content
  ✓ Well-structured and logically organized
  ✓ Highly relevant to the query

⚠️  WEAKNESSES:
  ✗ Response feels incomplete or truncated

🔴 ISSUES FOUND:
  ⚠ Low completeness (79/100)
================================================================================
```

## API Changes

### Removed
- `ClaudeEvaluator` - No longer using Anthropic Claude
- `ANTHROPIC_API_KEY` environment variable requirement
- Claude-based evaluation endpoints

### Added
- `LLMResponseEvaluator` - New Java evaluation system
- `InferenceProvider` - TypeScript inference wrapper
- Comprehensive evaluation metrics logged for every LLM call

## Troubleshooting

### Issue: "HF_TOKEN not set"
**Solution:** Set the environment variable:
```bash
export HF_TOKEN="your_key_here"
```

### Issue: "401 Unauthorized from HuggingFace"
**Solution:** Verify your HuggingFace API key is valid and active

### Issue: "Model not found"
**Solution:** Check that the model endpoint is available on HuggingFace Router

### Issue: "Response quality too low"
**Solution:** Check the detailed evaluation metrics in logs - they indicate which aspect needs improvement

## Performance Optimization

### Recommended Settings

**For Speed (Real-time responses):**
```
temperature: 0.3
max_tokens: 512
top_p: 0.95
```

**For Quality (Better accuracy):**
```
temperature: 0.1
max_tokens: 1024
top_p: 0.9
```

**Balanced:**
```
temperature: 0.3
max_tokens: 1024
top_p: 0.95
```

## Monitoring Quality

Check the `src/main/resources/logback-spring.xml` for detailed logging:

```xml
<!-- Enable detailed evaluation logs -->
<logger name="com.resumescreener.util.LLMResponseEvaluator" level="INFO"/>
```

### Key Metrics to Monitor

1. **Average Score**: Should be 70+ for production
2. **Quality Distribution**: Track percentage of EXCELLENT/GOOD ratings
3. **Execution Time**: Monitor for performance degradation
4. **Error Rate**: Track failed evaluations
5. **Model Consistency**: Compare scores across models

## Switching Models

To use different models, modify:

**Java Backend (AIOrchestrationService.java):**
```java
private static final String MODEL_EXTRACTION = "new-model:provider";
private static final String MODEL_INTERVIEW = "new-model:provider";
private static final String MODEL_SUMMARY = "new-model:provider";
```

**TypeScript Frontend (inference-providers.ts):**
```typescript
private modelMap = {
  mistral: "new-model-here",
  llama: "new-model-here",
  custom: "new-model-here"
};
```

## API Rate Limiting

HuggingFace Router typically has:
- Free tier: Limited concurrent requests
- Pro tier: Higher limits
- Enterprise: Custom limits

Configure timeouts in application.yml:
```yaml
spring:
  mvc:
    async:
      request-timeout: 30000
```

## Cost Analysis

HuggingFace Router charges based on:
- Inference API tokens used
- Model provider pricing
- Concurrent request limits

Monitor usage in your HuggingFace dashboard.

## Future Enhancements

- [ ] Support for GPT-4 via OpenAI Router
- [ ] Model-specific prompt optimization
- [ ] Caching layer for repeated queries
- [ ] A/B testing framework for models
- [ ] Custom fine-tuned models support
- [ ] Multi-model ensemble voting

## Support & Resources

- [HuggingFace Router Docs](https://huggingface.co/docs/inference-api/router)
- [OpenAI Python/Node SDK](https://github.com/openai/openai-python)
- [Mistral AI Documentation](https://docs.mistral.ai/)
- [Meta Llama Information](https://www.llama.com/)

---

**Note:** All Claude/Anthropic references have been removed. The system now operates independently using open-source models and internal evaluation metrics.
