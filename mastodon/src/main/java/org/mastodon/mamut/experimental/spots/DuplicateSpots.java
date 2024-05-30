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

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.ulman.util.NumberSequenceHandler;

@Plugin( type = Command.class, name = "Duplicate spots in time" )
public class DuplicateSpots implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...also only selected ones.";

	@Parameter(label = "Select spots from this time point:")
	int sourceTimePoint = 0;

	@Parameter(label = "Link the added spots with the source Spot:")
	boolean doLinkSource = true;

	@Parameter(label = "Link the added spots themselves:")
	boolean doLink = true;

	@Parameter(label = "Target time points as, e.g., 1,2,5-8,10:")
	String targetTPsSpecification = "0";
	final SortedSet<Integer> timePoints = new TreeSet<>(); //to be filled in run() from the String above

	@Parameter(persist = false)
	ProjectModel appModel;

	@Parameter
	LogService logService;

	@Override
	public void run() {
		final Logger log = logService.subLogger("DuplicateMastodonPoints");

		graph = appModel.getModel().getGraph();
		prevSpot = graph.vertexRef();
		newSpot = graph.vertexRef();

		try {
			NumberSequenceHandler.parseSequenceOfNumbers(targetTPsSpecification, timePoints);
		} catch (ParseException e) {
			logService.error("Don't understand the target time points: "+e.getMessage());
			return;
		}

		tagsUtil = new CopyTagsBetweenSpots(appModel);

		final ReentrantReadWriteLock lock = graph.getLock();
		lock.writeLock().lock();
		try {
			if (appModel.getSelectionModel().isEmpty()) {
				processSpots((o) -> appModel.getModel().getSpatioTemporalIndex().getSpatialIndex(sourceTimePoint).iterator(), log);
			} else {
				processSpots((o) -> appModel.getSelectionModel().getSelectedVertices().iterator(), log);
			}
		} finally {
			lock.writeLock().unlock();
		}

		graph.releaseRef(prevSpot);
		graph.releaseRef(newSpot);

		log.info("Duplicating done.");
		appModel.getModel().getGraph().notifyGraphChanged();
	}

	ModelGraph graph;
	Spot prevSpot, newSpot;
	final double[] pos = new double[3];
	final double[][] cov = new double[3][3];
	CopyTagsBetweenSpots tagsUtil;

	public void processSpots(final Function<?,Iterator<Spot>> iterFactory, final Logger log) {
		final Set<Integer> toBeProcessedIDs = new HashSet<>(100000);
		iterFactory.apply(null).forEachRemaining( s -> {
			if (s.getTimepoint() == sourceTimePoint) toBeProcessedIDs.add(s.getInternalPoolIndex());
		} );

		while (!toBeProcessedIDs.isEmpty()) {
			log.info("There are "+toBeProcessedIDs.size()+" spots to be cloned...");
			Iterator<Spot> iter = iterFactory.apply(null);
			while (iter.hasNext()) {
				Spot s = iter.next();
				final int sId = s.getInternalPoolIndex();
				if (!toBeProcessedIDs.contains( sId )) continue;

				//add the past vertices first
				boolean firstAddition = true;
				for (int t : timePoints) { //assuming 't's are provided sorted
					if (t >= s.getTimepoint()) break;
					cloneToTime(s,t, !firstAddition && doLink);
					firstAddition = false;
				}
				if (!firstAddition && doLinkSource) graph.addEdge(prevSpot, s).init();

				//now add the future vertices
				prevSpot.refTo(s);
				firstAddition = true;
				for (int t : timePoints) { //assuming 't's are provided sorted
					if (t <= s.getTimepoint()) continue;
					cloneToTime(s,t, (firstAddition && doLinkSource) || (!firstAddition && doLink));
					firstAddition = false;
				}

				toBeProcessedIDs.remove( sId );
			}
			log.info("There are "+toBeProcessedIDs.size()+" spots left untouched...");
		}
	}

	private boolean cloneToTime(final Spot s, final int t, final boolean linkWithPrevSpot) {
		if (t < appModel.getMinTimepoint() || t > appModel.getMaxTimepoint()) return false;

		s.localize(pos);
		s.getCovariance(cov);
		graph.addVertex(newSpot).init(t, pos, cov);
		newSpot.setLabel(s.getLabel());
		tagsUtil.insertSpotIntoSameTSAs(newSpot, s);

		if (linkWithPrevSpot) graph.addEdge(prevSpot, newSpot).init();
		prevSpot.refTo(newSpot);

		return true;
	}
}
