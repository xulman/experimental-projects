package org.mastodon.benchmark.windows;

import bdv.viewer.animate.SimilarityTransformAnimator;
import net.imglib2.realtransform.AffineTransform3D;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.views.MamutViewI;
import org.mastodon.mamut.views.bdv.MamutViewBdv;
import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.model.tag.TagSetStructure;

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

		//enable the first available TagSet
		List<TagSetStructure.TagSet> tagSets = projectModel.getModel().getTagSetModel().getTagSetStructure().getTagSets();
		if (tagSets.size() > 0) {
			bdv.getColoringModel().colorByTagSet(tagSets.get(0));
		}

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

		//enable the first available TagSet
		List<TagSetStructure.TagSet> tagSets = projectModel.getModel().getTagSetModel().getTagSetStructure().getTagSets();
		if (tagSets.size() > 0) {
			ts.getColoringModel().colorByTagSet(tagSets.get(0));
		}

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


	public MultipleStepsCommand rotateBDV(final MamutViewBdv bdv, final double rotationStepDeg, final int rotationStepsCnt) {
		BdvViewRotator rotator = new BdvViewRotator(bdv.getViewerPanelMamut());
		rotator.setOneStepAngle_deg(rotationStepDeg);
		rotator.prepareForRotations();
		rotator.planForRotationSteps(rotationStepsCnt);
		return rotator;
	}

	public void visitBookmarkBDV(final MamutViewBdv bdv, final String bookmarkKey) {
		//try to retrieve the bookmark data in the first place
		AffineTransform3D tgt = projectModel.getSharedBdvData().getBookmarks().get(bookmarkKey);
		if (tgt != null) {
			final AffineTransform3D src = new AffineTransform3D();
			bdv.getViewerPanelMamut().state().getViewerTransform( src );
			//
			double cX = bdv.getViewerPanelMamut().getDisplayComponent().getWidth() / 2.0;
			double cY = bdv.getViewerPanelMamut().getDisplayComponent().getHeight() / 2.0;
			/*
			//alternative way of retrieving the image view centre,
			//"116" is "linux constant", might be that on another OS the window
			//decoration will need different number of pixels
			System.out.println("cX = "+cX+", cY = "+cY);
			cX = bdv.getFrame().getSize().getWidth() / 2.0;
			cY = (bdv.getFrame().getSize().getHeight()-116) / 2.0;
			System.out.println("cX = "+cX+", cY = "+cY);
			*/
			src.set( src.get( 0, 3 ) - cX, 0, 3 );
			src.set( src.get( 1, 3 ) - cY, 1, 3 );
			//
			bdv.getViewerPanelMamut().state().setViewerTransform(
				new SimilarityTransformAnimator(src, tgt, cX, cY, 3000).get(1.0) );
			//TODO: this should happen w/o the TransformAnimator, I'm currently outsourcing the math-work to it...
		} else {
			System.out.println("Bookmark '"+bookmarkKey+"' was not found in the current project, skipping it.");
		}
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
