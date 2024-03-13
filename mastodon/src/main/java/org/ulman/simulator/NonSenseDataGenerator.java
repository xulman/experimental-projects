package org.ulman.simulator;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Random;

public class NonSenseDataGenerator {
	public NonSenseDataGenerator(final ProjectModel projectModel,
	                             final int numberOfTimepoints,
	                             final int numberOfSpotsPerTimepoint,
	                             final boolean doLinkSpots,
	                             final int reportFromRound,
	                             final int reportEveryNthRound) {

		ReentrantReadWriteLock lock = projectModel.getModel().getGraph().getLock();
		final Spot auxSpot = projectModel.getModel().getGraph().vertexRef();
		final Spot prevSpot = projectModel.getModel().getGraph().vertexRef();
		//Edge auxEdge = projectModel.getModel().getGraph().edgeRef();

		try {
			System.out.println("GENERATOR STARTED on "+java.time.LocalTime.now());

			new ModelGraphListeners().pauseListeners();
			lock.writeLock().lock();

			final double[] coords = new double[3];

			//start generating
			if (doLinkSpots) {
				//"vertically"
				for (int cnt = 0; cnt < numberOfSpotsPerTimepoint; ++cnt) {
					for (int time = 0; time < numberOfTimepoints; ++time) {
						//create and add a spot, and link to its parent
						coords[0] = 100.0 * Math.cos(6.28 * (double)cnt / (double)numberOfSpotsPerTimepoint);
						coords[1] = 100.0 * Math.sin(6.28 * (double)cnt / (double)numberOfSpotsPerTimepoint);
						projectModel.getModel().getGraph().addVertex(auxSpot).init(time, coords, 0.87);
						if (time > 0) projectModel.getModel().getGraph().addEdge(prevSpot, auxSpot).init();
						prevSpot.refTo(auxSpot);
					}
					if (cnt > reportFromRound && (cnt % reportEveryNthRound) == 0) {
						System.out.println("added in total "+(numberOfTimepoints*(cnt+1))
								+" spots and "+((numberOfTimepoints-1)*(cnt+1))+" links on "
								+java.time.LocalTime.now());
					}
				}
			} else {
				//"horizontally", no-linking
				for (int time = 0; time < numberOfTimepoints; ++time) {
					for (int cnt = 0; cnt < numberOfSpotsPerTimepoint; ++cnt) {
						//create and add a spot
						coords[0] = 100.0 * Math.cos(6.28 * (double)cnt / (double)numberOfSpotsPerTimepoint);
						coords[1] = 100.0 * Math.sin(6.28 * (double)cnt / (double)numberOfSpotsPerTimepoint);
						projectModel.getModel().getGraph().addVertex(auxSpot).init(time, coords, 0.87);
					}
					if (time > reportFromRound && (time % reportEveryNthRound) == 0) {
						System.out.println("added in total "+(numberOfSpotsPerTimepoint*(time+1))
								+" spots and on "+java.time.LocalTime.now());
					}
				}
			}

		} catch (Exception e) {
			System.out.println("GENERATOR ERROR: "+e.getMessage());
		} finally {
			System.out.println("GENERATOR FINISHED on "+java.time.LocalTime.now());

			//projectModel.getModel().getGraph().releaseRef(auxEdge);
			projectModel.getModel().getGraph().releaseRef(auxSpot);
			projectModel.getModel().getGraph().releaseRef(prevSpot);

			lock.writeLock().unlock();
			new ModelGraphListeners().resumeListeners();
			projectModel.getModel().setUndoPoint();
			projectModel.getModel().getGraph().notifyGraphChanged();
		}
	}

	class ModelGraphListeners extends ModelGraph {
		public void pauseListeners() {
			super.pauseListeners();
		}
		public void resumeListeners() {
			super.resumeListeners();
		}
	}
}
