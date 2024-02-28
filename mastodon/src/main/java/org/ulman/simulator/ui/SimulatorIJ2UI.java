package org.ulman.simulator.ui;

import net.imagej.ImageJ;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.ulman.simulator.Simulator;
import javax.swing.WindowConstants;


@Plugin(type = Command.class)
public class SimulatorIJ2UI implements Command {
	@Parameter
	ProjectModel projectModel;

	@Parameter(label = "Number of cells initially:", min="1")
	int numCells = 2;

	@Parameter(label = "Number of time points to be created:", min="1")
	int maxTimePoint = 10;

	@Override
	public void run() {
		Simulator s = new Simulator(projectModel);
		try {
			System.out.println("SIMULATOR STARTED on "+java.time.LocalTime.now());
			s.open();
			s.populate(numCells);
			s.pushToMastodonGraph();
			for (int time = 0; time < maxTimePoint; ++time) {
				s.doOneTime();
				s.pushToMastodonGraph();
			}
		} catch (Exception e) {
			System.out.println("SOME ERROR: "+e.getMessage());
		} finally {
			s.close();
			System.out.println("SIMULATOR FINISHED on "+java.time.LocalTime.now());
		}
	}

	public static void main(String[] args) {
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ProjectModel appModel = ProjectModel.create(ij.getContext(),
				new Model(),
				SharedBigDataViewerData.fromDummyFilename("DUMMY x=100 y=100 z=100 t=100.dummy"),
				new MamutProject("/temp/CLsim.mastodon"));
		final MainWindow win = new MainWindow( appModel );
		win.setVisible( true );
		win.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
	}
}