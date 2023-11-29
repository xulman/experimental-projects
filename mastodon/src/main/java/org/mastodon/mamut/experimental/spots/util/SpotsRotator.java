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

	final double[][] sBaseMatrix;
	final double[] b = new double[3];
	final Vector3f tx,ty,tz;
	final Vector3f sx,sy,sz;

	public void rotateSpot(final Spot s) {
		s.localize(b);
		b[0] -= sourceCentreCoord[0];
		b[1] -= sourceCentreCoord[1];
		b[2] -= sourceCentreCoord[2];

		GaussianElimination gaussian = new GaussianElimination(sBaseMatrix, b);
		if (gaussian.isFeasible()) {
			final double[] x = gaussian.primal();
			/*
			coord[0] = (float)(x[0]*sx.x + x[2]*sy.x + x[1]*sz.x);
			coord[1] = (float)(x[0]*sx.y + x[2]*sy.y + x[1]*sz.y);
			coord[2] = (float)(x[0]*sx.z + x[2]*sy.z + x[1]*sz.z);

			System.out.println("Spot "+s.getLabel()+" at absolute position ["
					+s.getFloatPosition(0)+","+s.getFloatPosition(1)+","+s.getFloatPosition(2)
					+"] is at coords "+x[0]+","+x[1]+","+x[2]);
			System.out.println("Spot "+s.getLabel()+" at relative position ["+b[0]+","+b[1]+","+b[2]
					+"] is at coords "+x[0]+","+x[1]+","+x[2]);
			System.out.println("Spot "+s.getLabel()+", rel. pos. from coords ["+coord[0]+","+coord[1]+","+coord[2]+"]");
			*/

			coord[0] = (float)(x[0]*tx.x + x[2]*ty.x + x[1]*tz.x);
			coord[1] = (float)(x[0]*tx.y + x[2]*ty.y + x[1]*tz.y);
			coord[2] = (float)(x[0]*tx.z + x[2]*ty.z + x[1]*tz.z);
		} else {
			//just keep the original relative coordinate...
			coord[0] = (float)b[0];
			coord[1] = (float)b[1];
			coord[2] = (float)b[2];
		}

		coord[0] += targetCentreCoord[0];
		coord[1] += targetCentreCoord[1];
		coord[2] += targetCentreCoord[2];
		s.setPosition(coord);
	}

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
		sx = (new Vector3f(coord)).sub(centre); //.normalize();
		//
		setting.sU.localize(coord);
		sz = (new Vector3f(coord)).sub(centre); //.normalize();
		//
		sy = new Vector3f();
		sx.cross(sz, sy); //sy = sx x sz
		sy.normalize();

		System.out.println("Right vec (x): ("+sx.x+","+sx.y+","+sx.z+")");
		System.out.println("Up vec    (y): ("+sz.x+","+sz.y+","+sz.z+")");
		System.out.println("3rd vec   (z): ("+sy.x+","+sy.y+","+sy.z+")");

		sBaseMatrix = new double[][] {
				{sx.x, sz.x, sy.x},
				{sx.y, sz.y, sy.y},
				{sx.z, sz.z, sy.z} };

		setting.tC.localize(targetCentreCoord);
		centre.set(
				(float)targetCentreCoord[0],
				(float)targetCentreCoord[1],
				(float)targetCentreCoord[2] );
		//
		setting.tR.localize(coord);
		tx = (new Vector3f(coord)).sub(centre); //.normalize();
		//
		setting.tU.localize(coord);
		tz = (new Vector3f(coord)).sub(centre); //.normalize();
		//
		ty = new Vector3f();
		tx.cross(tz, ty); //ty = tx x tz
		ty.normalize();
	}
}
