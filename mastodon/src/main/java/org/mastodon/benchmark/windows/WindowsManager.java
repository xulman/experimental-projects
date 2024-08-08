package org.mastodon.benchmark.windows;

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
		BdvViewRotator rotator = new BdvViewRotator(bdv.getViewerPanelMamut());
		rotator.setOneStepAngle_deg(rotationStepDeg);
		rotator.prepareForRotations();
		for (int i = 0; i < rotationStepsCnt; ++i) {
			rotator.rotateOneStep();
			//TODO: waitThisLong(delaysInMillis, "after one step of rotations");
		}
	}

	public void visitBookmarkBDV(final MamutViewBdv bdv, final String bookmarkKey) {
		AffineTransform3D t = projectModel.getSharedBdvData().getBookmarks().get(bookmarkKey);
		if (t != null) {
			bdv.getViewerPanelMamut().state().setViewerTransform(t);
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
