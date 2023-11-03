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

import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.mastodon.collection.RefCollections;
import org.mastodon.collection.RefSet;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.experimental.spots.util.SpotsRotator;
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

@Plugin( type = Command.class, name = "General-rotate spots in space" )
public class RotateSpotsGeneral implements Command {

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
		try {
			final SpotsRotator.InitialSetting setting = new SpotsRotator.InitialSetting();
			setting.appModel = this.appModel;
			setting.sC = getCoordinateFromMastodonSpot(appModel, sourceCentre, sourceTime);
			setting.sR = getCoordinateFromMastodonSpot(appModel, sourceRight, sourceTime);
			setting.sU = getCoordinateFromMastodonSpot(appModel, sourceUp, sourceTime);
			//
			setting.tC = getCoordinateFromMastodonSpot(appModel, targetCentre, targetTime);
			setting.tR = getCoordinateFromMastodonSpot(appModel, targetRight, targetTime);
			setting.tU = getCoordinateFromMastodonSpot(appModel, targetUp, targetTime);
			log.info("Found all six spots, good.");

			execute(setting, chosenTimepoints, log);
		} catch (IllegalArgumentException e) {
			log.error("Error running the plugin: " + e.getMessage());
		}
	}

	static public void execute(
			final SpotsRotator.InitialSetting setting,
			final String chosenTimepoints,
			final Logger log) {

		final ReentrantReadWriteLock lock = setting.appModel.getModel().getGraph().getLock();
		lock.writeLock().lock();
		try {
			final SpotsRotator transform = new SpotsRotator(setting);
			log.info("Created transform: "+transform);

			if (setting.appModel.getSelectionModel().isEmpty()) {
				final Set<Integer> timepoints = new HashSet<>(1000);
				NumberSequenceHandler.parseSequenceOfNumbers(chosenTimepoints,timepoints);
				log.info("Going to rotate spots from time points: "+timepoints);

				final RefSet<Spot> spots = RefCollections.createRefSet(setting.appModel.getModel().getGraph().vertices());
				timepoints.forEach(t -> {
					setting.appModel.getModel().getSpatioTemporalIndex().getSpatialIndex(t).forEach(spots::add);
					spots.forEach(transform::rotateSpot);
					spots.clear();
					} );
			} else {
				log.info("Going to rotate selected spots only.");
				setting.appModel.getSelectionModel().getSelectedVertices().forEach(transform::rotateSpot);
			}
		} catch (ParseException e) {
			log.error("Error parsing the time points field: " + e.getMessage());
		} catch (Exception e) {
			log.error("Error running the plugin: " + e.getMessage());
		} finally {
			lock.writeLock().unlock();
		}
	}

	static public RealLocalizable getCoordinateFromMastodonSpot(
			final MamutAppModel mastodonAppModel,
			final String spotLabel,
			final int spotTimepoint)
	throws IllegalArgumentException {

		if (mastodonAppModel == null || spotLabel == null) {
			throw new IllegalArgumentException("No AppModel or spotLabel provided.");
		}

		final RealPoint p = new RealPoint(3);
		p.setPosition(Float.NEGATIVE_INFINITY, 0); //flag to see if a spot has been found

		mastodonAppModel.getModel().getSpatioTemporalIndex()
				.getSpatialIndex(spotTimepoint)
				.forEach(s -> {
					if (s.getLabel().equals(spotLabel)) {
						p.setPosition( s );
					}
				});

		if (p.getFloatPosition(0) == Float.NEGATIVE_INFINITY) {
			throw new IllegalArgumentException("Spot \""+spotLabel+"\" not found in time point "+spotTimepoint+".");
		}

		return p;
	}
}
