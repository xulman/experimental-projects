/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2023, Vladimír Ulman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.mastodon.mamut.experimental.spots;

import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.experimental.spots.util.CopyTagsBetweenSpots;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, name = "Move spots in space and time" )
public class ShiftSpots implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...also only selected ones.";

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
	ProjectModel appModel;

	@Parameter
	LogService logService;

	@Override
	public void run() {
		final Logger log = logService.subLogger("ShiftAndRotateMastodonPoints");

		graph = appModel.getModel().getGraph();
		curSpot = graph.vertexRef();
		newSpot = graph.vertexRef();

		tagsUtil = new CopyTagsBetweenSpots(appModel);

		final ReentrantReadWriteLock lock = graph.getLock();
		lock.writeLock().lock();
		try {
			if (appModel.getSelectionModel().isEmpty()) {
				processSpots((o) -> appModel.getModel().getSpatioTemporalIndex().iterator(), log);
			} else {
				processSpots((o) -> appModel.getSelectionModel().getSelectedVertices().iterator(), log);
			}
		} finally {
			lock.writeLock().unlock();
		}

		graph.releaseRef(curSpot);
		graph.releaseRef(newSpot);

		log.info("Shifting done.");
		appModel.getModel().getGraph().notifyGraphChanged();
	}

	ModelGraph graph;
	Spot curSpot, newSpot;
	final double[] pos = new double[3];
	final double[][] cov = new double[3][3];
	CopyTagsBetweenSpots tagsUtil;

	/* int counter = 0; */
	public void processSpots(final Function<?,Iterator<Spot>> iterFactory, final Logger log) {
		final Set<Integer> toBeProcessedIDs = new HashSet<>(100000);
		iterFactory.apply(null).forEachRemaining( s -> toBeProcessedIDs.add(s.getInternalPoolIndex()) );

		while (!toBeProcessedIDs.isEmpty()) {
			log.info("There are "+toBeProcessedIDs.size()+" spots to be moved...");
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
			log.info("There are "+toBeProcessedIDs.size()+" spots left untouched...");
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

		tagsUtil.insertSpotIntoSameTSAs(newSpot, s);
		tagsUtil.deleteSpotFromAllTS(s);

		graph.remove(s);

		return newSpot;
	}
}
