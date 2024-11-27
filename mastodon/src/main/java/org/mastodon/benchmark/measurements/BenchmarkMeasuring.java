package org.mastodon.benchmark.measurements;

import bdv.tools.benchmarks.TimeReporter;
import org.mastodon.benchmark.BenchmarkLanguage;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class BenchmarkMeasuring {
	public BenchmarkMeasuring(final int maxRounds,
	                          final List<MamutViewTrackScheme> tsWindows,
	                          final List<MamutViewBdv> bdvWindows) {
		//just allocate all necessary pieces of memory....
		measurements = new HashMap<>(maxRounds);
		for (int round = 1; round <= maxRounds; ++round) {
			Map<String,BenchmarkMeasurement> sourcesMap = new HashMap<>(3+tsWindows.size()+ bdvWindows.size());
			measurements.put(round, sourcesMap);

			sourcesMap.put(MEASURING_STATS_CATEGORY_COMMANDS, new BenchmarkMeasurement(MEASURING_STATS_CATEGORY_COMMANDS));
			sourcesMap.put(MEASURING_STATS_CATEGORY_TS_WINDOWS, new BenchmarkMeasurement(MEASURING_STATS_CATEGORY_TS_WINDOWS));
			sourcesMap.put(MEASURING_STATS_CATEGORY_BDV_WINDOWS, new BenchmarkMeasurement(MEASURING_STATS_CATEGORY_BDV_WINDOWS));
			sourcesMap.put(MEASURING_STATS_CATEGORY_CURR_TIMEPOINT, new BenchmarkMeasurement(MEASURING_STATS_CATEGORY_CURR_TIMEPOINT));
			sourcesMap.put(MEASURING_STATS_CATEGORY_SPOTS_IN_TIMEPOINT, new BenchmarkMeasurement(MEASURING_STATS_CATEGORY_SPOTS_IN_TIMEPOINT));
			tsWindows.forEach(win -> {
				String winName = win.getFrame().getTrackschemePanel().getDisplay().getDisplayName();
				sourcesMap.put(winName, new BenchmarkMeasurement(winName));
			});
			bdvWindows.forEach(win -> {
				String winName = win.getViewerPanelMamut().getDisplay().getDisplayName();
				sourcesMap.put(winName, new BenchmarkMeasurement(winName));
			});
		}
	}


	private final static String MEASURING_STATS_CATEGORY_COMMANDS = "Avg per command";
	private final static String MEASURING_STATS_CATEGORY_TS_WINDOWS = "Avg per TrackSchemes";
	private final static String MEASURING_STATS_CATEGORY_BDV_WINDOWS = "Avg per BigDataViewers";
	private final static String MEASURING_STATS_CATEGORY_CURR_TIMEPOINT = "Current time point";
	private final static String MEASURING_STATS_CATEGORY_SPOTS_IN_TIMEPOINT = "Spots in this time point";

	//Map< round, Map<source,Measurement> >
	//where Measurement is source(as String) and List<times(as doubles)>
	private final Map<Integer, Map<String,BenchmarkMeasurement>> measurements;
	int currentRound = 1; //NB 1-based counter (besides, it's an _index_ to a map, _not an offset_ to a buffer...)

	public void nextRound() {
		currentRound++;
		//let the header build again, it will build the same one as before (because we're repeating the same benchmark rounds),
		//and we need it grow (not stay on its final size) to know where to currently position incoming time measurements/values
		tableHeader.clear();
	}

	private final List<String> tableHeader = new ArrayList<>(300);

	public void recordMeasurements(final Map<String,Integer> expectingNowTheseWindowNames,
	                               final BenchmarkLanguage tokenizer) {
		final Map<String, BenchmarkMeasurement> stats = measurements.get(currentRound);
		double perCommand_windowsTimesSum = 0.0;
		int perCommand_windowsCount = 0;
		double perTS_windowsTimesSum = 0.0;
		int perTS_windowsCount = 0;
		double perBDV_windowsTimesSum = 0.0;
		int perBDV_windowsCount = 0;

		final int tableColumn = tableHeader.size();
		tableHeader.add(tokenizer.getCurrentToken());

		final TimeReporter times = TimeReporter.getInstance();
		for (String windowName : times.observedTimes.keySet()) {
			if (!expectingNowTheseWindowNames.containsKey(windowName)) {
				//recorded an unexpected window!
				throw new RuntimeException("Mastodon Benchmark:\nDuring the command "
						  +tokenizer.getCurrentToken()+", a window '"+windowName+"' executed "
						  +times.observedTimes.get(windowName).size()+" repaint events, expected was 0."
						  +" Don't work with Mastodon during the benchmark.");
			} else if (times.observedTimes.get(windowName).size() != 1) {
				//recorded correctly an expected window, but more than once!
				throw new RuntimeException("Mastodon Benchmark:\nDuring the command "
						  +tokenizer.getCurrentToken()+", a window '"+windowName+"' executed "
						  +times.observedTimes.get(windowName).size()+" repaint events, expected was 1."
						  +" Don't press keys, don't move mouse during the benchmark.");
			}
			//recorded correctly an expected window exactly once, mark it as "enlisted in the stats" ;)
			expectingNowTheseWindowNames.put(windowName, 0);

			//"enlist in the stats"
			for (double time : times.observedTimes.get(windowName)) {
				//per window, per type of window (BDV vs TS), totals per command
				//System.out.println(windowName+" needed "+time+" ms");
				stats.get(windowName).add(time, tableColumn);
				if (windowName.startsWith("BenchBDV")) {
					perBDV_windowsTimesSum += time;
					perBDV_windowsCount++;
				} else if (windowName.startsWith("BenchTS")) {
					perTS_windowsTimesSum += time;
					perTS_windowsCount++;
				}
				perCommand_windowsTimesSum += time;
				perCommand_windowsCount++;
			}
		}

		if (perTS_windowsCount == 0) perTS_windowsCount = 1; //NB: keeps the avg time to 0.0 anyway
		stats.get(MEASURING_STATS_CATEGORY_TS_WINDOWS).add( perTS_windowsTimesSum / (double)perTS_windowsCount, tableColumn );
		if (perBDV_windowsCount == 0) perBDV_windowsCount = 1; //NB: keeps the avg time to 0.0 anyway
		stats.get(MEASURING_STATS_CATEGORY_BDV_WINDOWS).add( perBDV_windowsTimesSum / (double)perBDV_windowsCount, tableColumn );
		if (perCommand_windowsCount == 0) perCommand_windowsCount = 1; //NB: keeps the avg time to 0.0 anyway
		stats.get(MEASURING_STATS_CATEGORY_COMMANDS).add( perCommand_windowsTimesSum / (double)perCommand_windowsCount, tableColumn );

		//check if there are some unmarked windows?
		for (String windowName : expectingNowTheseWindowNames.keySet()) {
			if (expectingNowTheseWindowNames.get(windowName) != 0) {
				throw new RuntimeException("Mastodon Benchmark:\nDuring the command "
						  +tokenizer.getCurrentToken()+", a measurement for window '"+windowName
						  +"' hasn't been recorded! Increase waiting times, perhaps.");
			}
		}
	}

	public void recordMeasurements(int timepoint, int numOfSpots) {
		final Map<String, BenchmarkMeasurement> stats = measurements.get(currentRound);
		final int tableColumn = tableHeader.size()-1;
		stats.get(MEASURING_STATS_CATEGORY_CURR_TIMEPOINT).add(timepoint, tableColumn);
		stats.get(MEASURING_STATS_CATEGORY_SPOTS_IN_TIMEPOINT).add(numOfSpots, tableColumn);
	}


	public void exportMeasurementsToHorizontalCsv(final String pathToCSV) {
		exportMeasurementsToHorizontalCsv(pathToCSV,null);
	}

	public void exportMeasurementsToHorizontalCsv(final String pathToCSV, final String optionalExtraInfo) {
		//collect all available sources
		final Set<String> sources = measurements //NB: to have sources sorted
				  .values()
				  .stream()
				  .flatMap(s -> s.keySet().stream())
				  .collect(Collectors.toCollection(TreeSet::new));

		try (PrintWriter writer = new PrintWriter(pathToCSV))
		{
			//writer.print("# Benchmarked: "); writer.println(LocalDateTime.now());
			//writer.print("# Columns: source\tround\tmin\tmax\tavg\tmedian\tindividual times in seconds");
			writer.print("source\tround\ttotal time\tmin\tmax\tavg\tavg FPS\tmedian");
			for (String cmd : tableHeader) writer.print("\t"+cmd);
			//if (optionalExtraInfo != null) writer.print(optionalExtraInfo);
			writer.println();

			for (String source : sources) {
				for (int round : measurements.keySet()) {
					if (!measurements.get(round).containsKey(source)) continue;
					final BenchmarkMeasurement stats = measurements.get(round).get(source);
					writer.print(stats.sourceName+"\t"+round);
					writer.print("\t"+f(stats.getSum()));
					writer.print("\t"+f(stats.getMin()));
					writer.print("\t"+f(stats.getMax()));
					double avgTime = stats.getAvg();
					writer.print("\t"+f(avgTime));
					writer.print("\t"+f1(avgTime > 0.0 ? 1.0/avgTime : 0.0));
					writer.print("\t"+f(stats.getMedian()));
					stats.measuredTimes.forEach( t -> writer.print("\t"+f(t)) );
					writer.println();
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Reading file error: "+e.getMessage());
		}
	}


	/** f = format the number */
	public static String f(Double val) {
		return val == null ? "" : String.format("%.5f", val);
	}

	public static String f1(double val) {
		return String.format("%.1f", val);
	}
}
