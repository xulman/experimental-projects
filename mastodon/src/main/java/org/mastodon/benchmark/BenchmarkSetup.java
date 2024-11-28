package org.mastodon.benchmark;

import bdv.tools.benchmarks.TimeReporter;
import net.imagej.ImageJ;
import mpicbg.spim.data.SpimDataException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mastodon.benchmark.measurements.BenchmarkMeasuring;
import org.mastodon.benchmark.windows.MultipleStepsCommand;
import org.mastodon.benchmark.windows.TrackSchemeBookmarks;
import org.mastodon.benchmark.windows.TsViewsTransition;
import org.mastodon.benchmark.windows.WindowsManager;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.views.MamutViewI;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkSetup implements Runnable {

	// ============================ LOADING/CREATING PROJECTS & MAIN() ============================
	public static ProjectModel openDummyProject(final Context ctx) {
		return ProjectModel.create(ctx,
				  new Model(),
				  SharedBigDataViewerData.fromDummyFilename("DUMMY x=100 y=100 z=100 t=1000.dummy"),
				  new MamutProject("/temp/CLsim.mastodon"));
	}

	public static ProjectModel loadProject(final String pathToMastodonFile, final Context ctx) {
		ProjectModel projectModel;
		try {
			projectModel = ProjectLoader.open(pathToMastodonFile, ctx);
		} catch (IOException | SpimDataException e) {
			System.out.println("Error opening the project "+pathToMastodonFile+": "+e.getMessage());
			throw new RuntimeException(e);
		}
		return projectModel;
	}

	public static void main(String[] args) {
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ProjectModel projectModel = loadProject("/temp/NG_BENCHMARK_DATASET.mastodon", ij.getContext());
		//final ProjectModel projectModel = loadProject("/home/ulman/data/Mastodon-benchmarkData/Benchmark_Cube/Cube_noedges_finished-cyclesproject.mastodon", ij.getContext());
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		//executeBenchmark(projectModel, new BenchmarkInstructions());
	}


	// ============================ THE BENCHMARK ENTRY POINT ============================
	/**
	 * This is the expected starting point of the benchmarking. It instantiates this class
	 * into a new, separate thread, which is the most important thing here. Being in a separate
	 * thread allows us to initiate events on Mastodon (on its main thread, with its GUI AWT
	 * underpinnings), and _wait_ for them here (in this new, separate thread) until they finish
	 * while not blocking them (as it would have happened if the waiting would be on the main thread).
	 */
	public static void executeBenchmark(final ProjectModel project, final BenchmarkInstructions instructions) {
		//start a thread that will do the waiting for the issued benchmarked actions
		Thread benchThread = new Thread(new BenchmarkSetup(project,instructions), "Mastodon Benchmark Controller");
		benchThread.start();
	}

	/**
	 * The constructor is intentionally public so that it allows callers to start/place this
	 * benchmark in their own ways, perhaps in their own threads.
	 * Read more on this in the docs of {@link BenchmarkSetup#executeBenchmark(ProjectModel, BenchmarkInstructions)}.
	 */
	public BenchmarkSetup(final ProjectModel project, final BenchmarkInstructions instructions) {
		this.projectModel = project;
		this.windowsManager = new WindowsManager(project);
		this.windowsManager.resetBdvLocations( instructions.howManyBDVsToOpen > 0 ? instructions.windowSizeOfBDVs.height : 0 );
		this.instructions = instructions;
	}

	private final ProjectModel projectModel;
	private final WindowsManager windowsManager;
	private final BenchmarkInstructions instructions;

	private final List<MamutViewI> allWindows = new ArrayList<>(20);
	private final List<MamutViewBdv> bdvWindows = new ArrayList<>(20);
	private final List<MamutViewTrackScheme> tsWindows = new ArrayList<>(20);
	private final List<TrackSchemeBookmarks> tsBookmarks = new ArrayList<>(20);


	// ============================ THE BENCHMARK MAIN THREAD ============================
	@Override
	public void run() {
		UIService ui = this.projectModel.getContext().getService(UIService.class);
		if (ui != null && ui.getDefaultUI() != null) ui.getDefaultUI().getConsolePane().show();

		if (instructions.bdvSettingsXmlFilename != null && !instructions.bdvSettingsXmlFilename.isEmpty()) {
			try {
				final SAXBuilder sax = new SAXBuilder();
				final Document doc = sax.build( instructions.bdvSettingsXmlFilename );
				final Element root = doc.getRootElement();
				//viewer.stateFromXml( root );
				//setupAssignments.restoreFromXml( root );
				//manualTransformation.restoreFromXml( root );
				projectModel.getSharedBdvData().getBookmarks().restoreFromXml(root);
				//activeSourcesDialog.update();
				//viewer.requestRepaint();
				//TODO: figure out how to close the file! (it's locked on Win as long as Fiji/Mastodon is there)
				System.out.println("Using settings file    : "+instructions.bdvSettingsXmlFilename);
			} catch (IOException | JDOMException e) {
				System.out.println("Failed opening the settings xml file: "+instructions.bdvSettingsXmlFilename);
				System.out.println("The error message was: "+e.getMessage());
			}
		}
		System.out.println("Using TS bookmarks file: "+instructions.tsBookmarksFilename);
		System.out.println("Using  CSV results file: "+instructions.measurementsCsvFilename);

		if (instructions.shouldCloseAllWindowsBeforeBenchmark) windowsManager.closeAllWindows();

		System.out.println("\n==============>\nBenchmark starting, hands-off, no mouse, no keyboard, grab a coffee.\n==============>\n");

		try {
			SwingUtilities.invokeAndWait( () -> {
				Integer groupLockID = instructions.shouldLockButtonsLinkOpenedWindows ? 1 : null;
				for (int bdvs = 1; bdvs <= instructions.howManyBDVsToOpen; ++bdvs) {
					MamutViewBdv win = windowsManager.openBDV("BenchBDV #" + bdvs, instructions.windowSizeOfBDVs, groupLockID);
					allWindows.add(win);
					bdvWindows.add(win);
				}
				for (int tss = 1; tss <= instructions.howManyTSsToOpen; ++tss) {
					MamutViewTrackScheme win = windowsManager.openTS("BenchTS #" + tss, instructions.windowSizeOfTSs, groupLockID);
					allWindows.add(win);
					tsWindows.add(win);
					//
					final File tsbFile = new File(instructions.tsBookmarksFilename);
					TrackSchemeBookmarks tsb = new TrackSchemeBookmarks(win, tsbFile);
					if (tsbFile.canRead()) {
						tsb.loadBookmarksFromFile(tsbFile);
					} else {
						System.out.println("Failed opening the bookmark file: "+instructions.tsBookmarksFilename);
					}
					tsBookmarks.add(tsb);
				}
			} );
		} catch (InterruptedException|InvocationTargetException  e) {
			throw new RuntimeException("Error opening Mastodon windows for the benchmark: "+e.getMessage(), e);
		}

		System.out.println("All "+allWindows.size()+" benchmarked windows were opened.");
		waitThisLong(instructions.millisToWaitAfterInitialization, "for all windows to load fully.");
		//explainInstructions( instructions.benchmarkInitializationSequence );

		try {
			SwingUtilities.invokeAndWait( () -> {
				System.out.println("Setting the windows:");
				this.currentTimePoint = 0; //initiate, but may get updated during the "setting-up sequence"
				this.numSpotsInThisTimePoint = projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(currentTimePoint).size();
				executeInstructions(instructions.benchmarkInitializationSequence, 0, null);
			} );
		} catch (InterruptedException|InvocationTargetException  e) {
			throw new RuntimeException("Error presetting Mastodon windows for the benchmark: "+e.getMessage(), e);
		}

		waitThisLong(instructions.millisToWaitAfterInitialization, "until the world calms down.");
		System.out.println("All "+allWindows.size()+" benchmarked windows are ready.");

		int prepareNumRounds = instructions.measurementsReportsAlsoPerRound ? instructions.benchmarkRounds : 1;
		final BenchmarkMeasuring measurings
				  = new BenchmarkMeasuring(prepareNumRounds, this.tsWindows, this.bdvWindows);

		for (int round = 1; round <= instructions.benchmarkRounds; ++round) {
			System.out.println("\nStarting the benchmark, round #"+round+":");
			executeInstructions(instructions.benchmarkExecutionSequence, instructions.millisToWaitAfterEachBenchmarkAction, measurings);
			System.out.println("Benchmark is over.");
			TimeReporter.getInstance().stopReportingNow();

			if (round < instructions.benchmarkRounds) {
				//round(s) remaining.... we have to reset the env
				System.out.println("\nRe-Setting the windows:");
				executeInstructions(instructions.benchmarkInitializationSequence, 0, null);
				waitThisLong(instructions.millisToWaitAfterInitialization, "until the world calms down.");
				System.out.println("All "+allWindows.size()+" benchmarked windows are ready.");
				if (instructions.measurementsReportsAlsoPerRound) measurings.nextRound();
			}
		}

		if (instructions.measurementsCsvFilename != null && !instructions.measurementsCsvFilename.isEmpty()) {
			System.out.println("Writing measurements file: " + instructions.measurementsCsvFilename);
			String optionalInfo = " [for "
					  +instructions.howManyBDVsToOpen+" BDV and "
					  +instructions.howManyTSsToOpen+" TS windows doing "
					  +instructions.benchmarkExecutionSequence+"]";
			measurings.exportMeasurementsToHorizontalCsv(instructions.measurementsCsvFilename, optionalInfo);
		}
	}

	protected void explainInstructions(final String query) {
		final BenchmarkLanguage tokenizer = new BenchmarkLanguage(query);
		while (tokenizer.isTokenAvailable()) {
			System.out.println("--> "+tokenizer.getCurrentToken());

			int winIdx = tokenizer.getCurrentWindowNumber();
			if (winIdx == -1) {
				System.out.println("  Addressing: all "+tokenizer.getCurrentWindowType()+" windows");
			} else {
				System.out.println("  Addressing: "+tokenizer.getCurrentWindowType()+" #"+winIdx);
			}

			BenchmarkLanguage.ActionType act = tokenizer.getCurrentAction();
			if (act == BenchmarkLanguage.ActionType.B) {
				System.out.println("  Switch to bookmark "+tokenizer.getBookmarkKey());
			} else if (act == BenchmarkLanguage.ActionType.T) {
				System.out.println("  Switch to timepoint " + tokenizer.getTimepoint());
			} else if (act == BenchmarkLanguage.ActionType.F) {
				System.out.println("  Focus on spot "+tokenizer.getSpotLabel());
			} else if (act == BenchmarkLanguage.ActionType.R) {
				System.out.println("  Rotate using "+tokenizer.getFullRotationSteps()+" steps");
			}

			tokenizer.moveToNextToken();
		}
	}

	private int currentTimePoint = 0;
	private int numSpotsInThisTimePoint = -1;

	protected void executeInstructions(final String commands, final long millisBetweenCommands, final BenchmarkMeasuring measurings) {
		if (commands == null || commands.isEmpty()) {
			System.out.println("No instructions, finished trivially.");
			return;
		}

		final Map<String, Integer> currentlyMeasuringTheseWindowNames
				  = new HashMap<>(instructions.howManyBDVsToOpen + instructions.howManyTSsToOpen);
		final boolean doMeasureCommands = measurings != null;

		final BenchmarkLanguage tokenizer = new BenchmarkLanguage(commands);
		List<MultipleStepsCommand> loopingCommands = new ArrayList<>(allWindows.size());
		while (tokenizer.isTokenAvailable()) {
			do {
				System.out.println("executing command: "+tokenizer.getCurrentToken());
				currentlyMeasuringTheseWindowNames.clear();

				final int winIdx = tokenizer.getCurrentWindowNumber();
				if (tokenizer.getCurrentWindowType() == BenchmarkLanguage.WindowType.TS) {
					if (winIdx > tsWindows.size() || (winIdx == -1 && tsWindows.isEmpty())) {
						if (winIdx == -1) System.out.println("Skipping a command that requests TS windows because no TS windows are available.");
						else System.out.println("Skipping a command that requests window TS #"+winIdx+", only "+tsWindows.size()+" TS windows are available.");
						continue;
					}
					List<MamutViewTrackScheme> wins = winIdx == -1 ? tsWindows : Collections.singletonList( tsWindows.get( winIdx-1 ) );
					List<TrackSchemeBookmarks> bms = winIdx == -1 ? tsBookmarks : Collections.singletonList( tsBookmarks.get( winIdx-1 ) );
					wins.forEach(w -> currentlyMeasuringTheseWindowNames.put( w.getFrame().getTrackschemePanel().getDisplay().getDisplayName(),1 ));
					BenchmarkLanguage.ActionType act = tokenizer.getCurrentAction();
					if (act == BenchmarkLanguage.ActionType.B) {
						final int key = (int)tokenizer.getBookmarkKey() - 49;
						if (key >= 0 && key < TrackSchemeBookmarks.MAX_BOOKMARKS) {
							if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size()+1);
							bms.forEach(b -> b.applyBookmark(key));
						} else {
							System.out.println("Skipping command, failed parsing bookmark or bookmark outside interval 1 to "+TrackSchemeBookmarks.MAX_BOOKMARKS+".");
						}
					} else if (act == BenchmarkLanguage.ActionType.F) {
						doCommandF(tokenizer, doMeasureCommands);
					} else if (act == BenchmarkLanguage.ActionType.Z) {
						if (loopingCommands.size() == 0) {
							//the first handling of this particular command, let's prepare and populate the "inner loop" list
							AtomicInteger offset = new AtomicInteger(0);
							wins.forEach( w -> {
								loopingCommands.add( new TsViewsTransition( w,
										  bms.get(offset.get()).getBookmark( (int)tokenizer.getFromBookmark() - 49 ),
										  bms.get(offset.get()).getBookmark( (int)tokenizer.getToBookmark() - 49 ),
										  tokenizer.getFromToSteps() ) );
								offset.incrementAndGet();
							} );
						}
						if (loopingCommands.size() > 0 && loopingCommands.get(0).hasNext()) {
							//here, do the action, and perhaps clean the "inner loop" list if no further actions are available
							loopingCommands.forEach( cmd -> System.out.println("  -> "+cmd.reportCurrentStep()) );
							if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size()+1);
							loopingCommands.forEach( MultipleStepsCommand::doNext );
							if (!loopingCommands.get(0).hasNext()) loopingCommands.clear();
						}
					} else {
						throw new IllegalArgumentException("Benchmark ran into unsupported command "+tokenizer.getCurrentToken());
					}
				} else {
					if (winIdx > bdvWindows.size() || (winIdx == -1 && bdvWindows.isEmpty())) {
						if (winIdx == -1) System.out.println("Skipping a command that requests BDV windows because no BDV windows are available.");
						else System.out.println("Skipping a command that requests window BDV #"+winIdx+", only "+bdvWindows.size()+" BDV windows are available.");
						continue;
					}
					List<MamutViewBdv> wins = winIdx == -1 ? bdvWindows : Collections.singletonList( bdvWindows.get( winIdx-1 ) );
					wins.forEach(w -> currentlyMeasuringTheseWindowNames.put( w.getViewerPanelMamut().getDisplay().getDisplayName(),1 ));
					BenchmarkLanguage.ActionType act = tokenizer.getCurrentAction();
					if (act == BenchmarkLanguage.ActionType.B) {
						final String key = String.valueOf(tokenizer.getBookmarkKey());
						if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size()+1);
						wins.forEach(w -> windowsManager.visitBookmarkBDV(w,key));
					} else if (act == BenchmarkLanguage.ActionType.T && winIdx == -1) { //NB: _T works only for all windows, not for just one particular
						final int time = tokenizer.getTimepoint();
						currentTimePoint = time;
						numSpotsInThisTimePoint = projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(currentTimePoint).size();
						if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size()+1);
						wins.forEach(w -> windowsManager.changeTimepoint(w,time));
					} else if (act == BenchmarkLanguage.ActionType.R) {
						if (loopingCommands.size() == 0) {
							//the first handling of this particular command, let's prepare and populate the "inner loop" list
							final int steps = tokenizer.getFullRotationSteps();
							wins.forEach( w -> loopingCommands.add( windowsManager.rotateBDV(w, 360.0/(double)steps, steps) ) );
						}
						if (loopingCommands.size() > 0 && loopingCommands.get(0).hasNext()) {
							//here, do the action, and perhaps clean the "inner loop" list if no further actions are available
							loopingCommands.forEach( cmd -> System.out.println("  -> "+cmd.reportCurrentStep()) );
							if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size()+1);
							loopingCommands.forEach( MultipleStepsCommand::doNext );
							if (!loopingCommands.get(0).hasNext()) loopingCommands.clear();
						}
					} else if (act == BenchmarkLanguage.ActionType.F) {
						doCommandF(tokenizer, doMeasureCommands);
					} else {
						throw new IllegalArgumentException("Benchmark ran into unsupported command "+tokenizer.getCurrentToken());
					}
				}

				if (millisBetweenCommands > 0) waitForWinsAtMostThisLong(currentlyMeasuringTheseWindowNames.keySet(), millisBetweenCommands);
				//reporting... (now that we have hopefully waited long enough (for the windows to finish their command))
				if (doMeasureCommands) {
					measurings.recordMeasurements(currentlyMeasuringTheseWindowNames, tokenizer);
					measurings.recordMeasurements(currentTimePoint, numSpotsInThisTimePoint);
				}
			} while (loopingCommands.size() > 0);
			tokenizer.moveToNextToken();
		}
	}

	private void doCommandF(final BenchmarkLanguage tokenizer, final boolean doMeasureCommands) {
		if (!instructions.shouldLockButtonsLinkOpenedWindows) {
			System.out.println("Focusing makes sense only when all windows are linked with the lock icon/button, skipping.");
		} else {
			final String spotLabel = tokenizer.getSpotLabel();
			Optional<Spot> spot = projectModel.getModel().getGraph()
					.vertices()
					.stream()
					.filter(s -> s.getLabel().equals(spotLabel))
					.findFirst();
			if (spot.isPresent()) {
				final Spot target = spot.get();
				//NB: fetching the spot outside the measured zone... just in case

				if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(allWindows.size()+1);
				allWindows.get(0).getGroupHandle().getModel(projectModel.NAVIGATION).notifyNavigateToVertex(target);
				//NB: just use any window we have...
			} else {
				System.out.println("Couldn't find the spot with the label >>" + spotLabel + "<<, skipping.");
			}
		}
	}


	// ============================ AUX ROUTINES ============================
	/**
	 * Just pauses the execution of this thread for the given time. Since this thread
	 * should ideally NOT be the main thread of Mastodon (and of AWT), the Mastodon should
	 * be doing its stuff happily while we're waiting here (where the "stuff" is actually
	 * our triggered benchmarked actions...).
	 * See also {@link BenchmarkSetup#executeBenchmark(ProjectModel, BenchmarkInstructions)}.
	 */
	public void waitThisLong(final long periodInMillis) {
		System.out.println("  -> Benchmark thread: Going to wait "+periodInMillis+" ms");
		try {
			Thread.sleep(periodInMillis);
		} catch (InterruptedException e) {
			System.out.println("Interrupted while waiting during benchmark: "+e.getMessage());
		}
		System.out.println("  -> Benchmark thread: Finished the waiting...");
	}

	/**
	 * Prints out the 'reasonForWaiting' and then calls {@link BenchmarkSetup#waitThisLong(long)}.
	 */
	public void waitThisLong(final long periodInMillis, final String reasonForWaiting) {
		System.out.println("  -> Benchmark thread: Will wait "+reasonForWaiting);
		waitThisLong(periodInMillis);
	}

	public void waitForWinsAtMostThisLong(final Set<String> windowNames, final long periodInMillis) {
		System.out.println("  -> Benchmark thread: Going to wait not more than "+periodInMillis+" ms");

		final long waitingGranularity = 100; //millis
		long waitingSoFar = 0;
		Map<String, List<Double>> observedWins = TimeReporter.getInstance().observedTimes;

		boolean keepWaiting = true;
		try {
			while (keepWaiting && waitingSoFar < periodInMillis) {
				Thread.sleep(waitingGranularity);
				waitingSoFar += waitingGranularity;

				//check and possible flip the 'keepWaiting'
				keepWaiting = false;
				for (String w : windowNames) { if (!observedWins.containsKey(w)) keepWaiting = true; }
			}
		} catch (InterruptedException e) {
			System.out.println("Interrupted while waiting during benchmark: "+e.getMessage());
		}

		System.out.println("  -> Benchmark thread: Finished the waiting after "+waitingSoFar+ " ms...");
		//no checks!
		//this method really should only wait at most some time, and do nothing more
	}
}
