package org.mastodon.benchmark;

import bdv.tools.benchmarks.TimeReporter;
import net.imagej.ImageJ;
import mpicbg.spim.data.SpimDataException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.mastodon.benchmark.windows.WindowsManager;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectLoader;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.views.MamutViewI;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.Context;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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
				System.out.println("Using settings file: "+instructions.bdvSettingsXmlFilename);
			} catch (IOException | JDOMException e) {
				System.out.println("Failed opening the settings xml file: "+instructions.bdvSettingsXmlFilename);
				System.out.println("The error message was: "+e.getMessage());
			}
		}

		if (instructions.shouldCloseAllWindowsBeforeBenchmark) windowsManager.closeAllWindows();

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
				}
			} );
		} catch (InterruptedException|InvocationTargetException  e) {
			throw new RuntimeException("Error opening Mastodon windows for the benchmark: "+e.getMessage(), e);
		}

		System.out.println("All benchmarked "+allWindows.size()+" windows were opened.");
		//explainInstructions( instructions.benchmarkInitializationSequence );

		try {
			SwingUtilities.invokeAndWait( () -> {
				System.out.println("\nSetting the windows:");
				executeInstructions(instructions.benchmarkInitializationSequence, 0, false);
			} );
		} catch (InterruptedException|InvocationTargetException  e) {
			throw new RuntimeException("Error presetting Mastodon windows for the benchmark: "+e.getMessage(), e);
		}

		waitThisLong(instructions.millisToWaitAfterInitialization, "until the world calms down.");
		System.out.println("All benchmarked "+allWindows.size()+" windows are set ready.");

		System.out.println("\nStarting the benchmark:");
		executeInstructions(instructions.benchmarkExecutionSequence, instructions.millisToWaitAfterEachBenchmarkAction, true);
		System.out.println("Benchmark is over.");
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

	protected void executeInstructions(final String commands, final long millisBetweenCommands, final boolean doMeasureCommands) {
		final BenchmarkLanguage tokenizer = new BenchmarkLanguage(commands);
		while (tokenizer.isTokenAvailable()) {
			System.out.println("executing command: "+tokenizer.getCurrentToken());

			final int winIdx = tokenizer.getCurrentWindowNumber();
			if (tokenizer.getCurrentWindowType() == BenchmarkLanguage.WindowType.TS) {
				if (winIdx > tsWindows.size()) {
					System.out.println("Skipping a command that requests window TS #"+winIdx+", only "+tsWindows.size()+" TS windows is available.");
					tokenizer.moveToNextToken();
					continue;
				}
				List<MamutViewTrackScheme> wins = winIdx == -1 ? tsWindows : Collections.singletonList( tsWindows.get( winIdx-1 ) );
				System.out.println("NOT SUPPORTED YET");
				//TODO...
			} else {
				if (winIdx > bdvWindows.size()) {
					System.out.println("Skipping a command that requests window BDV #"+winIdx+", only "+bdvWindows.size()+" BDV windows is available.");
					tokenizer.moveToNextToken();
					continue;
				}
				List<MamutViewBdv> wins = winIdx == -1 ? bdvWindows : Collections.singletonList( bdvWindows.get( winIdx-1 ) );
				BenchmarkLanguage.ActionType act = tokenizer.getCurrentAction();
				if (act == BenchmarkLanguage.ActionType.B) {
					if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size());
					final String key = String.valueOf(tokenizer.getBookmarkKey());
					wins.forEach(w -> windowsManager.visitBookmarkBDV(w,key));
				} else if (act == BenchmarkLanguage.ActionType.T) {
					if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(wins.size());
					final int time = tokenizer.getTimepoint();
					wins.forEach(w -> windowsManager.changeTimepoint(w,time));
				} else if (act == BenchmarkLanguage.ActionType.R) {
					final int steps = tokenizer.getFullRotationSteps();
					if (doMeasureCommands) TimeReporter.getInstance().startNowAndReportNotMoreThan(steps * wins.size());
					wins.forEach(w -> windowsManager.rotateBDV(w, 360.0/(double)steps, steps));
				} else {
					System.out.println("NOT SUPPORTED TOKEN");
					//TODO...
				}
			}

			tokenizer.moveToNextToken();
			if (millisBetweenCommands > 0) waitThisLong(millisBetweenCommands, "a bit until the command finishes.");
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
}
