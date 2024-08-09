package org.mastodon.benchmark.windows;

import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.views.MamutViewI;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;

import java.awt.*;
import java.util.List;

public class WindowsManager {
	public WindowsManager(final ProjectModel project) {
		this.projectModel = project;
		this.resetBdvLocations(512); // 512 is an optimistic guess.... nothing more
	}
	private final ProjectModel projectModel;

	public void closeAllWindows() {
		projectModel.getWindowManager().closeAllWindows();
	}

	public MamutViewBdv openBDV(final String winTitle, final Dimension winSize, final Integer lockGroupId) {
		final MamutViewBdv bdv = projectModel.getWindowManager().createView(MamutViewBdv.class);

		//screen visible stuff
		bdv.getFrame().setTitle(winTitle);
		bdv.getFrame().setSize(winSize);
		bdv.getFrame().setLocation(bdvCurrentXlocation, bdvCurrentYlocation);
		bdvCurrentXlocation += winSize.width + WINDOWS_GAP;
		if (lockGroupId != null) bdv.getGroupHandle().setGroupId(lockGroupId);

		//title for the time-needed-to-draw reports
		bdv.getViewerPanelMamut().getDisplay().setDisplayName(winTitle);

		return bdv;
	}

	public MamutViewTrackScheme openTS(final String winTitle, final Dimension winSize, final Integer lockGroupId) {
		final MamutViewTrackScheme ts = projectModel.getWindowManager().createView(MamutViewTrackScheme.class);

		//screen visible stuff
		ts.getFrame().setTitle(winTitle);
		ts.getFrame().setSize(winSize);
		ts.getFrame().setLocation(tsCurrentXlocation, tsCurrentYlocation);
		tsCurrentXlocation += winSize.width + WINDOWS_GAP;
		if (lockGroupId != null) ts.getGroupHandle().setGroupId(lockGroupId);

		//title for the time-needed-to-draw reports
		ts.getFrame().getTrackschemePanel().getDisplay().setDisplayName(winTitle);

		return ts;
	}


	private int bdvCurrentXlocation, bdvCurrentYlocation;
	private int tsCurrentXlocation, tsCurrentYlocation;
	static int WINDOWS_GAP = 10;

	public void resetBdvLocations(int heightOfBDVs) {
		bdvCurrentXlocation = WINDOWS_GAP;
		bdvCurrentYlocation = WINDOWS_GAP;
		tsCurrentXlocation = WINDOWS_GAP;
		tsCurrentYlocation = WINDOWS_GAP + heightOfBDVs + WINDOWS_GAP;
	}


	public void rotateBDV(final MamutViewBdv bdv, final double rotationStepDeg, final int rotationStepsCnt) {
		System.out.println("Rotations in angle "+rotationStepDeg+" deg in total steps "+rotationStepsCnt);
		BdvViewRotator rotator = new BdvViewRotator(bdv.getViewerPanelMamut());
		rotator.setOneStepAngle_deg(rotationStepDeg);
		rotator.prepareForRotations();
		for (int i = 0; i < rotationStepsCnt; ++i) {
			rotator.rotateOneStep();
			//TODO: waitThisLong(delaysInMillis, "after one step of rotations");
			try { Thread.sleep(1000); }
			catch (InterruptedException e) { /* empty */ }
		}
	}

	public void visitBookmarkBDV(final MamutViewBdv bdv, final String bookmarkKey) {
		//try to retrieve the bookmark data in the first place
		AffineTransform3D tgt = projectModel.getSharedBdvData().getBookmarks().get(bookmarkKey);
		if (tgt != null) {
			final AffineTransform3D src = new AffineTransform3D();
			bdv.getViewerPanelMamut().state().getViewerTransform( src );
			final double cX = bdv.getViewerPanelMamut().getDisplayComponent().getWidth() / 2.0;
			final double cY = bdv.getViewerPanelMamut().getDisplayComponent().getHeight() / 2.0;
			src.set( src.get( 0, 3 ) - cX, 0, 3 );
			src.set( src.get( 1, 3 ) - cY, 1, 3 );
			if ( bdv.getFrame().getTitle().contains("#1") ) {
				System.out.println("bdv1 doing animated rotation");
				final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(src, tgt, cX, cY, 10000);
				bdv.getViewerPanelMamut().setTransformAnimator(animator);
				for (int i = 0; i < 15; ++i) {
					System.out.println("ratio complete = "+animator.ratioComplete()+", isComplete = "+animator.isComplete());
					pause(1000);
				}
			} else {
				System.out.println("bdv2 doing three-steps, just-end rotation");
				final SimilarityTransformAnimator animator = new SimilarityTransformAnimator(src, tgt, cX, cY, 3000);
				animator.setTime( System.currentTimeMillis() );

				bdv.getViewerPanelMamut().state().setViewerTransform(animator.get(0.25));
				System.out.println("after step #1");
				pause(2000);

				bdv.getViewerPanelMamut().state().setViewerTransform(animator.get(0.50));
				System.out.println("after step #2");
				pause(2000);

				bdv.getViewerPanelMamut().state().setViewerTransform(animator.get(0.75));
				System.out.println("after step #3");
				pause(2000);

				bdv.getViewerPanelMamut().state().setViewerTransform(animator.get(1.0));
				System.out.println("after step #4");
				pause(2000);

				bdv.getViewerPanelMamut().state().setViewerTransform(animator.get(1.0));
				System.out.println("after step #4");
			}
		} else {
			System.out.println("Bookmark '"+bookmarkKey+"' was not found in the current project, skipping it.");
		}
	}

	private void pause(long millis) {
		try { Thread.sleep(millis); }
		catch (InterruptedException e) { /* nothing */ }
	}

	public void changeTimepoint(final List<MamutViewI> windows, final int setTP) {
		//System.out.println("Asking all "+windows.size()+" windows to switch to time point "+setTP);
		windows.forEach( w -> w.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP) );
	}

	//TODO: why this goes via the group handle !?!?
	public void changeTimepoint(final MamutViewI window, final int setTP) {
		//System.out.println("Asking a given window to switch to time point "+setTP);
		window.getGroupHandle().getModel(projectModel.TIMEPOINT).setTimepoint(setTP);
	}
}
