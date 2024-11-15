package org.mastodon.benchmark.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BenchmarkMeasurement {

	public final String sourceName;
	public final List<Double> measuredTimes;

	public BenchmarkMeasurement(final String sourceName) {
		this.sourceName = sourceName;
		this.measuredTimes = new ArrayList<>(500);
	}

	/** Enlists the given 'value' to {@link org.mastodon.benchmark.measurements.BenchmarkMeasurement#measuredTimes}
	    to end up at index 'toPosition'. */
	public void add(double value, int toPosition) {
		while (measuredTimes.size() < toPosition) measuredTimes.add(null);
		measuredTimes.add(value);
	}

	public Stream<Double> streamOfValidOnly() {
		return measuredTimes.stream().filter(Objects::nonNull);
	}

	public int numOfValidOnly() {
		return (int)measuredTimes.stream().filter(Objects::nonNull).count();
	}

	public double getSum() {
		return measuredTimes.stream().reduce(0.0, Double::sum);
	}

	public double getMin() {
		if (measuredTimes.isEmpty()) return 0;
		return measuredTimes.stream().min(Double::compareTo).get();
	}

	public double getMax() {
		if (measuredTimes.isEmpty()) return 0;
		return measuredTimes.stream().max(Double::compareTo).get();
	}

	public double getAvg() {
		if (measuredTimes.isEmpty()) return 0;
		double sum = measuredTimes.stream().reduce(0.0, Double::sum);
		sum /= measuredTimes.size();
		return sum;
	}

	public double getMedian() {
		if (measuredTimes.isEmpty()) return 0;
		return measuredTimes.stream().sorted().collect(Collectors.toList()).get(measuredTimes.size() / 2);
	}
}
