package org.mastodon.benchmark;

import bdv.tools.benchmarks.TimeReporter;
import mpicbg.spim.data.SpimDataException;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
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
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class BenchmarkSetup implements Runnable {

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
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		executeBenchmark(projectModel);
	}

	public static void executeBenchmark(final ProjectModel project) {
		//start a thread that will do the waiting for the issued benchmarked actions
		Thread benchThread = new Thread(new BenchmarkSetup(project), "Mastodon Benchmark Controller");
		benchThread.start();
	}

	public BenchmarkSetup(final ProjectModel project) {
		//NB: keeping it 'public' allows callers to start/place this benchmark their own ways
		this.projectModel = project;
	}

	private final ProjectModel projectModel;


	@Override
	public void run() {
		clear();

		List<MamutViewI> windows = setup();
		//doActions(windows);

		MamutViewBdv bdv = projectModel.getWindowManager().getViewList(MamutViewBdv.class).get(0);
		visitBookmarks(Arrays.asList("1","2","3","4"), bdv, 3000);
	}


	public void clear() {
		//close all opened windows
		projectModel.getWindowManager().closeAllWindows();
	}

	public List<MamutViewI> setup() {
		final List<MamutViewI> windows = new ArrayList<>(10);

		System.out.println("opening windows...");
		MamutViewBdv bdvXY = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvXY.getFrame().setTitle("XY - CLsim");
		bdvXY.getViewerPanelMamut().getDisplay().setDisplayName("bdvXY");
		windows.add(bdvXY);

		/*
		//TODO: make BDV windows of my size (1024x1024), not of some random one
		MamutViewBdv bdvYZ = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvYZ.getFrame().setTitle("YZ - CLsim");
		bdvYZ.getViewerPanelMamut().getDisplay().setDisplayName("bdvYZ");
		//
		AffineTransform3D yzViewTransform = new AffineTransform3D();
		bdvYZ.getViewerPanelMamut().state().getViewerTransform(yzViewTransform);
		yzViewTransform.rotate(2,Math.PI/2.0);
		bdvYZ.getViewerPanelMamut().state().setViewerTransform(yzViewTransform);
		windows.add(bdvYZ);

		MamutViewBdv bdvXZ = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvXZ.getFrame().setTitle("XZ - CLsim");
		bdvXZ.getViewerPanelMamut().getDisplay().setDisplayName("bdvXZ");
		//
		AffineTransform3D xzViewTransform = new AffineTransform3D();
		bdvXZ.getViewerPanelMamut().state().getViewerTransform(xzViewTransform);
		xzViewTransform.rotate(1,Math.PI/2.7); //intentionally less than 90deg
		bdvXZ.getViewerPanelMamut().state().setViewerTransform(xzViewTransform);
		windows.add(bdvXZ);

		MamutViewTrackScheme ts = projectModel.getWindowManager().createView(MamutViewTrackScheme.class);
		ts.getFrame().getTrackschemePanel().getDisplay().setDisplayName(" TS  ");
		windows.add(ts);
		*/

		System.out.println("lock buttons in windows...");
		windows.forEach( w -> w.getGroupHandle().setGroupId(1) );

		waitThisLong(10000, "10 secs until the newly opened windows calm down");
		waitThisLong(1000, "READY IN 3...");
		waitThisLong(1000, "READY IN 2...");
		waitThisLong(1000, "READY IN 1...");

		return windows;
	}

	public void doActions(final List<MamutViewI> windows) {
		TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
		changeTimepoint(windows.get(0), 50);
		waitThisLong(5000);

		TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
		changeTimepoint(windows.get(0), 55);
		waitThisLong(5000);

		System.out.println("done benchmarking");
	}


	public void visitBookmarks(final List<String> bookmarkKeys, final MamutViewBdv bdvWin, final long delaysInMillis) {
		//MamutViewBdv bdv = projectModel.getWindowManager().getViewList(MamutViewBdv.class).get(0);
		//list of actions - find mouse moves and override when X key is pressed
		//bdv.getViewerPanelMamut().getInputTriggerConfig();

		//gives transform... hmmm
		//AffineTransform3D t = projectModel.getSharedBdvData().getBookmarks().get(key);
		//projectModel.getSharedBdvData().getBookmarks().restoreFromXml(...);

		bookmarkKeys.forEach(key -> {
			bdvWin.getViewerPanelMamut().state().setViewerTransform( projectModel.getSharedBdvData().getBookmarks().get(key) );
			waitThisLong(delaysInMillis, "after moving to bookmark "+key);
		});
	}


	public void changeTimepoint(final List<MamutViewI> windows, final int setTP) {
		System.out.println("Asking all "+windows.size()+" windows to switch to time point "+setTP);
		windows.forEach( w -> w.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP) );
	}

	public void changeTimepoint(final MamutViewI window, final int setTP) {
		System.out.println("Asking a given window to switch to time point "+setTP);
		window.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP);
	}


	/**
	 * Just pauses the execution of this thread for the given time. Since this thread
	 * should NOT be the main thread of Mastodon (and of AWT), the Mastodon should
	 * be doing its stuff happily while we're waiting here (and the "stuff" is actually
	 * our benchmarked actions...). That much the plan...
	 */
	public void waitThisLong(final long periodInMillis) {
		System.out.println("Benchmark thread: Going to wait "+periodInMillis+" ms");
		try {
			Thread.sleep(periodInMillis);
		} catch (InterruptedException e) {
			System.out.println("Interrupted while waiting during benchmark: "+e.getMessage());
		}
		System.out.println("Benchmark thread: Finished the waiting...");
	}

	public void waitThisLong(final long periodInMillis, final String reasonForWaiting) {
		System.out.println("Benchmark thread: Will wait "+reasonForWaiting);
		waitThisLong(periodInMillis);
	}
}
