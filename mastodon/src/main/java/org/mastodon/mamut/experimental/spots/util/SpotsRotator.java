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
package org.mastodon.mamut.experimental.spots.util;

import net.imglib2.RealLocalizable;
import net.imglib2.realtransform.AffineTransform3D;
import org.joml.Vector3f;
import org.mastodon.mamut.MamutAppModel;
import org.mastodon.mamut.model.Spot;

public class SpotsRotator {
	static public class InitialSetting {
		//public int sourceTime;
		//public String sourceCentre;
		//public String sourceRight;
		//public String sourceUp;
		//public int targetTime;
		//public String targetCentre;
		//public String targetRight;
		//public String targetUp;
		public RealLocalizable sC,sR,sU, tC,tR,tU;
		public MamutAppModel appModel;
	}

	public void rotateSpot(final Spot s) {
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

	private final AffineTransform3D transform;
	private final double[] sourceCentreCoord = new double[3];
	private final double[] targetCentreCoord = new double[3];
	private final float[] coord = new float[3];

	public SpotsRotator(final InitialSetting setting)
	throws IllegalArgumentException {

		setting.sC.localize(sourceCentreCoord);
		final Vector3f centre = new Vector3f(
				(float)sourceCentreCoord[0],
				(float)sourceCentreCoord[1],
				(float)sourceCentreCoord[2] );
		//
		setting.sR.localize(coord);
		final Vector3f sx = (new Vector3f(coord)).sub(centre).normalize();
		//
		setting.sU.localize(coord);
		final Vector3f sz = (new Vector3f(coord)).sub(centre).normalize();
		//
		final Vector3f sy = new Vector3f();
		sx.cross(sz, sy); //sy = sx x sz
		sy.normalize();

		setting.tC.localize(targetCentreCoord);
		centre.set(
				(float)targetCentreCoord[0],
				(float)targetCentreCoord[1],
				(float)targetCentreCoord[2] );
		//
		setting.tR.localize(coord);
		final Vector3f tx = (new Vector3f(coord)).sub(centre).normalize();
		//
		setting.tU.localize(coord);
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

	@Override
	public String toString() {
		return transform.toString();
	}
}
