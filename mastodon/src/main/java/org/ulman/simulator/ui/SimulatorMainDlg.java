package org.ulman.simulator.ui;

import net.imagej.ImageJ;
import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import javax.swing.WindowConstants;


@Plugin(type = Command.class)
public class SimulatorMainDlg implements Command {
	@Parameter
	ProjectModel projectModel;

	@Parameter(label = "Number of cells initially:", min="1")
	int numCells = 2;

	@Parameter(label = "Number of time points to be created:", min="1")
	int maxTimePoint = 10;

	@Parameter(label = "Show the advanced dialog:")
	boolean showAdvancedDlg = false;

	@Parameter
	CommandService commandService;

	@Override
	public void run() {
		if (showAdvancedDlg) {
			commandService.run(SimulatorAdvancedDlg.class,true,
					"basicDialog",this);
			return;
		}

		runInsideMastodon();
	}

	public void runInsideMastodon() {
		if (continueWithExisting) {
			new Runner(projectModel, numTimepoints).run();
		} else {
			new Runner(projectModel, numCells, numTimepoints).run();
		}
	}

	// ===============================================================================================
	public static void main(String[] args) {
		runWithGUI();
		//runHeadless("/temp/CLsim.mastodon");
	}

	public static void runWithGUI() {
		System.setProperty( "apple.laf.useScreenMenuBar", "true" );

		ImageJ ij = new ImageJ();
		ij.ui().showUI();

		final ProjectModel projectModel = ProjectModel.create(ij.getContext(),
				new Model(),
				SharedBigDataViewerData.fromDummyFilename("DUMMY x=100 y=100 z=100 t=100.dummy"),
				new MamutProject("/temp/CLsim.mastodon"));
		final MainWindow win = new MainWindow( projectModel );
		win.setVisible( true );
		win.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

		//and the GUI triggers the Simulator itself...
	}

	public static void runHeadless(final String projectFileName) {
		new Runner(projectFileName,5,10).run();
	}
}