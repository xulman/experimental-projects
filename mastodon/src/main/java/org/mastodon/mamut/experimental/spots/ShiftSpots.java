package org.mastodon.mamut.experimental.spots;

import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, name = "Move spots in space and time" )
public class ShiftSpots implements Command {

	@Parameter
	double multiply_x = 1;
	@Parameter
	double multiply_y = 1;
	@Parameter
	double multiply_z = 1;

	@Parameter
	double delta_x = 0;
	@Parameter
	double delta_y = 0;
	@Parameter
	double delta_z = 0;

	@Parameter(min = "1")
	int multiply_t = 1;
	@Parameter
	int delta_t = 0;

	@Parameter(persist = false)
	MamutAppModel appModel;

	@Override
	public void run() {
		graph = appModel.getModel().getGraph();
		curSpot = graph.vertexRef();
		newSpot = graph.vertexRef();

		final ReentrantReadWriteLock lock = graph.getLock();
		lock.writeLock().lock();
		try {
			if (appModel.getSelectionModel().isEmpty()) {
				processSpots((o) -> appModel.getModel().getSpatioTemporalIndex().iterator());
			} else {
				processSpots((o) -> appModel.getSelectionModel().getSelectedVertices().iterator());
			}
		} finally {
			lock.writeLock().unlock();
		}

		graph.releaseRef(curSpot);
		graph.releaseRef(newSpot);

		System.out.println("Shifting done.");
		appModel.getModel().getGraph().notifyGraphChanged();
	}

	ModelGraph graph;
	Spot curSpot, newSpot;
	final double[] pos = new double[3];
	final double[][] cov = new double[3][3];

	/* int counter = 0; */
	public void processSpots(final Function<?,Iterator<Spot>> iterFactory) {
		final Set<Integer> toBeProcessedIDs = new HashSet<>(100000);
		iterFactory.apply(null).forEachRemaining( s -> toBeProcessedIDs.add(s.getInternalPoolIndex()) );

		while (!toBeProcessedIDs.isEmpty()) {
			System.out.println("There are "+toBeProcessedIDs.size()+" spots to be moved...");
			Iterator<Spot> iter = iterFactory.apply(null);
			while (iter.hasNext()) {
				Spot s = iter.next();
				final int sId = s.getInternalPoolIndex();
				if (!toBeProcessedIDs.contains( sId )) continue;

				//progress bar...
				/*
				if (counter == 0) System.out.print("Shifting spot with label ");
				System.out.print( s.getLabel() + "," );
				++counter;
				if (counter == 50) {
					System.out.println();
					counter = 0;
				}
				*/

				if (delta_t == 0) shiftSpatially(s);
				else shiftSpatiallyAndTemporarily(s);

				toBeProcessedIDs.remove( sId );
			}
			System.out.println("There are "+toBeProcessedIDs.size()+" spots left untouched...");
		}
	}


	private void shiftPosition(final double[] pos) {
		pos[0] = multiply_x * pos[0] + delta_x;
		pos[1] = multiply_y * pos[1] + delta_y;
		pos[2] = multiply_z * pos[2] + delta_z;
	}

	private Spot shiftSpatially(final Spot s) {
		s.localize(pos);
		shiftPosition(pos);
		s.setPosition(pos);
		return s;
	}

	private Spot shiftSpatiallyAndTemporarily(final Spot s) {
		s.localize(pos);
		shiftPosition(pos);
		s.getCovariance(cov);
		int newtime = Math.max(appModel.getMinTimepoint(),
				Math.min(multiply_t * s.getTimepoint() + delta_t, appModel.getMaxTimepoint()));

		graph.addVertex(newSpot).init(newtime, pos, cov);
		newSpot.setLabel(s.getLabel());
		s.incomingEdges().forEach(l -> graph.addEdge(l.getSource(), newSpot).init());
		s.outgoingEdges().forEach(l -> graph.addEdge(newSpot, l.getTarget()).init());
		graph.remove(s);

		return newSpot;
	}
}
