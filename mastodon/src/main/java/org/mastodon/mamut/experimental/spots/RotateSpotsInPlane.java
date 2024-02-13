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

import net.imglib2.RealPoint;
import org.joml.Vector3f;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.experimental.spots.util.SpotsRotator;
import org.scijava.ItemVisibility;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.log.Logger;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin( type = Command.class, name = "Simpler-rotate spots in space" )
public class RotateSpotsInPlane implements Command {

	@Parameter(visibility = ItemVisibility.MESSAGE)
	private final String selectionInfoMsg = "...also only selected ones.";

	@Parameter(label = "Coordinate system at time point: ",
			callback = "checkTimePoints")
	int sourceTime;

	@Parameter(label = "Label of the centre spot: ")
	String sourceCentre;

	@Parameter(label = "Label of the \"from\" spot: ")
	String sourceFrom;

	@Parameter(label = "Label of the \"to\" spot: ")
	String sourceTo;

	private void checkTimePoints() {
		sourceTime = Math.max(appModel.getMinTimepoint(), Math.min(sourceTime, appModel.getMaxTimepoint()) );
	}

	@Parameter(label = "If no spots are selected, which time points to process:",
			description = "Accepted format is comma-separated number or interval: A-B,C,D,E-F,G",
			required = false)
	String chosenTimepoints = "0,1-5,6,7-9";

	@Parameter(persist = false)
	ProjectModel appModel;

	@Parameter
	LogService logService;

	@Override
	public void run() {
		final Logger log = logService.subLogger("ShiftAndRotateMastodonPoints");
		try {
			final SpotsRotator.InitialSetting setting = new SpotsRotator.InitialSetting();
			setting.appModel = this.appModel;
			setting.sC = RotateSpotsGeneral.getCoordinateFromMastodonSpot(appModel, sourceCentre, sourceTime);
			setting.sR = RotateSpotsGeneral.getCoordinateFromMastodonSpot(appModel, sourceFrom, sourceTime);
			setting.tR = RotateSpotsGeneral.getCoordinateFromMastodonSpot(appModel, sourceTo, sourceTime);
			log.info("Found all three spots, good.");

			final float[] coord = new float[3];
			setting.sC.localize(coord);
			Vector3f centre = new Vector3f(coord);
			setting.sR.localize(coord);
			Vector3f vecA = (new Vector3f(coord)).sub(centre);
			setting.tR.localize(coord);
			Vector3f vecB = (new Vector3f(coord)).sub(centre);
			vecA.cross(vecB).normalize(100.f);
			centre.add(vecA);
			setting.sU = new RealPoint(centre.x, centre.y, centre.z);

			setting.tC = setting.sC;
			setting.tU = setting.sU;

			RotateSpotsGeneral.execute(setting, chosenTimepoints, log);
		} catch (IllegalArgumentException e) {
			log.error("Error running the plugin: " + e.getMessage());
		}
	}
}
