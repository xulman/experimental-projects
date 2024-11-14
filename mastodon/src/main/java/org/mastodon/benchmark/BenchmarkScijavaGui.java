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
	@Parameter(label = "Mastodon project .mastodon file:", style = FileWidget.OPEN_STYLE,
	           description = "The benchmark commands are executed over this project whose content thus influences the command execution times.")
	public File mastodonProjectPath;

	@Parameter(label = "BigDataViewer display settings .xml file:", style = FileWidget.OPEN_STYLE)
	public File bdvSettingsXmlFilePath;

	/**
	 * If this is set to non-null, it takes over the 'mastodonProjectPath'.
	 * That said, no file is opened and this ProjectModel is used instead.
	 * This is typically used from within a running Mastodon session,
	 * see e.g. {@link ExperimentalPluginsFacade#benchmark()}.
	 */
	@Parameter(persist = false, required = false)
	ProjectModel projectModel = null;

	@Parameter(label = "Close Mastodon windows before this benchmark:")
	public boolean shouldCloseAllWindowsBeforeBenchmark = true;

	@Parameter(label = "Number of BigDataViewer windows in the benchmark:")
	public int howManyBDVsToOpen = 0;
	@Parameter(label = "Width of the windows (pixels):")
	public int windowWidthOfBDVs = 512;
	@Parameter(label = "Height of the windows (pixels):")
	public int windowHeightOfBDVs = 512;

	@Parameter(label = "Number of TrackScheme windows in the benchmark:")
	public int howManyTSsToOpen = 0;
	@Parameter(label = "Width of the windows (pixels):")
	public int windowWidthOfTSs = 512;
	@Parameter(label = "Height of the windows (pixels):")
	public int windowHeighthOfTSs = 512;

	@Parameter(label = "Bind all windows in a Lock group:")
	public boolean shouldLockButtonsLinkOpenedWindows = false;

	@Parameter(label = "Benchmark commands to setup the stage:",
	           description = "This can be left empty if only one round is conducted, otherwise make sure all opened windows will be commanded/reset.")
	public String benchmarkInitializationSequence = "";
	@Parameter(label = "Pause after each command (milliseconds):")
	public long millisToWaitAfterInitialization = 5000;

	@Parameter(label = "Benchmark commands to be measured:", description = "This is the actual benchmarked sequence.")
	public String benchmarkExecutionSequence = "";
	@Parameter(label = "Pause after each command (milliseconds):")
	public long millisToWaitAfterEachBenchmarkAction = 3000;

	@Parameter(label = "Benchmark runs:")
	public int repetitions = 1;

	@Parameter(label = "CSV results filename extra infix:", description = "Any title to distinguish this particular experiment.")
	public String csvInfix = "";
	@Parameter(label = "CSV results report mode:",
	           choices = {"Rows grouped per measured source", "Rows grouped per measured source and per benchmark run"})
	public String csvMode = "Rows grouped per measured source";

	@Parameter
	private CommandService contextProviderService;

	public BenchmarkInstructions instructions = new BenchmarkInstructions();

	@Override
	public void run() {
		instructions.bdvSettingsXmlFilename = bdvSettingsXmlFilePath.getAbsolutePath().toString();
		instructions.suggestTsBookmarksFilename();
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
		instructions.benchmarkRounds = repetitions;
		instructions.suggestCsvResultsFilename(csvInfix);
		instructions.measurementsReportsAlsoPerRound = csvMode.contains("run");

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
