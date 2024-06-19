package org.mastodon.benchmark;

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
import java.util.Arrays;
import java.util.List;

public class BenchmarkSetup {
	public static void main(String[] args) {
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		ImageJ ij = new ImageJ();
		//ij.ui().showUI();

		//TODO: should open on a particular testing benchmark
		final ProjectModel projectModel = ProjectModel.create(ij.getContext(),
				new Model(),
				SharedBigDataViewerData.fromDummyFilename("DUMMY x=100 y=100 z=100 t=1000.dummy"),
				new MamutProject("/temp/CLsim.mastodon"));
		final MainWindow mainWindow = new MainWindow( projectModel );
		mainWindow.setVisible( true );
		mainWindow.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		System.out.println("opening windows...");
		MamutViewBdv bdvXY = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvXY.getFrame().setTitle("XY - CLsim");

		MamutViewBdv bdvYZ = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvYZ.getFrame().setTitle("YZ - CLsim");
		//
		AffineTransform3D yzViewTransform = new AffineTransform3D();
		bdvYZ.getViewerPanelMamut().state().getViewerTransform(yzViewTransform);
		yzViewTransform.rotate(2,Math.PI/2.0);
		bdvYZ.getViewerPanelMamut().state().setViewerTransform(yzViewTransform);

		MamutViewBdv bdvXZ = projectModel.getWindowManager().createView(MamutViewBdv.class);
		bdvXZ.getFrame().setTitle("XZ - CLsim");
		//
		AffineTransform3D xzViewTransform = new AffineTransform3D();
		bdvXZ.getViewerPanelMamut().state().getViewerTransform(xzViewTransform);
		xzViewTransform.rotate(1,Math.PI/2.7); //intentionally less than 90deg
		bdvXZ.getViewerPanelMamut().state().setViewerTransform(xzViewTransform);

		MamutViewTrackScheme ts = projectModel.getWindowManager().createView(MamutViewTrackScheme.class);

		System.out.println("lock buttons in windows...");
		List<MamutViewI> windows = Arrays.asList(bdvXY,bdvXZ,bdvYZ,ts);
		windows.forEach(w -> w.getGroupHandle().setGroupId(1));

		try {
			changeTimepoint(projectModel,windows, 100);
			Thread.sleep(3000);
			changeTimepoint(projectModel,windows, 200);
			Thread.sleep(3000);
			changeTimepoint(projectModel,windows, 300);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	static void changeTimepoint(final ProjectModel projectModel, final List<MamutViewI> windows, final int setTP) {
		System.out.println("changing windows to time point "+setTP);
		for (MamutViewI win : windows) {
			win.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP);
		}
	}
}
