package org.ulman.simulator.ui;

import net.imagej.ImageJ;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.io.ProjectSaver;
import org.mastodon.mamut.io.project.MamutProject;
import org.mastodon.mamut.model.Model;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.views.bdv.SharedBigDataViewerData;
import org.ulman.simulator.Simulator;
import java.io.File;
import java.io.IOException;

/** This class hosts the main simulation loop. */
public class Runner implements Runnable {
	private final ProjectModel projectModel;
	private final String outputProjectFilename;

	private final int initialNumberOfCells;
	private final int timeFrom;
	private final int timeTill;

	/** intended for when full Mastodon is around, starts from the beginning */
	public Runner(final ProjectModel projectModel,
					  final int numberOfCells,
	              final int timepoints) {
		this.projectModel = projectModel;
		this.outputProjectFilename = null; //save nothing in the end
		this.initialNumberOfCells = Math.max(numberOfCells,1);
		this.timeFrom = projectModel.getMinTimepoint();
		this.timeTill = Math.min(timeFrom+timepoints-1, projectModel.getMaxTimepoint());
	}

	/** intended for when full Mastodon is around, starts from the last time point */
	public Runner(final ProjectModel projectModel,
	              final int timepoints) {
		this.projectModel = projectModel;
		this.outputProjectFilename = null; //save nothing in the end
		this.initialNumberOfCells = -1;    //indicates to find them in the last time point of the projectModel

		for (int time = projectModel.getMaxTimepoint()-1; time >= projectModel.getMinTimepoint(); --time) {
			final SpatialIndex<Spot> spots = projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(time);
			if (spots.size() > 0) {
				System.out.println("Found last non-empty time point "+time);
				this.timeFrom = time+1;
				this.timeTill = Math.min(time+timepoints-1, projectModel.getMaxTimepoint());
				return;
			}
		}
		throw new IllegalArgumentException("There are only empty time points in this Mastodon project.");
	}

	/** intended for starts from a command line, from the very beginning */
	public Runner(final String outputProjectFileName,
					  final int numberOfCells,
	              final int timepoints) {
		//setup a Mastodon project first
		final String DUMMYXML="DUMMY x=100 y=100 z=100 t="+timepoints+".dummy";
		ImageJ ij = new ImageJ();
		this.projectModel = ProjectModel.create(ij.getContext(),
				new Model(),
				SharedBigDataViewerData.fromDummyFilename(DUMMYXML),
				new MamutProject(outputProjectFileName));
		this.projectModel.getProject().setDatasetXmlFile(new File(DUMMYXML));
		this.outputProjectFilename = outputProjectFileName;
		//
		this.initialNumberOfCells = Math.max(numberOfCells,1);
		this.timeFrom = 0;
		this.timeTill = timepoints-1;
	}

	@Override
	public void run() {
		Simulator s = new Simulator(projectModel);
		try {
			System.out.println("SIMULATOR STARTED on "+java.time.LocalTime.now());
			s.open();

			if (initialNumberOfCells == -1) {
				s.populate(projectModel, timeFrom);
			} else {
				s.populate(initialNumberOfCells, timeFrom);
			}
			s.pushToMastodonGraph();

			//TODO: !this.outputProjectFilename -> do progress bar
			for (int time = this.timeFrom+1; time <= this.timeTill; ++time) {
				s.doOneTime();
				s.pushToMastodonGraph();
			}
		} catch (Exception e) {
			System.out.println("SIMULATION ERROR: "+e.getMessage());
		} finally {
			s.close();
			System.out.println("SIMULATOR FINISHED on "+java.time.LocalTime.now());
		}

		if (this.outputProjectFilename != null) {
			try {
				ProjectSaver.saveProject(new File(outputProjectFilename), projectModel);
				System.out.println("PROJECT SAVED TO "+outputProjectFilename);
			} catch (IOException e) {
				System.out.println("Hmm... some issue saving the file "+outputProjectFilename);
				System.out.println("Complaint was: "+e.getMessage());
			}
		}
	}
}