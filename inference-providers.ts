import { OpenAI } from "openai";

/**
 * Multi-Model Inference Provider using HuggingFace Router
 * Supports: Mistral, Meta Llama, and other open-source models
 * Completely replaces Claude with evaluations based on LLM quality metrics
 */

interface ResponseEvaluation {
	accuracy: number; // 0-100: How factually correct is the response?
	coherence: number; // 0-100: How logically structured and understandable?
	relevance: number; // 0-100: How well does it answer the query?
	factuality: number; // 0-100: Based on verifiable facts?
	responseLength: number; // Character count
	tokens: number; // Estimated token count
	completeness: number; // 0-100: Is the response complete?
	quality: "EXCELLENT" | "GOOD" | "ACCEPTABLE" | "POOR"; // Overall quality rating
	score: number; // Final score 0-100
	strengths: string[];
	weaknesses: string[];
	issues: string[];
	executionTime: number; // milliseconds
	model: string;
}

interface InferenceConfig {
	model: "mistral" | "llama" | "custom";
	temperature?: number;
	maxTokens?: number;
	topP?: number;
}

class InferenceProvider {
	private client: OpenAI;
	private modelMap = {
		mistral: "mistralai/Mistral-7B-Instruct-v0.2:groq",
		llama: "meta-llama/Llama-3.1-8B-Instruct:together",
		custom: "openai/gpt-oss-120b:groq",
	};

	constructor() {
		this.client = new OpenAI({
			baseURL: "https://router.huggingface.co/v1",
			apiKey: process.env.HF_TOKEN || "",
		});
	}

	/**
	 * Call LLM inference with a specific model
	 */
	async callInference(
		prompt: string,
		config: InferenceConfig
	): Promise<{ response: string; evaluation: ResponseEvaluation }> {
		const startTime = Date.now();
		const model = this.modelMap[config.model] || config.model;

		console.log(`\n${"=".repeat(80)}`);
		console.log(`🚀 INFERENCE CALL INITIATED`);
		console.log(`${"=".repeat(80)}`);
		console.log(`Model: ${model}`);
		console.log(`Temperature: ${config.temperature ?? 0.3}`);
		console.log(`Max Tokens: ${config.maxTokens ?? 1024}`);
		console.log(`Top P: ${config.topP ?? 0.95}`);
		console.log(`${"=".repeat(80)}\n`);

		try {
			const chatCompletion = await this.client.chat.completions.create({
				model,
				messages: [
					{
						role: "user",
						content: prompt,
					},
				],
				temperature: config.temperature ?? 0.3,
				max_tokens: config.maxTokens ?? 1024,
				top_p: config.topP ?? 0.95,
			});

			const response =
				chatCompletion.choices[0]?.message?.content || "";
			const executionTime = Date.now() - startTime;

			console.log(`\n📝 RESPONSE RECEIVED`);
			console.log(`${"=".repeat(80)}`);
			console.log(`Response:\n${response}`);
			console.log(`${"=".repeat(80)}\n`);

			const evaluation = this.evaluateResponse(
				response,
				prompt,
				model,
				executionTime
			);

			return { response, evaluation };
		} catch (error) {
			const executionTime = Date.now() - startTime;
			console.error(`\n❌ INFERENCE FAILED`);
			console.error(`Error: ${error}`);
			console.error(`Execution Time: ${executionTime}ms\n`);
			throw error;
		}
	}

	/**
	 * Evaluate response quality based on multiple criteria
	 */
	private evaluateResponse(
		response: string,
		prompt: string,
		model: string,
		executionTime: number
	): ResponseEvaluation {
		const evaluation: ResponseEvaluation = {
			accuracy: 0,
			coherence: 0,
			relevance: 0,
			factuality: 0,
			responseLength: response.length,
			tokens: Math.ceil(response.split(/\s+/).length * 1.3), // Rough estimate
			completeness: 0,
			quality: "ACCEPTABLE",
			score: 0,
			strengths: [],
			weaknesses: [],
			issues: [],
			executionTime,
			model,
		};

		console.log(`\n📊 RESPONSE EVALUATION`);
		console.log(`${"=".repeat(80)}`);

		// 1. ACCURACY EVALUATION (0-100)
		const accuracyScore = this.evaluateAccuracy(response, prompt);
		evaluation.accuracy = accuracyScore;
		console.log(`✓ Accuracy: ${accuracyScore}/100`);

		if (accuracyScore >= 85) {
			evaluation.strengths.push(
				"Highly accurate factual content"
			);
		} else if (accuracyScore < 60) {
			evaluation.weaknesses.push(
				"Contains potentially inaccurate information"
			);
			evaluation.issues.push("Low accuracy score");
		}

		// 2. COHERENCE EVALUATION (0-100)
		const coherenceScore = this.evaluateCoherence(response);
		evaluation.coherence = coherenceScore;
		console.log(`✓ Coherence: ${coherenceScore}/100`);

		if (coherenceScore >= 80) {
			evaluation.strengths.push("Well-structured and logically organized");
		} else if (coherenceScore < 60) {
			evaluation.weaknesses.push("Lacks clear logical structure");
			evaluation.issues.push("Poor coherence");
		}

		// 3. RELEVANCE EVALUATION (0-100)
		const relevanceScore = this.evaluateRelevance(response, prompt);
		evaluation.relevance = relevanceScore;
		console.log(`✓ Relevance: ${relevanceScore}/100`);

		if (relevanceScore >= 85) {
			evaluation.strengths.push("Highly relevant to the query");
		} else if (relevanceScore < 60) {
			evaluation.weaknesses.push("Response does not adequately address query");
			evaluation.issues.push("Low relevance");
		}

		// 4. FACTUALITY EVALUATION (0-100)
		const factualityScore = this.evaluateFactuality(response);
		evaluation.factuality = factualityScore;
		console.log(`✓ Factuality: ${factualityScore}/100`);

		if (factualityScore >= 80) {
			evaluation.strengths.push(
				"Based on verifiable facts and evidence"
			);
		} else if (factualityScore < 60) {
			evaluation.weaknesses.push(
				"Contains unverified claims or speculation"
			);
			evaluation.issues.push("Low factuality");
		}

		// 5. COMPLETENESS EVALUATION (0-100)
		const completenessScore = this.evaluateCompleteness(
			response,
			prompt
		);
		evaluation.completeness = completenessScore;
		console.log(`✓ Completeness: ${completenessScore}/100`);

		if (completenessScore >= 80) {
			evaluation.strengths.push("Comprehensive and complete response");
		} else if (completenessScore < 60) {
			evaluation.weaknesses.push("Response feels incomplete or truncated");
			evaluation.issues.push("Low completeness");
		}

		// CALCULATE FINAL SCORE (average of all metrics)
		evaluation.score = Math.round(
			(evaluation.accuracy +
				evaluation.coherence +
				evaluation.relevance +
				evaluation.factuality +
				evaluation.completeness) /
				5
		);

		// DETERMINE QUALITY RATING
		if (evaluation.score >= 85) {
			evaluation.quality = "EXCELLENT";
		} else if (evaluation.score >= 70) {
			evaluation.quality = "GOOD";
		} else if (evaluation.score >= 50) {
			evaluation.quality = "ACCEPTABLE";
		} else {
			evaluation.quality = "POOR";
		}

		// LOG FINAL EVALUATION
		console.log(`${"=".repeat(80)}`);
		console.log(`\n🎯 FINAL SCORE: ${evaluation.score}/100`);
		console.log(`📈 Quality Rating: ${evaluation.quality}`);
		console.log(`⏱️  Execution Time: ${executionTime}ms`);
		console.log(`📏 Response Length: ${response.length} characters, ~${evaluation.tokens} tokens`);

		console.log(`\n💪 STRENGTHS:`);
		evaluation.strengths.forEach((s) => console.log(`  • ${s}`));

		if (evaluation.weaknesses.length > 0) {
			console.log(`\n⚠️  WEAKNESSES:`);
			evaluation.weaknesses.forEach((w) => console.log(`  • ${w}`));
		}

		if (evaluation.issues.length > 0) {
			console.log(`\n🔴 ISSUES FOUND:`);
			evaluation.issues.forEach((i) => console.log(`  • ${i}`));
		}

		console.log(`${"=".repeat(80)}\n`);

		return evaluation;
	}

	/**
	 * ACCURACY: Check for factual correctness and known facts
	 */
	private evaluateAccuracy(response: string, prompt: string): number {
		let score = 70; // Start with baseline

		// Check for definitive claims backed by evidence
		const evidencePatterns = [
			/according to|research shows|studies indicate|evidence suggests|data shows/i,
		];
		const hasEvidence = evidencePatterns.some((p) =>
			p.test(response)
		);
		if (hasEvidence) score += 15;

		// Check for common inaccuracy indicators
		const inaccuracyPatterns = [
			/i'm not sure|i don't know|i cannot verify|unverified claim/i,
		];
		const hasUncertainty = inaccuracyPatterns.some((p) =>
			p.test(response)
		);
		if (hasUncertainty) score -= 10;

		// Length check - too short might indicate incomplete info
		if (response.length < 100) score -= 5;

		// Check for citations or sources
		if (/\(source:|sources:|references:|citation/i.test(response))
			score += 10;

		return Math.min(100, Math.max(0, score));
	}

	/**
	 * COHERENCE: Check logical flow and structure
	 */
	private evaluateCoherence(response: string): number {
		let score = 70; // Baseline

		// Check for proper paragraph structure
		const paragraphs = response
			.split(/\n\n+/)
			.filter((p) => p.trim().length > 0);
		if (paragraphs.length >= 2) score += 10;

		// Check for transition words
		const transitions = [
			/therefore|however|furthermore|moreover|in addition|as a result|consequently/i,
		];
		const transitionCount = transitions.filter((t) =>
			t.test(response)
		).length;
		if (transitionCount >= 2) score += 10;

		// Check for bulleted or numbered lists
		if (/^[\s]*[-•*\d.]\s+/m.test(response)) score += 5;

		// Check for logical sentence connection
		const sentences = response.split(/[.!?]+/).filter((s) => s.trim());
		if (sentences.length >= 5) score += 5;

		// Check for topic coherence - no sudden jumps
		if (this.hasLogicalFlow(response)) score += 5;

		return Math.min(100, Math.max(0, score));
	}

	/**
	 * RELEVANCE: Check if response answers the question
	 */
	private evaluateRelevance(response: string, prompt: string): number {
		let score = 70; // Baseline

		// Extract key terms from prompt
		const promptKeywords = this.extractKeywords(prompt);
		const responseText = response.toLowerCase();

		// Count keyword matches
		const keywordMatches = promptKeywords.filter((kw) =>
			responseText.includes(kw.toLowerCase())
		).length;
		const keywordRatio = keywordMatches / (promptKeywords.length || 1);

		if (keywordRatio >= 0.7) score += 20;
		else if (keywordRatio >= 0.5) score += 10;
		else if (keywordRatio >= 0.3) score += 5;
		else score -= 10;

		// Check if response directly addresses the query
		if (
			/^(yes|no|true|false|correct|incorrect)/i.test(response)
		)
			score += 5;

		// Check for off-topic content
		if (this.isOffTopic(response, prompt)) score -= 15;

		return Math.min(100, Math.max(0, score));
	}

	/**
	 * FACTUALITY: Check for verifiable facts vs. speculation
	 */
	private evaluateFactuality(response: string): number {
		let score = 70; // Baseline

		// Check for hedging language (speculation indicators)
		const hedges = [
			/might|may|could|possibly|perhaps|seems|appears|suggests|may indicate/i,
		];
		const hedgeCount = hedges.filter((h) =>
			h.test(response)
		).length;

		if (hedgeCount > 5) score -= 15;
		else if (hedgeCount > 2) score -= 5;

		// Check for specific numbers/dates (factual indicators)
		if (
			/\d{4}|\d{1,2}[\/\-]\d{1,2}[\/\-]\d{2,4}|\d+\s*(million|billion|thousand|percent|%)/i.test(
				response
			)
		)
			score += 10;

		// Check for qualified statements
		if (/according to|research indicates|studies show|evidence suggests/i.test(
			response
		))
			score += 5;

		// Check for common misconceptions
		if (this.containsMisconceptions(response)) score -= 20;

		return Math.min(100, Math.max(0, score));
	}

	/**
	 * COMPLETENESS: Check if response fully addresses the topic
	 */
	private evaluateCompleteness(response: string, prompt: string): number {
		let score = 70; // Baseline

		// Check minimum length
		if (response.length < 100) score -= 20;
		else if (response.length < 500) score -= 5;
		else if (response.length >= 1000) score += 10;

		// Check if response covers multiple aspects
		const aspects = response.split(/[.!?]/).length;
		if (aspects >= 10) score += 10;

		// Check for conclusion/summary
		if (/in conclusion|to summarize|in summary|finally/i.test(response))
			score += 5;

		// Check if answer is cut off
		if (
			response.endsWith("...") ||
			response.endsWith("[truncated]") ||
			response.endsWith("cont")
		)
			score -= 15;

		// Check for examples
		if (/for example|for instance|such as|like/i.test(response))
			score += 5;

		return Math.min(100, Math.max(0, score));
	}

	/**
	 * Helper: Extract key terms from prompt
	 */
	private extractKeywords(text: string): string[] {
		return text
			.toLowerCase()
			.split(/\s+/)
			.filter((word) => word.length > 4)
			.slice(0, 10);
	}

	/**
	 * Helper: Check if response has logical flow
	 */
	private hasLogicalFlow(text: string): boolean {
		const sentences = text.split(/[.!?]+/).filter((s) => s.trim());
		if (sentences.length < 2) return false;

		// Very basic check for connected ideas
		return sentences.some((s, i) => {
			if (i === 0) return true;
			const current = s.toLowerCase();
			const previous = sentences[i - 1].toLowerCase();
			return (
				current.includes("this") ||
				current.includes("that") ||
				current.includes("therefore") ||
				previous.includes(
					current.split(" ")[0]
				)
			);
		});
	}

	/**
	 * Helper: Check if response is off-topic
	 */
	private isOffTopic(response: string, prompt: string): boolean {
		const promptKeywords = this.extractKeywords(prompt);
		const responseText = response.toLowerCase();
		const matchCount = promptKeywords.filter((kw) =>
			responseText.includes(kw.toLowerCase())
		).length;
		return matchCount < promptKeywords.length * 0.2;
	}

	/**
	 * Helper: Detect common misconceptions
	 */
	private containsMisconceptions(text: string): boolean {
		const misconceptions = [
			/the earth is flat/i,
			/vaccines cause autism/i,
			/climate change is a hoax/i,
		];
		return misconceptions.some((m) => m.test(text));
	}
}

/**
 * Example Usage: Test Inference with Multiple Models
 */
async function exampleUsage() {
	const provider = new InferenceProvider();

	const testQuery =
		"What is the capital of France and explain its historical significance?";

	console.log(`\n${"#".repeat(80)}`);
	console.log(`# TESTING MISTRAL MODEL`);
	console.log(`${"#".repeat(80)}\n`);

	try {
		const mistralResult = await provider.callInference(testQuery, {
			model: "mistral",
			temperature: 0.3,
			maxTokens: 512,
		});

		console.log(`\n${"#".repeat(80)}`);
		console.log(`# TESTING META LLAMA MODEL`);
		console.log(`${"#".repeat(80)}\n`);

		const llamaResult = await provider.callInference(testQuery, {
			model: "llama",
			temperature: 0.3,
			maxTokens: 512,
		});

		// Comparison Summary
		console.log(`\n${"#".repeat(80)}`);
		console.log(`# COMPARISON SUMMARY`);
		console.log(`${"#".repeat(80)}\n`);

		console.log(`MISTRAL:`);
		console.log(
			`  Score: ${mistralResult.evaluation.score}/100 (${mistralResult.evaluation.quality})`
		);
		console.log(`  Accuracy: ${mistralResult.evaluation.accuracy}/100`);
		console.log(
			`  Relevance: ${mistralResult.evaluation.relevance}/100`
		);
		console.log(`  Coherence: ${mistralResult.evaluation.coherence}/100`);
		console.log(`  Execution Time: ${mistralResult.evaluation.executionTime}ms`);

		console.log(`\nLLAMA:`);
		console.log(
			`  Score: ${llamaResult.evaluation.score}/100 (${llamaResult.evaluation.quality})`
		);
		console.log(`  Accuracy: ${llamaResult.evaluation.accuracy}/100`);
		console.log(
			`  Relevance: ${llamaResult.evaluation.relevance}/100`
		);
		console.log(`  Coherence: ${llamaResult.evaluation.coherence}/100`);
		console.log(`  Execution Time: ${llamaResult.evaluation.executionTime}ms`);

		const winner =
			mistralResult.evaluation.score >=
			llamaResult.evaluation.score
				? "MISTRAL 🏆"
				: "LLAMA 🏆";
		console.log(`\n🏅 WINNER: ${winner}\n`);
	} catch (error) {
		console.error("Error during inference:", error);
	}
}

// Export for use in other modules
export { InferenceProvider, ResponseEvaluation, InferenceConfig };

// Run example if executed directly
if (require.main === module) {
	exampleUsage().catch(console.error);
}
