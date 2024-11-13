package org.mastodon.benchmark;

import java.awt.Dimension;
import java.nio.file.Paths;

public class BenchmarkInstructions {
	public String bdvSettingsXmlFilename = "benchmark_settings.xml";
	public String tsBookmarksFilename = "bookmarks_trackscheme.txt";
	public void suggestTsBookmarksFilename() {
		tsBookmarksFilename = Paths.get(bdvSettingsXmlFilename)
				  .getParent()
				  .resolve("bookmarks_trackscheme.txt")
				  .toString();
	}
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
