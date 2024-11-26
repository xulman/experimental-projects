package org.mastodon.benchmark.windows;

import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.views.trackscheme.ScreenTransform;

public class TsViewsTransition implements MultipleStepsCommand {
	public TsViewsTransition(final MamutViewTrackScheme trackSchemeWindow,
	                         final ScreenTransform fromView,
	                         final ScreenTransform toView,
	                         final int steps) {
		this.associatedTS = trackSchemeWindow;

		this.fromST = fromView;
		this.steps_done = 1;
		this.steps_total = steps > 0 ? steps : 1;

		dxMinX = (toView.getMinX() - fromST.getMinX()) / (double)steps_total;
		dxMaxX = (toView.getMaxX() - fromST.getMaxX()) / (double)steps_total;
		dyMinY = (toView.getMinY() - fromST.getMinY()) / (double)steps_total;
		dyMaxY = (toView.getMaxY() - fromST.getMaxY()) / (double)steps_total;
		currST = fromST.copy();
	}

	private final MamutViewTrackScheme associatedTS;
	private final ScreenTransform fromST, currST;
	private final int steps_total;
	private int steps_done;
	final double dxMinX, dxMaxX, dyMinY, dyMaxY;

	@Override
	public boolean hasNext() {
		return steps_done <= steps_total;
	}

	@Override
	public void doNext() {
		final double r = steps_done;
		steps_done++;

		currST.set(
				  fromST.getMinX()+ r*dxMinX,
				  fromST.getMaxX()+ r*dxMaxX,
				  fromST.getMinY()+ r*dyMinY,
				  fromST.getMaxY()+ r*dyMaxY,
				  fromST.getScreenWidth(), fromST.getScreenHeight() );

		associatedTS.getFrame().getTrackschemePanel().getScreenTransform().set(currST);
	}

	@Override
	public String reportCurrentStep() {
		return associatedTS.getFrame().getTrackschemePanel().getDisplay().getDisplayName()
				+" travelling "+steps_done+"/"+steps_total+" steps";
	}
}
