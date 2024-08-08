package org.mastodon.benchmark;

import bdv.tools.benchmarks.TimeReporter;
import net.imagej.ImageJ;
import mpicbg.spim.data.SpimDataException;
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

import javax.swing.*;
import java.io.IOException;
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

		final ProjectModel projectModel = loadProject("/temp/NG_BENCHMARK_DATASET.mastodon", ij.getContext());
		//final ProjectModel projectModel = loadProject("/home/ulman/data/Mastodon-benchmarkData/Benchmark_Cube/Cube_noedges_finished-cyclesproject.mastodon", ij.getContext());
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		executeBenchmark(projectModel, new BenchmarkInstructions());
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
		if (instructions.shouldCloseAllWindowsBeforeBenchmark) windowsManager.closeAllWindows();

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
		System.out.println("All benchmarked "+allWindows.size()+" windows were opened.");

		//executeWarmUpInstructions();
		System.out.println("All benchmarked "+allWindows.size()+" windows are set ready.");

		//executeInstructions();
/*
		TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
		changeTimepoint(windows.get(0), 50);
		waitThisLong(5000);

		TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
		changeTimepoint(windows.get(0), 55);
		waitThisLong(5000);
*/
		System.out.println("Benchmark is over.");
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
