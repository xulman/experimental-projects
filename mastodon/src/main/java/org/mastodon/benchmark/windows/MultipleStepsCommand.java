package org.mastodon.benchmark.windows;

public interface MultipleStepsCommand {
	boolean hasNext();
	void doNext();
}
