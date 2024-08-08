package org.mastodon.benchmark;

import org.mastodon.mamut.ProjectModel;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import java.awt.*;

@Plugin(type = Command.class, name = "Benchmark GUI", menuPath = "Plugins>Mastodon Benchmark")
public class BenchmarkScijavaGui implements Command {
	@Parameter
	public String mastodonProjectPath = "provide/path/to/project.mastodon";

	@Parameter
	public boolean shouldCloseAllWindowsBeforeBenchmark = true;

	@Parameter
	public int howManyBDVsToOpen = 0;
	@Parameter
	public int windowWidthOfBDVs = 512;
	@Parameter
	public int windowHeightOfBDVs = 512;

	@Parameter
	public int howManyTSsToOpen = 0;
	@Parameter
	public int windowWidthOfTSs = 512;
	@Parameter
	public int windowHeighthOfTSs = 512;

	@Parameter
	public boolean shouldLockButtonsLinkOpenedWindows = false;

	@Parameter
	public String benchmarkInitializationSequence = "";
	@Parameter
	public String benchmarkExecutionSequence = "";

	@Parameter
	public long millisToWaitAfterInitialization = 5000;
	@Parameter
	public long millisToWaitAfterEachBenchmarkAction = 3000;

	@Parameter
	private CommandService contextProviderService;

	public BenchmarkInstructions instructions = new BenchmarkInstructions();

	@Override
	public void run() {
		instructions.shouldCloseAllWindowsBeforeBenchmark = shouldCloseAllWindowsBeforeBenchmark;
		instructions.howManyTSsToOpen = howManyTSsToOpen;
		instructions.windowSizeOfBDVs = new Dimension(windowWidthOfBDVs, windowHeightOfBDVs);
		instructions.howManyBDVsToOpen = howManyBDVsToOpen;
		instructions.windowSizeOfTSs = new Dimension(windowWidthOfTSs, windowHeighthOfTSs);
		instructions.shouldLockButtonsLinkOpenedWindows = shouldLockButtonsLinkOpenedWindows;
		instructions.benchmarkInitializationSequence = benchmarkInitializationSequence;
		instructions.benchmarkExecutionSequence = benchmarkExecutionSequence;
		instructions.millisToWaitAfterInitialization = millisToWaitAfterInitialization;
		instructions.millisToWaitAfterEachBenchmarkAction = millisToWaitAfterEachBenchmarkAction;

		//this is when this GUI is called from inside the Mastodon, which
		//provides its own context and project, and executes the benchmark as well
		if (mastodonProjectPath.startsWith("don't execute")) return;

		//for not-inside-Mastodon world:
		ProjectModel p = BenchmarkSetup.loadProject(mastodonProjectPath, contextProviderService.getContext());
		BenchmarkSetup.executeBenchmark(p, instructions);
	}

	public BenchmarkInstructions getInstructions() {
		return instructions;
	}

	public String getProjectPath() {
		return mastodonProjectPath;
	}
}
