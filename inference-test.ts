import { InferenceProvider, ResponseEvaluation } from './inference-providers';

/**
 * Test Suite: Multi-Model LLM Inference & Evaluation
 * Tests Mistral and Meta Llama models with comprehensive quality metrics
 */

class InferenceTest {
    private provider: InferenceProvider;

    constructor() {
        this.provider = new InferenceProvider();
    }

    /**
     * Test 1: Simple Resume Extraction Query
     */
    async testResumeExtraction() {
        console.log('\n' + '#'.repeat(80));
        console.log('TEST 1: RESUME EXTRACTION WITH MISTRAL');
        console.log('#'.repeat(80));

        const resumeText = `
            Senior Software Engineer with 8 years of experience
            Skills: Java, Python, Docker, Kubernetes, AWS
            Experience:
            - Led microservices architecture migration (3 years)
            - Improved system performance by 40% with optimization
            - Managed team of 5 developers
        `;

        const jobDescription = `
            We are looking for a Senior Backend Engineer with:
            - 5+ years of backend development
            - Experience with microservices
            - Cloud infrastructure knowledge (AWS/GCP)
            - Strong Java/Python skills
        `;

        const prompt = `Analyze this resume against the job description and extract key information.
Resume: ${resumeText}
Job Description: ${jobDescription}
Return a JSON with extracted skills, experience level, and match score.`;

        try {
            const result = await this.provider.callInference(prompt, {
                model: 'mistral',
                temperature: 0.3,
                maxTokens: 1024,
            });

            this.printDetailedEvaluation(result.evaluation);
            return result.evaluation;
        } catch (error) {
            console.error('Test 1 failed:', error);
            throw error;
        }
    }

    /**
     * Test 2: Interview Questions Generation
     */
    async testInterviewQuestions() {
        console.log('\n' + '#'.repeat(80));
        console.log('TEST 2: INTERVIEW QUESTIONS WITH MISTRAL');
        console.log('#'.repeat(80));

        const prompt = `You are a senior hiring manager. Generate 8 interview questions for a Backend Engineer position.

Requirements:
- Mix of technical and behavioral questions
- Difficulty levels: 2 easy, 3 medium, 3 hard
- Include follow-up tips

Return ONLY a JSON object with an array of questions.`;

        try {
            const result = await this.provider.callInference(prompt, {
                model: 'mistral',
                temperature: 0.3,
                maxTokens: 1024,
            });

            this.printDetailedEvaluation(result.evaluation);
            return result.evaluation;
        } catch (error) {
            console.error('Test 2 failed:', error);
            throw error;
        }
    }

    /**
     * Test 3: Rejection Guidance Generation with Llama
     */
    async testRejectionGuidance() {
        console.log('\n' + '#'.repeat(80));
        console.log('TEST 3: REJECTION GUIDANCE WITH META LLAMA');
        console.log('#'.repeat(80));

        const prompt = `You are a compassionate career coach. This candidate scored 55% on a job match (below 70% threshold).

Their missing requirements:
- Only 2 years experience (need 5+)
- No cloud infrastructure experience
- Limited system design knowledge

Provide constructive feedback with improvement suggestions.
Return JSON with rejection_reasons, improvements, alternative_roles, and encouragement.`;

        try {
            const result = await this.provider.callInference(prompt, {
                model: 'llama',
                temperature: 0.3,
                maxTokens: 1024,
            });

            this.printDetailedEvaluation(result.evaluation);
            return result.evaluation;
        } catch (error) {
            console.error('Test 3 failed:', error);
            throw error;
        }
    }

    /**
     * Test 4: Recruiter Summary with Llama
     */
    async testRecruiterSummary() {
        console.log('\n' + '#'.repeat(80));
        console.log('TEST 4: RECRUITER SUMMARY WITH META LLAMA');
        console.log('#'.repeat(80));

        const prompt = `You are a professional recruiter. Write an executive summary for a hiring manager.

Candidate Profile:
- Match Score: 82%
- Skills: Java, Spring Boot, Kubernetes, PostgreSQL, AWS
- Experience: 7 years as backend engineer
- Strengths: Architecture design, team leadership, system optimization

Return JSON with executive_summary, strengths, concerns, recommendation (YES/NO/MAYBE), and next_steps.`;

        try {
            const result = await this.provider.callInference(prompt, {
                model: 'llama',
                temperature: 0.3,
                maxTokens: 1024,
            });

            this.printDetailedEvaluation(result.evaluation);
            return result.evaluation;
        } catch (error) {
            console.error('Test 4 failed:', error);
            throw error;
        }
    }

    /**
     * Test 5: Model Comparison (Mistral vs Llama)
     */
    async testModelComparison() {
        console.log('\n' + '#'.repeat(80));
        console.log('TEST 5: MODEL COMPARISON - MISTRAL VS LLAMA');
        console.log('#'.repeat(80));

        const testPrompt = `What are the top 5 qualities of a great software engineer? List them concisely.`;

        console.log('\n[Testing Mistral...]');
        const mistralResult = await this.provider.callInference(testPrompt, {
            model: 'mistral',
            temperature: 0.3,
            maxTokens: 512,
        });

        console.log('\n[Testing Llama...]');
        const llamaResult = await this.provider.callInference(testPrompt, {
            model: 'llama',
            temperature: 0.3,
            maxTokens: 512,
        });

        this.printComparisonResults(mistralResult.evaluation, llamaResult.evaluation);
        return { mistral: mistralResult.evaluation, llama: llamaResult.evaluation };
    }

    /**
     * Helper: Print detailed evaluation metrics
     */
    private printDetailedEvaluation(evaluation: ResponseEvaluation) {
        console.log('\n' + '='.repeat(80));
        console.log('📊 DETAILED EVALUATION RESULTS');
        console.log('='.repeat(80));

        console.log(`\nOverall Score: ${evaluation.score}/100`);
        console.log(`Quality Rating: ${evaluation.quality}`);
        console.log(`Model: ${evaluation.model}`);
        console.log(`Response Length: ${evaluation.responseLength} chars, ~${evaluation.tokens} tokens`);
        console.log(`Execution Time: ${evaluation.executionTime}ms`);

        console.log(`\nMetric Breakdown:`);
        console.log(`  Accuracy:     ${this.getBar(evaluation.accuracy)} ${evaluation.accuracy}/100`);
        console.log(`  Coherence:    ${this.getBar(evaluation.coherence)} ${evaluation.coherence}/100`);
        console.log(`  Relevance:    ${this.getBar(evaluation.relevance)} ${evaluation.relevance}/100`);
        console.log(`  Factuality:   ${this.getBar(evaluation.factuality)} ${evaluation.factuality}/100`);
        console.log(`  Completeness: ${this.getBar(evaluation.completeness)} ${evaluation.completeness}/100`);

        if (evaluation.strengths.length > 0) {
            console.log(`\n💪 Strengths:`);
            evaluation.strengths.forEach((s) => console.log(`  ✓ ${s}`));
        }

        if (evaluation.weaknesses.length > 0) {
            console.log(`\n⚠️  Weaknesses:`);
            evaluation.weaknesses.forEach((w) => console.log(`  ✗ ${w}`));
        }

        if (evaluation.issues.length > 0) {
            console.log(`\n🔴 Issues:`);
            evaluation.issues.forEach((i) => console.log(`  ⚠ ${i}`));
        }

        console.log('\n' + '='.repeat(80));
    }

    /**
     * Helper: Print model comparison results
     */
    private printComparisonResults(
        mistral: ResponseEvaluation,
        llama: ResponseEvaluation
    ) {
        console.log('\n' + '='.repeat(80));
        console.log('🏆 MODEL COMPARISON RESULTS');
        console.log('='.repeat(80));

        console.log('\nMISTRAL:');
        console.log(`  Overall Score:  ${mistral.score}/100 (${mistral.quality})`);
        console.log(`  Accuracy:       ${mistral.accuracy}/100`);
        console.log(`  Coherence:      ${mistral.coherence}/100`);
        console.log(`  Relevance:      ${mistral.relevance}/100`);
        console.log(`  Factuality:     ${mistral.factuality}/100`);
        console.log(`  Completeness:   ${mistral.completeness}/100`);
        console.log(`  Response Time:  ${mistral.executionTime}ms`);

        console.log('\nLLAMA:');
        console.log(`  Overall Score:  ${llama.score}/100 (${llama.quality})`);
        console.log(`  Accuracy:       ${llama.accuracy}/100`);
        console.log(`  Coherence:      ${llama.coherence}/100`);
        console.log(`  Relevance:      ${llama.relevance}/100`);
        console.log(`  Factuality:     ${llama.factuality}/100`);
        console.log(`  Completeness:   ${llama.completeness}/100`);
        console.log(`  Response Time:  ${llama.executionTime}ms`);

        // Find winner
        const winnerModel = mistral.score > llama.score ? 'MISTRAL' : 'LLAMA';
        const scoreDiff = Math.abs(mistral.score - llama.score);
        console.log(`\n🏅 WINNER: ${winnerModel} 🏆 (by ${scoreDiff} points)`);

        // Category winners
        console.log(`\nCategory Winners:`);
        console.log(`  Accuracy:     ${mistral.accuracy > llama.accuracy ? 'MISTRAL' : 'LLAMA'}`);
        console.log(`  Coherence:    ${mistral.coherence > llama.coherence ? 'MISTRAL' : 'LLAMA'}`);
        console.log(`  Relevance:    ${mistral.relevance > llama.relevance ? 'MISTRAL' : 'LLAMA'}`);
        console.log(`  Factuality:   ${mistral.factuality > llama.factuality ? 'MISTRAL' : 'LLAMA'}`);
        console.log(`  Speed:        ${mistral.executionTime < llama.executionTime ? 'MISTRAL' : 'LLAMA'}`);

        console.log('\n' + '='.repeat(80));
    }

    /**
     * Helper: Create a visual bar for scores
     */
    private getBar(score: number): string {
        const filled = Math.round(score / 10);
        const empty = 10 - filled;
        return '[' + '█'.repeat(filled) + '░'.repeat(empty) + ']';
    }

    /**
     * Run all tests
     */
    async runAllTests() {
        console.log('\n' + '#'.repeat(80));
        console.log('🚀 RUNNING COMPLETE INFERENCE TEST SUITE');
        console.log('#'.repeat(80));

        const results: {
            [key: string]: ResponseEvaluation | { mistral: ResponseEvaluation; llama: ResponseEvaluation };
        } = {};

        try {
            results['resumeExtraction'] = await this.testResumeExtraction();
            results['interviewQuestions'] = await this.testInterviewQuestions();
            results['rejectionGuidance'] = await this.testRejectionGuidance();
            results['recruiterSummary'] = await this.testRecruiterSummary();
            results['modelComparison'] = await this.testModelComparison();

            this.printFinalSummary(results);
        } catch (error) {
            console.error('\n❌ Test suite failed:', error);
            throw error;
        }
    }

    /**
     * Print final test summary
     */
    private printFinalSummary(
        results: {
            [key: string]: ResponseEvaluation | {
                mistral: ResponseEvaluation;
                llama: ResponseEvaluation;
            };
        }
    ) {
        console.log('\n' + '#'.repeat(80));
        console.log('✅ TEST SUITE COMPLETED');
        console.log('#'.repeat(80));

        console.log('\nSummary:');
        console.log(`  ✓ Resume Extraction: ${(results.resumeExtraction as ResponseEvaluation).score}/100`);
        console.log(`  ✓ Interview Questions: ${(results.interviewQuestions as ResponseEvaluation).score}/100`);
        console.log(`  ✓ Rejection Guidance: ${(results.rejectionGuidance as ResponseEvaluation).score}/100`);
        console.log(`  ✓ Recruiter Summary: ${(results.recruiterSummary as ResponseEvaluation).score}/100`);

        const comparison = results.modelComparison as {
            mistral: ResponseEvaluation;
            llama: ResponseEvaluation;
        };
        console.log(`  ✓ Model Comparison: Mistral ${comparison.mistral.score}/100 vs Llama ${comparison.llama.score}/100`);

        console.log('\n' + '#'.repeat(80) + '\n');
    }
}

// Run tests if executed directly
if (require.main === module) {
    const tester = new InferenceTest();
    tester.runAllTests().catch(console.error);
}

export { InferenceTest };
