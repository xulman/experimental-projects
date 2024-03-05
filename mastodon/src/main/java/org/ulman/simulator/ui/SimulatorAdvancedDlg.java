package org.ulman.simulator.ui;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.ulman.simulator.Simulator;

@Plugin(type = Command.class)
public class SimulatorAdvancedDlg implements Command {
	@Parameter(description = "If the _B,_W,_BW indicators should be prepended or appended to the spot label.")
	boolean PREPEND_HINT_LABELS = true;

	@Parameter(description = "Collect internal status info per every Agent. If not, may speed up the simulation as no extra data will be stored.")
	boolean COLLECT_INTERNAL_DATA = false;

	@Parameter(description = "Prints a lot of data to understand decisions making of the agents.")
	boolean VERBOSE_AGENT_DEBUG = false;

	@Parameter(description = "Prints relative little reports about what the simulation framework was asked to do.")
	boolean VERBOSE_SIMULATOR_DEBUG = false;

	@Parameter(description = "How far around shall an agent look for \"nearby\" agents to consider them for overlaps.")
	double AGENT_SEARCH_RADIUS = 4.2;

	@Parameter(description = "How close two agents can come before they are considered overlapping.")
	double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = 3.2;

	@Parameter(description = "How far an agent can move between time points.")
	double AGENT_USUAL_STEP_SIZE = 1.0;

	@Parameter(description = "How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position.")
	int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = 6;

	@Parameter(description = "The mean life span of an agent (cell). Shorted means divisions occurs more often.")
	int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 10;

	@Parameter(description = "Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation, whenever it's life exceeded this value.")
	int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = 30;

	@Parameter(description = "The maximum number of neighbors tolerated for a division to occur; if more neighbors are around, the system believes the space is too condensed and doesn't permit agents (cells) to divide.")
	int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = 2;

	@Parameter(description = "Given the last move of a mother cell, project it onto an xy-plane, one can then imagine a perpendicular line in the xy-plane. A division line in the xy-plane is randomly picked such that it does not coincide by larger angle with that perpendicular line, and this random line would be a \"division\" orientation for the x,y coords, the z-coord is randomized.")
	double AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = 2.35;

	@Parameter(description = "Freshly \"born\" daughters are placed exactly this distance apart from one another.")
	double AGENT_DAUGHTERS_INITIAL_DISTANCE = 1.6;

	@Parameter(description = "Using this radius the new spots are introduced into Mastodon.")
	double MASTODON_SPOT_RADIUS = 1.5;

	@Parameter(description = "Produce a \"lineage\" that stays in the geometric centre of the generated data.")
	boolean MASTODON_CENTER_SPOT = false;

	@Parameter
	SimulatorMainDlg basicDialog = null;

	@Override
	public void run() {
		Simulator.PREPEND_HINT_LABELS = PREPEND_HINT_LABELS;
		Simulator.COLLECT_INTERNAL_DATA = COLLECT_INTERNAL_DATA;
		Simulator.VERBOSE_AGENT_DEBUG = VERBOSE_AGENT_DEBUG;
		Simulator.VERBOSE_SIMULATOR_DEBUG = VERBOSE_SIMULATOR_DEBUG;
		Simulator.AGENT_SEARCH_RADIUS = AGENT_SEARCH_RADIUS;
		Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
		Simulator.AGENT_USUAL_STEP_SIZE = AGENT_USUAL_STEP_SIZE;
		Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;
		Simulator.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
		Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER = AGENT_MAX_LIFESPAN_AND_DIES_AFTER;
		Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;
		Simulator.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE;
		Simulator.AGENT_DAUGHTERS_INITIAL_DISTANCE = AGENT_DAUGHTERS_INITIAL_DISTANCE;
		Simulator.MASTODON_SPOT_RADIUS = MASTODON_SPOT_RADIUS;
		Simulator.MASTODON_CENTER_SPOT = MASTODON_CENTER_SPOT;
		if (basicDialog != null) basicDialog.runInsideMastodon();
	}
}
