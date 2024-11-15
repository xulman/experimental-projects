package org.mastodon.benchmark.measurements;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
		return streamOfValidOnly().reduce(0.0, Double::sum);
	}

	public double getMin() {
		Optional<Double> m = streamOfValidOnly().min(Double::compareTo);
		return m.isPresent() ? m.get() : 0;
	}

	public double getMax() {
		Optional<Double> m = streamOfValidOnly().max(Double::compareTo);
		return m.isPresent() ? m.get() : 0;
	}

	public double getAvg() {
		int size = numOfValidOnly();
		if (size == 0) return 0;

		double sum = streamOfValidOnly().reduce(0.0, Double::sum);
		sum /= size;
		return sum;
	}

	public double getMedian() {
		int size = numOfValidOnly();
		if (size == 0) return 0;

		return streamOfValidOnly().sorted().collect(Collectors.toList()).get(size / 2);
	}
}
