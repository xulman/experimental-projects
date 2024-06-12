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

import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

@Plugin( type = Command.class, name = "Place spots on a surface" )
public class PlaceSpotsOnSpotSurface implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...of ONLY selected spots.";

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String msg1 = "The created spots take the name from their source spot.";
	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String msg2 = "Consider naming the source spot as keep_out.";

	@Parameter(label = "Radius of the created spots:")
	double targetRadius = 5.0;

	@Parameter(label = "Randomize radius (disabled=0):")
	double targetRadiusVar = 0.0;

	@Parameter(label = "Overlap of the created spots:")
	double targetOverlap = 1.5;

	@Parameter(label = "Created spots are selected:")
	boolean shouldBeSelected = true;

	@Parameter(persist = false)
	ProjectModel projectModel;

	@Parameter
	LogService logService;

	@Override
	public void run() {
		//if nothing is selected, complain
		if (projectModel.getSelectionModel().isEmpty()) {
			logService.warn("Please, select at least one spot first!");
			return;
		}

		final CopyTagsBetweenSpots tagsUtil = new CopyTagsBetweenSpots(projectModel);
		final ModelGraph graph = projectModel.getModel().getGraph();

		final Random rng = new Random();
		final double radiusSigma = 0.33 * targetRadiusVar;
		final Spot newSpot = graph.vertexRef();
		for (Spot s : projectModel.getSelectionModel().getSelectedVertices()) {
			enumerateSurfacePositions(s, Math.sqrt(s.getBoundingSphereRadiusSquared()), targetRadius-targetOverlap)
					  .forEach(p -> {
						  graph.addVertex(newSpot).init(s.getTimepoint(), p.positionAsDoubleArray(), targetRadius + rng.nextGaussian()*radiusSigma);
						  newSpot.setLabel( s.getLabel() );
						  tagsUtil.insertSpotIntoSameTSAs(newSpot, s);
						  if (shouldBeSelected) projectModel.getSelectionModel().setSelected(newSpot, true);
					  });
		}
		graph.releaseRef(newSpot);
	}

	public static List<RealPoint> enumerateSurfacePositions(final RealLocalizable srcCentre,
	                                                        final double srcRadius,
	                                                        final double tgtRadius) {
		List<RealPoint> list = new ArrayList<>(1000);
		final double quarterOfPerimeter = 0.5 * Math.PI * srcRadius;

		int steps = (int)Math.floor((4.0 * quarterOfPerimeter) / (2.0 * tgtRadius));
		double stepsAng = 2.0 * Math.PI / (double)steps;
		for (int s = 0; s < steps; ++s) {
			list.add(new RealPoint(
					  srcCentre.getDoublePosition(0) + srcRadius * Math.cos((double)s * stepsAng),
					  srcCentre.getDoublePosition(1) + srcRadius * Math.sin((double)s * stepsAng),
					  srcCentre.getDoublePosition(2)  ));
		}

		int latLines = (int)Math.floor( quarterOfPerimeter / (2.0 * tgtRadius) );
		double latSteppingAng = 0.5 * Math.PI / (double)latLines;
		for (int l = 1; l < latLines; ++l) {
			double latRadius = srcRadius * Math.cos(l * latSteppingAng);
			double latPerimeter = 2.0 * Math.PI * latRadius;

			steps = (int)Math.floor(latPerimeter / (2.0 * tgtRadius));
			stepsAng = 2.0 * Math.PI / (double)steps;

			for (int s = 0; s < steps; ++s) {
				list.add(new RealPoint(
						  srcCentre.getDoublePosition(0) + latRadius * Math.cos((double)s * stepsAng),
						  srcCentre.getDoublePosition(1) + latRadius * Math.sin((double)s * stepsAng),
						  srcCentre.getDoublePosition(2) + srcRadius * Math.sin((double)l * latSteppingAng) ));
				list.add(new RealPoint(
						  srcCentre.getDoublePosition(0) + latRadius * Math.cos((double)s * stepsAng),
						  srcCentre.getDoublePosition(1) + latRadius * Math.sin((double)s * stepsAng),
						  srcCentre.getDoublePosition(2) - srcRadius * Math.sin((double)l * latSteppingAng) ));
			}
		}

		return list;
	}
}
