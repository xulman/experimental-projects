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
package org.mastodon;

import ai.nets.samj.bdv.promptresponders.ReportImageOnConsoleResponder;
import ai.nets.samj.bdv.promptresponders.SamjResponder;
import ai.nets.samj.communication.model.SAM2Tiny;
import bdv.interactive.prompts.BdvPrompts;
import bdv.interactive.prompts.planarshapes.PlanarPolygonIn3D;
import bdv.viewer.SourceAndConverter;
import net.imglib2.type.numeric.real.FloatType;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.views.bdv.MamutViewBdv;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class Samj {
	public static void installSamjBdv(final ProjectModel projectModel) {
		List<MamutViewBdv> openedBdvs = projectModel.getWindowManager().getViewList(MamutViewBdv.class);
		if (openedBdvs.size() == 0) {
			System.out.println("Please, first open _one_ BDV window.");
			return;
		}

		final MamutViewBdv mBdv = openedBdvs.get(0);
		SourceAndConverter<?> sac = projectModel.getSharedBdvData().getSources().get(0);
		final BdvPrompts<?, FloatType> samj = new BdvPrompts<>(
				  mBdv.getViewerPanelMamut(),
				  (SourceAndConverter)sac,
				  projectModel.getSharedBdvData().getConverterSetups().getConverterSetup(sac),
				  mBdv.getFrame().getTriggerbindings(),
				  "SAMJ-based detector",
				  new FloatType(), false );

		try {
			//samj.enableShowingPolygons();

			samj.addPromptsProcessor( new ReportImageOnConsoleResponder<>() );
			//samj.addPromptsProcessor( new FakeResponder<>() );
			SamjResponder<?> nn = new SamjResponder<>( new SAM2Tiny() );
			nn.returnLargestRoi = true;
			samj.addPromptsProcessor( (SamjResponder)nn );

			//NB: creates Mastodon Spot at polygon's centre
			samj.addPolygonsConsumer(new Consumer<PlanarPolygonIn3D>() {
				final double[] pos = new double[3];
				@Override
				public void accept(PlanarPolygonIn3D p) {
					if (p.size() == 0) return;
					//NB: the polygon is defined in the "embedded" 2D plane, thus:
					//    1st and 2nd coords zeroed for computing "stats"
					//    3rd coord equals 0.0 (to be "on the plane")
					pos[0] = pos[1] = pos[2] = 0.0;
					for (double[] xy : p.getAllCorners()) {
						pos[0] += xy[0];
						pos[1] += xy[1];
					}
					pos[0] /= p.size();
					pos[1] /= p.size();
					//System.out.println("Polygon view 2D centre position: ["+pos[0]+","+pos[1]+"]");
					p.getTransformTo3d().apply(pos, pos);
					//System.out.println("Polygon orig 3D centre position: ["+pos[0]+","+pos[1]+","+pos[2]+"]");
					//
					final int currTime = mBdv.getViewerPanelMamut().state().getCurrentTimepoint();
					projectModel.getModel().getGraph().addVertex().init(currTime, pos, 5.0);
				}
			});
		} catch (IOException | InterruptedException e) {
			System.out.println("Not using a network, because of the error: "+e.getMessage());
		}
	}
}
