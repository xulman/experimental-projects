package org.mastodon.benchmark;

import org.mastodon.mamut.MainWindow;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.experimental.ExperimentalPluginsFacade;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;

import javax.swing.*;
import java.awt.*;
import java.io.File;

@Plugin(type = Command.class, name = "Benchmark GUI", menuPath = "Plugins>Mastodon Benchmark")
public class BenchmarkScijavaGui implements Command {
	@Parameter(style = FileWidget.OPEN_STYLE)
	public File mastodonProjectPath;

	/**
	 * If this is set to non-null, it takes over the 'mastodonProjectPath'.
	 * That said, no file is opened and this ProjectModel is used instead.
	 * This is typically used from within a running Mastodon session,
	 * see e.g. {@link ExperimentalPluginsFacade#benchmark()}.
	 */
	@Parameter(persist = false, required = false)
	ProjectModel projectModel = null;

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
	public long millisToWaitAfterInitialization = 5000;

	@Parameter
	public String benchmarkExecutionSequence = "";
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

		if (projectModel != null) {
			//this is an indication that this GUI is called from inside the Mastodon, which
			//is instructed to provide project, so we do the benchmark on this project
			BenchmarkSetup.executeBenchmark(projectModel, instructions);
		} else {
			//for not-inside-Mastodon world:
			final ProjectModel p = BenchmarkSetup.loadProject(mastodonProjectPath.getAbsolutePath().toString(), contextProviderService.getContext());
			//show the main Mastodon window first before the benchmark starts itself
			final MainWindow mainWindow = new MainWindow(p);
			mainWindow.setVisible(true);
			mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			BenchmarkSetup.executeBenchmark(p, instructions);
		}
	}

	public BenchmarkInstructions getInstructions() {
		return instructions;
	}

	public File getProjectPath() {
		return mastodonProjectPath;
	}
}
