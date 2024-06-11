package org.ulman.simulator;

public class SimulationConfig {
	public AgentNamingPolicy LABELS_NAMING_POLICY = Simulator.LABELS_NAMING_POLICY;
	public boolean COLLECT_INTERNAL_DATA = Simulator.COLLECT_INTERNAL_DATA;
	public boolean VERBOSE_AGENT_DEBUG = Simulator.VERBOSE_AGENT_DEBUG;
	public boolean VERBOSE_SIMULATOR_DEBUG = Simulator.VERBOSE_SIMULATOR_DEBUG;
	public double AGENT_LOOK_AROUND_DISTANCE = Simulator.AGENT_LOOK_AROUND_DISTANCE;
	public double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
	public double AGENT_USUAL_STEP_SIZE = Simulator.AGENT_USUAL_STEP_SIZE;
	public int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;
	public boolean AGENT_DO_2D_MOVES_ONLY = Simulator.AGENT_DO_2D_MOVES_ONLY;
	public int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = Simulator.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
	public int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER;
	public int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;
	public double AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = Simulator.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE;
	public double AGENT_DAUGHTERS_INITIAL_DISTANCE = Simulator.AGENT_DAUGHTERS_INITIAL_DISTANCE;
	public int AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS = Simulator.AGENT_MAX_TIME_DAUGHTERS_IGNORE_ANOTHER_AGENTS;
	public double AGENT_INITIAL_RADIUS = Simulator.AGENT_INITIAL_RADIUS;
	public boolean CREATE_MASTODON_CENTER_SPOT = Simulator.CREATE_MASTODON_CENTER_SPOT;
	public int MASTODON_KEEPS_EVERY_ROUND = Simulator.MASTODON_KEEPS_EVERY_ROUND;
}
