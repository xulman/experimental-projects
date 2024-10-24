package org.mastodon.benchmark.windows;

import org.mastodon.benchmark.BenchmarkSetup;

/**
 * Common API to govern a benchmark action that consists of several consecutive steps.
 * When such action is started in the main benchmark loop {@link BenchmarkSetup#executeInstructions(String, long, boolean)},
 * the loop, when starting another round, would not advance in processing of its instructions but
 * would rather be calling {@link MultipleStepsCommand#doNext()} as long as {@link MultipleStepsCommand#hasNext()}.
 * Only after all steps of this multiple-step action are executed, the main loop would proceed to next
 * benchmarking instruction.
 */
public interface MultipleStepsCommand {
	boolean hasNext();
	void doNext();
	String reportCurrentStep();
}
