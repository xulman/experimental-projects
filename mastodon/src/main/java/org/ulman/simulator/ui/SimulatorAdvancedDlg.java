package org.ulman.simulator.ui;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.ulman.simulator.Simulator;
import org.ulman.simulator.AgentNamingPolicy;

@Plugin(type = Command.class)
public class SimulatorAdvancedDlg implements Command {
	@Parameter(description = "Spots labels can be either 'M' or can be encoding the lineage history, also optionally with debug hints _B,_W,_BW.",
		choices = { "Lineage encoding labels (1aabba...)",
		            "Prepend hints B_,W_,BW_ to encoding labels",
		            "Append hints _B,_W,_BW to encoding labels",
		            "Always 'M' (no lineage encoding)" })
	String LABELS_NAMING_POLICY = "Lineage";

	@Parameter(description = "Collect internal status info per every Agent. If not, may speed up the simulation as no extra data will be stored.")
	boolean COLLECT_INTERNAL_DATA = Simulator.COLLECT_INTERNAL_DATA;

	@Parameter(description = "Prints a lot of data to understand decisions making of the agents.")
	boolean VERBOSE_AGENT_DEBUG = Simulator.VERBOSE_AGENT_DEBUG;

	@Parameter(description = "Prints relative little reports about what the simulation framework was asked to do.")
	boolean VERBOSE_SIMULATOR_DEBUG = Simulator.VERBOSE_SIMULATOR_DEBUG;

	@Parameter(description = "How far around shall an agent look for \"nearby\" agents to consider their positions for its own development.")
	double AGENT_LOOK_AROUND_DISTANCE = Simulator.AGENT_LOOK_AROUND_DISTANCE;

	@Parameter(description = "How close two agents can come before they start repelling each other.")
	double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;

	@Parameter(description = "How far an agent can move between time points.")
	double AGENT_USUAL_STEP_SIZE = Simulator.AGENT_USUAL_STEP_SIZE;

	@Parameter(description = "How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position.")
	int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;

	@Parameter(description = "The mean life span of an agent (cell). Shorted means divisions occurs more often.")
	int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = Simulator.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;

	@Parameter(description = "Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation, whenever it's life exceeded this value.")
	int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER;

	@Parameter(description = "The maximum number of neighbors tolerated for a division to occur; if more neighbors are around, the system believes the space is too condensed and doesn't permit agents (cells) to divide.")
	int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;

	@Parameter(description = "Given the last move of a mother cell, project it onto an xy-plane, one can then imagine a perpendicular line in the xy-plane. A division line in the xy-plane is randomly picked such that it does not coincide by larger angle with that perpendicular line, and this random line would be a \"division\" orientation for the x,y coords, the z-coord is randomized.")
	double AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = Simulator.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE;

	@Parameter(description = "Freshly \"born\" daughters are placed exactly this distance apart from one another.")
	double AGENT_DAUGHTERS_INITIAL_DISTANCE = Simulator.AGENT_DAUGHTERS_INITIAL_DISTANCE;

	@Parameter(description = "After the two daughters are born, they translate away from each other from their INITIAL_DISTANCE to MIN_DISTANCE_TO_ANOTHER_AGENT for this number of time points, during this the daughters are not influenced by any surrounding agents (even when they are in overlap), but the surrounding agents are influenced by these daughters (so the influence is asymmetrical).")
	int AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS = Simulator.AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS;

	@Parameter(description = "Using this radius the new spots are introduced into the simulation.")
	double AGENT_INITIAL_RADIUS = Simulator.AGENT_INITIAL_RADIUS;

	@Parameter(description = "Produce a \"lineage\" that stays in the geometric centre of the generated data.")
	boolean MASTODON_CENTER_SPOT = Simulator.MASTODON_CENTER_SPOT;

	@Parameter
	SimulatorMainDlg basicDialog = null;

	@Override
	public void run() {
		if (this.LABELS_NAMING_POLICY.startsWith("Always")) {
			Simulator.LABELS_NAMING_POLICY = AgentNamingPolicy.USE_ALWAYS_M;
		} else if (this.LABELS_NAMING_POLICY.startsWith("Prepend")) {
			Simulator.LABELS_NAMING_POLICY = AgentNamingPolicy.ENCODING_LABELS_AND_PREPENDING;
		} else if (this.LABELS_NAMING_POLICY.startsWith("Append")) {
			Simulator.LABELS_NAMING_POLICY = AgentNamingPolicy.ENCODING_LABELS_AND_APPENDING;
		} else {
			Simulator.LABELS_NAMING_POLICY = AgentNamingPolicy.ENCODING_LABELS;
		}
		Simulator.COLLECT_INTERNAL_DATA = COLLECT_INTERNAL_DATA;
		Simulator.VERBOSE_AGENT_DEBUG = VERBOSE_AGENT_DEBUG;
		Simulator.VERBOSE_SIMULATOR_DEBUG = VERBOSE_SIMULATOR_DEBUG;
		Simulator.AGENT_LOOK_AROUND_DISTANCE = AGENT_LOOK_AROUND_DISTANCE;
		Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
		Simulator.AGENT_USUAL_STEP_SIZE = AGENT_USUAL_STEP_SIZE;
		Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;
		Simulator.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
		Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER = AGENT_MAX_LIFESPAN_AND_DIES_AFTER;
		Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;
		Simulator.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE;
		Simulator.AGENT_DAUGHTERS_INITIAL_DISTANCE = AGENT_DAUGHTERS_INITIAL_DISTANCE;
		Simulator.AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS = AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS;
		Simulator.AGENT_INITIAL_RADIUS = AGENT_INITIAL_RADIUS;
		Simulator.MASTODON_CENTER_SPOT = MASTODON_CENTER_SPOT;
		if (basicDialog != null) basicDialog.runInsideMastodon();
	}
}
