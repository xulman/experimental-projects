package org.mastodon.benchmark.windows;

import bdv.util.BdvHandle;
import bdv.viewer.ViewerPanel;
import net.imglib2.realtransform.AffineTransform3D;
import java.awt.*;

public class BdvViewRotator implements Runnable, MultipleStepsCommand {
	public BdvViewRotator(final BdvHandle bdv) {
		viewerPanel = bdv.getViewerPanel();
	}
	public BdvViewRotator(final ViewerPanel bdvViewerPanel) {
		viewerPanel = bdvViewerPanel;
	}

	private final ViewerPanel viewerPanel;
	private final AffineTransform3D transform = new AffineTransform3D();

	private double cX,cY;
	private final AffineTransform3D rotatorAlongY= new AffineTransform3D();

	public void prepareForRotations() {
		final Dimension displaySize = viewerPanel.getDisplayComponent().getSize();
		cX = displaySize.getWidth() / 2;
		cY = displaySize.getHeight() / 2;

		rotatorAlongY.set(1,0,0,0, 0,1,0,0, 0,0,1,0);
		rotatorAlongY.rotate(1, this.oneRotStepRad);
	}

	public void rotateOneStep() {
		viewerPanel.state().getViewerTransform(transform);

		// center shift
		transform.set( transform.get( 0, 3 ) - cX, 0, 3 );
		transform.set( transform.get( 1, 3 ) - cY, 1, 3 );

		transform.preConcatenate( rotatorAlongY );

		// center un-shift
		transform.set( transform.get( 0, 3 ) + cX, 0, 3 );
		transform.set( transform.get( 1, 3 ) + cY, 1, 3 );

		viewerPanel.state().setViewerTransform(transform);
	}


	private double oneRotStepRad = 0.314159;

	public void setOneStepAngle_deg(final double angle) {
		oneRotStepRad = Math.PI * angle / 180.0;
	}
	public void setOneStepAngle_rad(final double angle) {
		oneRotStepRad = angle;
	}


	@Override
	public void run() {
		prepareForRotations();
		rotateOneStep();
	}


	//stuff for the MultipleStepsCommand iface
	public void planForRotationSteps(final int stepsToRotate) {
		numberOfExecutedSteps = 0;
		numberOfRemainingSteps = stepsToRotate;
	}

	private int numberOfExecutedSteps = 0;
	private int numberOfRemainingSteps = 0;

	@Override
	public boolean hasNext() {
		return numberOfRemainingSteps > 0;
	}

	@Override
	public void doNext() {
		rotateOneStep();
		numberOfExecutedSteps++;
		numberOfRemainingSteps--;
	}

	@Override
	public String reportCurrentStep() {
		int nextStep = numberOfExecutedSteps+1;
		return viewerPanel.getDisplay().getDisplayName()+" view rotates "
				+nextStep+"/"+(numberOfExecutedSteps+numberOfRemainingSteps)
				+" to "+(nextStep*oneRotStepRad*180.0/3.14159)+" deg";
	}
}
