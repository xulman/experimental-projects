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

import org.joml.Vector3f;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Set;
import java.util.HashSet;
import java.text.ParseException;
import org.ulman.util.NumberSequenceHandler;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Plugin( type = Command.class, name = "Rotate spots in space" )
public class RotateSpots implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...also only selected ones.";

	@Parameter(label = "Source coordinate system, time point: ",
			callback = "checkTimePoints")
	int sourceTime;

	@Parameter(label = "Label of the centre spot: ")
	String sourceCentre;

	@Parameter(label = "Label of the horizontal spot: ")
	String sourceRight;

	@Parameter(label = "Label of the vertical spot: ")
	String sourceUp;

	@Parameter(label = "Target coordinate system, time point: ",
			callback = "checkTimePoints")
	int targetTime;

	private void checkTimePoints() {
		sourceTime = Math.max(appModel.getMinTimepoint(), Math.min(sourceTime, appModel.getMaxTimepoint()) );
		targetTime = Math.max(appModel.getMinTimepoint(), Math.min(targetTime, appModel.getMaxTimepoint()) );
	}

	@Parameter(label = "Label of the centre spot: ")
	String targetCentre;

	@Parameter(label = "Label of the horizontal spot: ")
	String targetRight;

	@Parameter(label = "Label of the vertical spot: ")
	String targetUp;

	@Parameter(label = "If no spots are selected, which time points to process:",
			description = "Accepted format is comma-separated number or interval: A-B,C,D,E-F,G",
			required = false)
	String chosenTimepoints = "0,1-5,6,7-9";

	@Parameter(persist = false)
	MamutAppModel appModel;

	@Parameter
	LogService logService;

	@Override
	public void run() {
		final Logger log = logService.subLogger("ShiftAndRotateMastodonPoints");

		final ReentrantReadWriteLock lock = appModel.getModel().getGraph().getLock();
		lock.writeLock().lock();
		try {
			setUpTheTransforming();
			log.info("Created transform: "+transform);

			if (appModel.getSelectionModel().isEmpty()) {
				final Set<Integer> timepoints = new HashSet<>(1000);
				NumberSequenceHandler.parseSequenceOfNumbers(chosenTimepoints,timepoints);
				log.info("Going to rotate spots from time points: "+timepoints);

				final RefSet<Spot> spots = RefCollections.createRefSet(appModel.getModel().getGraph().vertices());
				timepoints.forEach(t -> {
					appModel.getModel().getSpatioTemporalIndex().getSpatialIndex(t).forEach(spots::add);
					spots.forEach(this::rotateSpot);
					spots.clear();
					} );
			} else {
				log.info("Going to rotate selected spots only.");
				appModel.getSelectionModel().getSelectedVertices().forEach(this::rotateSpot);
			}
		} catch (IllegalArgumentException e) {
			log.error("Error running the plugin: " + e.getMessage());
		} catch (ParseException e) {
			log.error("Error parsing the time points field: " + e.getMessage());
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void rotateSpot(final Spot s) {
		s.localize(coord);
		coord[0] -= sourceCentreCoord[0];
		coord[1] -= sourceCentreCoord[1];
		coord[2] -= sourceCentreCoord[2];
		transform.apply(coord,coord);
		coord[0] += targetCentreCoord[0];
		coord[1] += targetCentreCoord[1];
		coord[2] += targetCentreCoord[2];
		s.setPosition(coord);
	}

	AffineTransform3D transform;
	double[] sourceCentreCoord = new double[3];
	double[] targetCentreCoord = new double[3];
	float[] coord = new float[3];

	private void setUpTheTransforming()
	throws IllegalArgumentException {
		RealLocalizable sC,sR,sU, tC,tR,tU;
		sC = getCoordinate(sourceCentre, sourceTime);
		sR = getCoordinate(sourceRight, sourceTime);
		sU = getCoordinate(sourceUp, sourceTime);

		tC = getCoordinate(targetCentre, targetTime);
		tR = getCoordinate(targetRight, targetTime);
		tU = getCoordinate(targetUp, targetTime);
		//log.info("Found all six spots, good.");

		sC.localize(sourceCentreCoord);
		final Vector3f centre = new Vector3f(
				(float)sourceCentreCoord[0],
				(float)sourceCentreCoord[1],
				(float)sourceCentreCoord[2] );
		//
		sR.localize(coord);
		final Vector3f sx = (new Vector3f(coord)).sub(centre).normalize();
		//
		sU.localize(coord);
		final Vector3f sz = (new Vector3f(coord)).sub(centre).normalize();
		//
		final Vector3f sy = new Vector3f();
		sx.cross(sz, sy); //sy = sx x sz
		sy.normalize();

		tC.localize(targetCentreCoord);
		centre.set(
				(float)targetCentreCoord[0],
				(float)targetCentreCoord[1],
				(float)targetCentreCoord[2] );
		//
		tR.localize(coord);
		final Vector3f tx = (new Vector3f(coord)).sub(centre).normalize();
		//
		tU.localize(coord);
		final Vector3f tz = (new Vector3f(coord)).sub(centre).normalize();
		//
		final Vector3f ty = new Vector3f();
		tx.cross(tz, ty); //ty = tx x tz
		ty.normalize();

		AffineTransform3D sourceCoord = new AffineTransform3D();
		sourceCoord.set(sx.x,sx.y,sx.z,0, sy.x,sy.y,sy.z,0, sz.x,sz.y,sz.z,0, 0,0,0,1);
		transform = new AffineTransform3D();
		transform.set(tx.x,ty.x,tz.x,0, tx.y,ty.y,tz.y,0, tx.z,ty.z,tz.z,0, 0,0,0,1);
		transform.concatenate(sourceCoord);
	}

	private RealLocalizable getCoordinate(final String spotLabel, final int timepoint)
			throws IllegalArgumentException {
		final RealPoint p = new RealPoint(3);
		p.setPosition(Float.NEGATIVE_INFINITY, 0); //flag to see if a spot has been found

		appModel.getModel().getSpatioTemporalIndex()
				.getSpatialIndex(timepoint)
				.forEach(s -> {
					if (s.getLabel().equals(spotLabel)) {
						p.setPosition( s );
					}
				});

		if (p.getFloatPosition(0) == Float.NEGATIVE_INFINITY) {
			throw new IllegalArgumentException("Spot \""+spotLabel+"\" not found in time point "+timepoint+".");
		}

		return p;
	}
}
