package org.ulman.simulator.ui;

import javax.swing.*;
import java.awt.*;

public class ProgressBar {
	public ProgressBar(final int min, final int max, final String label) {
		//creates the dialog
		mainFrame = new JFrame("Simulation progress bar");
		mainFrame.setLayout(new GridBagLayout());

		final GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(10,10,10,10);
		c.gridwidth = 2;
		c.gridx = 1;
		c.gridy = 1;

		pbElem = new JProgressBar(min,max);
		mainFrame.add(pbElem,c);

		c.insets = new Insets(0,10,10,10);
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 1;

		labelElem = new JLabel(label);
		mainFrame.add(labelElem,c);

		c.gridx = 2;

		JButton button = new JButton("Stop");
		button.addActionListener(l -> isStopBtnPressed = true);
		mainFrame.add(button,c);

		mainFrame.pack();
		mainFrame.setVisible(true);
	}

	private final JFrame mainFrame;
	private final JProgressBar pbElem;
	private final JLabel labelElem;
	private boolean isStopBtnPressed = false;

	public boolean isStop() {
		return isStopBtnPressed;
	}

	public void updateLabel(final String newLabel) {
		labelElem.setText(newLabel);
	}

	public void setProgress(final int val) {
		pbElem.setValue(val);
	}

	public void close() {
		mainFrame.setVisible(false);
		mainFrame.dispose();
	}
}
