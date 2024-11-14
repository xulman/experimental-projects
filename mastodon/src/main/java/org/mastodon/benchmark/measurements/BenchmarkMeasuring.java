package org.mastodon.benchmark.measurements;

import bdv.tools.benchmarks.TimeReporter;
import org.mastodon.benchmark.BenchmarkLanguage;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


	private final static String MEASURING_STATS_CATEGORY_COMMANDS = "Per command";
	private final static String MEASURING_STATS_CATEGORY_TS_WINDOWS = "All TrackSchemes";
	private final static String MEASURING_STATS_CATEGORY_BDV_WINDOWS = "All BigDataViewers";

	//Map< round, Map<source,Measurement> >
	//where Measurement is source(as String) and List<times(as doubles)>
	private final Map<Integer, Map<String,BenchmarkMeasurement>> measurements;
	int currentRound = 1; //NB 1-based counter (besides, it's an _index_ to a map, _not an offset_ to a buffer...)

	public void setRound(int r) {
		currentRound = r;
	}
	public void nextRound() {
		currentRound++;
	}


	public void recordMeasurements(final Map<String,Integer> expectingNowTheseWindowNames,
	                               final BenchmarkLanguage tokenizer) {
		final Map<String, BenchmarkMeasurement> stats = measurements.get(currentRound);
		final String windowType = tokenizer.getCurrentWindowType() == BenchmarkLanguage.WindowType.TS
				  ? MEASURING_STATS_CATEGORY_TS_WINDOWS : MEASURING_STATS_CATEGORY_BDV_WINDOWS;
		double perCommand_windowsTimesSum = 0.0;
		int perCommand_windowsCount = 0;

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
				stats.get(windowName).measuredTimes.add(time);
				stats.get(windowType).measuredTimes.add(time);
				perCommand_windowsTimesSum += time;
				perCommand_windowsCount++;
			}
		}

		if (perCommand_windowsCount == 0) perCommand_windowsCount = 1; //NB: keeps the avg time to 0.0 anyway
		stats.get(MEASURING_STATS_CATEGORY_COMMANDS).measuredTimes
				  .add( perCommand_windowsTimesSum / (double)perCommand_windowsCount );

		//check if there are some unmarked windows?
		for (String windowName : expectingNowTheseWindowNames.keySet()) {
			if (expectingNowTheseWindowNames.get(windowName) != 0) {
				throw new RuntimeException("Mastodon Benchmark:\nDuring the command "
						  +tokenizer.getCurrentToken()+", a measurement for window '"+windowName
						  +"' hasn't been recorded! Increase waiting times, perhaps.");
			}
		}
	}


	public void exportMeasurements(final String pathToCSV) {
	}
}
