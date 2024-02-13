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
import org.joml.Vector3f;
import org.mastodon.mamut.ProjectModel;
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
		public ProjectModel appModel;
		public boolean normalizeBase = true;
	}

	final double[][] sBaseMatrix;
	final Vector3f tx,ty,tz;
	final Vector3f sx,sy,sz;

	public void rotateSpot(final Spot s) {
		s.localize(coord);
		coord[0] -= sourceCentreCoord[0];
		coord[1] -= sourceCentreCoord[1];
		coord[2] -= sourceCentreCoord[2];

		final double[] x = new GaussianElimination(sBaseMatrix, coord).primal();
		if (x != null) {
			/*
			System.out.println("Spot "+s.getLabel()+" at absolute position ["
					+s.getFloatPosition(0)+","+s.getFloatPosition(1)+","+s.getFloatPosition(2)
					+"] is at coords "+x[0]+","+x[1]+","+x[2]);
			System.out.println("Spot "+s.getLabel()+" at relative position ["+coord[0]+","+coord[1]+","+coord[2]
					+"] is at coords "+x[0]+","+x[1]+","+x[2]);
			coord[0] = x[0]*sx.x + x[1]*sy.x + x[2]*sz.x;
			coord[1] = x[0]*sx.y + x[1]*sy.y + x[2]*sz.y;
			coord[2] = x[0]*sx.z + x[1]*sy.z + x[2]*sz.z;
			System.out.println("Spot "+s.getLabel()+", rel. pos. from coords ["+coord[0]+","+coord[1]+","+coord[2]+"]");
			*/

			coord[0] = x[0]*tx.x + x[1]*ty.x + x[2]*tz.x;
			coord[1] = x[0]*tx.y + x[1]*ty.y + x[2]*tz.y;
			coord[2] = x[0]*tx.z + x[1]*ty.z + x[2]*tz.z;
		}
		//else: just keep the original relative coordinate...

		coord[0] += targetCentreCoord[0];
		coord[1] += targetCentreCoord[1];
		coord[2] += targetCentreCoord[2];
		s.setPosition(coord);
	}

	private final float[] sourceCentreCoord = new float[3];
	private final float[] targetCentreCoord = new float[3];
	private final double[] coord = new double[3];

	public SpotsRotator(final InitialSetting setting)
	throws IllegalArgumentException {

		setting.sC.localize(sourceCentreCoord);
		final Vector3f centre = new Vector3f(sourceCentreCoord);
		//
		final float[] coord_ft = new float[3];
		setting.sR.localize(coord_ft);
		sx = (new Vector3f(coord_ft)).sub(centre); //.normalize();
		//
		setting.sU.localize(coord_ft);
		sz = (new Vector3f(coord_ft)).sub(centre); //.normalize();
		//
		if (setting.normalizeBase) {
			sx.normalize();
			sz.normalize();
		}
		//
		sy = new Vector3f();
		sx.cross(sz, sy); //sy = sx x sz
		sy.normalize();

		//TODO: AffineTransformation

		System.out.println("Hrzntl vec (x): ("+sx.x+","+sx.y+","+sx.z+")");
		System.out.println("Side vec   (y): ("+sy.x+","+sy.y+","+sy.z+")");
		System.out.println("Up vec     (z): ("+sz.x+","+sz.y+","+sz.z+")");

		sBaseMatrix = new double[][] {
				{sx.x, sy.x, sz.x},
				{sx.y, sy.y, sz.y},
				{sx.z, sy.z, sz.z} };

		setting.tC.localize(targetCentreCoord);
		centre.set(targetCentreCoord);
		//
		setting.tR.localize(coord_ft);
		tx = (new Vector3f(coord_ft)).sub(centre); //.normalize();
		//
		setting.tU.localize(coord_ft);
		tz = (new Vector3f(coord_ft)).sub(centre); //.normalize();
		//
		if (setting.normalizeBase) {
			tx.normalize();
			tz.normalize();
		}
		//
		ty = new Vector3f();
		tx.cross(tz, ty); //ty = tx x tz
		ty.normalize();
	}
}
