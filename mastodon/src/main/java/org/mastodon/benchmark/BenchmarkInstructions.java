package org.mastodon.benchmark;

import java.awt.*;

public class BenchmarkInstructions {
	public String bdvSettingsXmlFilename = "benchmark_settings.xml";
	public boolean shouldCloseAllWindowsBeforeBenchmark = true;

	public int howManyTSsToOpen = 0;
	public Dimension windowSizeOfBDVs = new Dimension(512,512);

	public int howManyBDVsToOpen = 0;
	public Dimension windowSizeOfTSs = new Dimension(512,512);

	public boolean shouldLockButtonsLinkOpenedWindows = false;

	public String benchmarkInitializationSequence = "";
	public String benchmarkExecutionSequence = "";

	public long millisToWaitAfterInitialization = 5000;
	public long millisToWaitAfterEachBenchmarkAction = 3000;
}
