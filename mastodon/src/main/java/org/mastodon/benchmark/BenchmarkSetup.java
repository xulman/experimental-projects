package org.mastodon.benchmark;

import bdv.tools.benchmarks.TimeReporter;
import net.imagej.ImageJ;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.views.MamutViewI;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.views.bdv.SharedBigDataViewerData;

import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

public class BenchmarkSetup {

	public static void main(String[] args) {
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		ImageJ ij = new ImageJ();

		//TODO: should open on a particular testing benchmark
		final ProjectModel projectModel = ProjectModel.create(ij.getContext(),
				new Model(),
				SharedBigDataViewerData.fromDummyFilename("DUMMY x=100 y=100 z=100 t=1000.dummy"),
				new MamutProject("/temp/CLsim.mastodon"));
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		new BenchmarkSetup(projectModel).runBenchmark();
	}


	public BenchmarkSetup(final ProjectModel project) {
		this.projectModel = project;
	}

	private final ProjectModel projectModel;


	public void runBenchmark() {
		clear();

		List<MamutViewI> windows = setup();
		doActions(windows);
	}


	public void clear() {
		//close all opened windows
	}

	public List<MamutViewI> setup() {
		final List<MamutViewI> windows = new ArrayList<>(10);

		System.out.println("opening windows...");
		MamutViewBdv bdvXY = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvXY.getFrame().setTitle("XY - CLsim");
		bdvXY.getViewerPanelMamut().getDisplay().setDisplayName("bdvXY");
		windows.add(bdvXY);


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

		System.out.println("lock buttons in windows...");
		windows.forEach( w -> w.getGroupHandle().setGroupId(1) );

		return windows;
	}

	public void doActions(final List<MamutViewI> windows) {
			TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
			changeTimepoint(windows, 50);
			busyWaitForMillis(3000);

			TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
			changeTimepoint(windows, 55);
			busyWaitForMillis(3000);
			System.out.println("done benchmarking");

			//TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
			//changeTimepoint(windows, 250);
			//Thread.sleep(5000);

			//TimeReporter.getInstance().startNowAndReportNotMoreThan(windows.size());
			//changeTimepoint(windows.get(0), 500);
	}

	public void busyWaitForMillis(final long period) {
		try {
			long time = System.currentTimeMillis();
			final long targetTime = time + period;
			while (time < targetTime) {
				Thread.sleep(1);
				time = System.currentTimeMillis();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void changeTimepoint(final List<MamutViewI> windows, final int setTP) {
		System.out.println("changing all "+windows.size()+" windows to time point "+setTP);
		windows.forEach( w -> w.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP) );
	}

	public void changeTimepoint(final MamutViewI window, final int setTP) {
		System.out.println("changing the given window to time point "+setTP);
		window.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP);
	}
}
