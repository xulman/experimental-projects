package org.mastodon.benchmark.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BenchmarkMeasurement {

	public final String sourceName;
	public final List<Double> measuredTimes;

	public BenchmarkMeasurement(final String sourceName) {
		this.sourceName = sourceName;
		this.measuredTimes = new ArrayList<>(500);
	}

	public double getMin() {
		return measuredTimes.stream().min(Double::compareTo).get();
	}

	public double getMax() {
		return measuredTimes.stream().max(Double::compareTo).get();
	}

	public double getAvg() {
		double sum = measuredTimes.stream().reduce(0.0, Double::sum);
		sum /= measuredTimes.size();
		return sum;
	}

	public double getMedian() {
		return measuredTimes.stream().sorted().collect(Collectors.toList()).get(measuredTimes.size() / 2);
	}
}
