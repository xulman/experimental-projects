package org.ulman.simulator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Util;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.spatial.SpatialIndex;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Simulator {
	/** If the _B,_W,_BW indicators should be prepended or appended to the spot label. */
	public static boolean PREPEND_HINT_LABELS = true;
	/** Collect internal status info per every Agent. If not, may speed up the simulation as no extra data will be stored. */
	public static boolean COLLECT_INTERNAL_DATA = false;
	/** Prints a lot of data to understand decisions making of the agents. */
	public static boolean VERBOSE_AGENT_DEBUG = false;
	/** Prints relative little reports about what the simulation framework was asked to do. */
	public static boolean VERBOSE_SIMULATOR_DEBUG = false;

	/** How far around shall an agent look for "nearby" agents to consider them for overlaps. */
	public static double AGENT_SEARCH_RADIUS = 5.0;
	/** How close two agents can come before they are considered overlapping. */
	public static double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = 3.0;
	/** How far an agent can move between time points. */
	public static double AGENT_USUAL_STEP_SIZE = 1.0;
	/** How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position. */
	public static int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = 6;
	/** Prohibit any changes in the z-coordinate of agents (cells). */
	public static boolean AGENT_DO_2D_MOVES_ONLY = false;

	/** The mean life span of an agent (cell). Shorted means divisions occurs more often. */
	public static int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 7;
	/** Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation,
	 *  whenever it's life exceeded this value. */
	public static int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = 30;
	/** The maximum number of neighbors (within the {@link Simulator#AGENT_SEARCH_RADIUS} distance)
	 *  tolerated for a division to occur; if more neighbors are around, the system believes the space
	 *  is too condensed and doesn't permit agents (cells) to divide. */
	public static int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = 4;
	/** Given the last move of a mother cell, project it onto an xy-plane, one can then imagine a perpendicular
	 *  line in the xy-plane. A division line in the xy-plane is randomly picked such that it does not coincide
	 *  by larger angle with that perpendicular line, and this random line would be a "division" orientation
	 *  for the x,y coords, the z-coord is randomized. */
	public static double AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = 3.14;

	/** Using this radius the new spots are introduced into Mastodon. */
	public static double MASTODON_SPOT_RADIUS = 2.0;
	/** Produce a \"lineage\" that stays in the geometric centre of the generated data. */
	public static boolean MASTODON_CENTER_SPOT = false;

	public final static String MASTODON_CENTER_SPOT_NAME = "centre";

	public void setParamsFromConfig(final SimulationConfig c) {
		PREPEND_HINT_LABELS = c.PREPEND_HINT_LABELS;
		COLLECT_INTERNAL_DATA = c.COLLECT_INTERNAL_DATA;
		VERBOSE_AGENT_DEBUG = c.VERBOSE_AGENT_DEBUG;
		VERBOSE_SIMULATOR_DEBUG = c.VERBOSE_SIMULATOR_DEBUG;
		AGENT_SEARCH_RADIUS = c.AGENT_SEARCH_RADIUS;
		AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = c.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
		AGENT_USUAL_STEP_SIZE = c.AGENT_USUAL_STEP_SIZE;
		AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = c.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;
		AGENT_DO_2D_MOVES_ONLY = c.AGENT_DO_2D_MOVES_ONLY;
		AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = c.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
		AGENT_MAX_LIFESPAN_AND_DIES_AFTER = c.AGENT_MAX_LIFESPAN_AND_DIES_AFTER;
		AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = c.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;
		AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = c.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE;
		MASTODON_SPOT_RADIUS = c.MASTODON_SPOT_RADIUS;
		MASTODON_CENTER_SPOT = c.MASTODON_CENTER_SPOT;
	}

	@Override
	public String toString() {
		String sb = "Simulation params:\n" + "  PREPEND_HINT_LABELS: " + PREPEND_HINT_LABELS +
				"\n  AGENT_SEARCH_RADIUS: " + AGENT_SEARCH_RADIUS +
				"\n  AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT: " + AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT +
				"\n  AGENT_USUAL_STEP_SIZE: " + AGENT_USUAL_STEP_SIZE +
				"\n  AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE: " + AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE +
				"\n  AGENT_DO_2D_MOVES_ONLY: " + AGENT_DO_2D_MOVES_ONLY +
				"\n  AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION: " + AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION +
				"\n  AGENT_MAX_LIFESPAN_AND_DIES_AFTER: " + AGENT_MAX_LIFESPAN_AND_DIES_AFTER +
				"\n  AGENT_MAX_DENSITY_TO_ENABLE_DIVISION: " + AGENT_MAX_DENSITY_TO_ENABLE_DIVISION +
				"\n  AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE: " + AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE +
				"\n  MASTODON_SPOT_RADIUS: " + MASTODON_SPOT_RADIUS +
				"\n  MASTODON_CENTER_SPOT: " + MASTODON_CENTER_SPOT;
		return sb;
	}


	private int assignedIds = 0;
	private int time = 0;
	private long spotsInTotal = 0;
	private final List<Agent> agentsContainer = new ArrayList<>(500000);
	private final List<Agent> newAgentsContainer = new ArrayList<>(100000);
	private final List<Agent> deadAgentsContainer = new ArrayList<>(100000);

	private final ProjectModel projectModel;
	private final ReentrantReadWriteLock lock;

	public Simulator(final ProjectModel projectModel) {
		this.projectModel = projectModel;
		this.lock = projectModel.getModel().getGraph().getLock();
	}

	synchronized
	public int getNewId() {
		this.assignedIds += 1;
		return this.assignedIds;
	}

	synchronized
	public void registerAgent(Agent spot) {
		if (VERBOSE_SIMULATOR_DEBUG) {
			System.out.println("========== SIM: registering agent " + spot.getId());
		}
		this.newAgentsContainer.add(spot);
	}

	synchronized
	public void deregisterAgent(Agent spot) {
		if (VERBOSE_SIMULATOR_DEBUG) {
			System.out.println("========== SIM: DEregistering agent " + spot.getId());
		}
		this.deadAgentsContainer.add(spot);
	}

	public void commitNewAndDeadAgents() {
		final int expectedSize = agentsContainer.size() - deadAgentsContainer.size() + newAgentsContainer.size();
		agentsContainer.removeAll(deadAgentsContainer);
		agentsContainer.addAll(newAgentsContainer);
		if (agentsContainer.size() != expectedSize) {
			System.out.println("========== SIM: ERROR with updating the main lists of agents");
		}
	}


	/** returns how many coordinates it has put into the array 'nearbyCoordinates', and
	 *  returns the actual last offset (so divide by three to learn how many neighbors are there */
	int getListOfOccupiedCoords(Agent fromThisSpot, final double searchDistance, final double[] nearbyCoordinates) {
		//do no searching if the agent actually doesn't care...
		if (searchDistance == 0) return 0;

		//NB: should exist since this method is always called after this.pushToMastodonGraph()
		final Spot thisSpot = fromThisSpot.getMostRecentMastodonSpotRepre();
		final SpatialIndex< Spot > spatialIndex
				= projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex( thisSpot.getTimepoint() );
		final IncrementalNearestNeighborSearch< Spot > search = spatialIndex.getIncrementalNearestNeighborSearch();

		search.search( thisSpot );
		int off = 0;
		while ( search.hasNext() && off < nearbyCoordinates.length )
		{
			Spot neighbor = search.next();
			if (neighbor.equals(thisSpot)) continue;
			if (Util.distance(thisSpot,neighbor) > searchDistance) break;

			nearbyCoordinates[off++] = neighbor.getDoublePosition(0);
			nearbyCoordinates[off++] = neighbor.getDoublePosition(1);
			nearbyCoordinates[off++] = neighbor.getDoublePosition(2);
		}
		return off;
	}

	public void doOneTime() {
		newAgentsContainer.clear();
		deadAgentsContainer.clear();

		time += 1;
		System.out.println("========== SIM: creating time point " + time
				+ " from " + agentsContainer.size() + " agents ("
				+ spotsInTotal + " in total)");
		agentsContainer.parallelStream().forEach(s -> s.progress(time));
		agentsContainer.parallelStream().forEach(Agent::progressFinish);
		commitNewAndDeadAgents();
	}

	final double[] coords = new double[3];
	final double[] sum_x = new double[2000];
	final double[] sum_y = new double[2000];
	final double[] sum_z = new double[2000];
	Spot auxSpot = null;

	public void pushToMastodonGraph() {
		sum_x[time] = 0;
		sum_y[time] = 0;
		sum_z[time] = 0;

		agentsContainer.forEach( agent -> {
			coords[0] = agent.getX();
			coords[1] = agent.getY();
			coords[2] = agent.getZ();
			sum_x[time] += coords[0];
			sum_y[time] += coords[1];
			sum_z[time] += coords[2];
			projectModel.getModel().getGraph().addVertex(auxSpot)
					.init(time, coords, MASTODON_SPOT_RADIUS);
			auxSpot.setLabel(agent.getName());

			if (agent.isMostRecentMastodonSpotValid()) {
				projectModel.getModel().getGraph().addEdge(agent.getMostRecentMastodonSpotRepre(), auxSpot).init();
			}
			agent.setMostRecentMastodonSpotRepre(auxSpot);
		});
		spotsInTotal += agentsContainer.size();

		sum_x[time] /= agentsContainer.size();
		sum_y[time] /= agentsContainer.size();
		sum_z[time] /= agentsContainer.size();
	}

	public void pushCenterSpotsToMastodonGraph(int timeFrom, int timeTill) {
		final Spot prevCentreSpot = projectModel.getModel().getGraph().vertexRef();

		for (int time = timeFrom; time <= timeTill; ++time) {
			coords[0] = sum_x[time];
			coords[1] = sum_y[time];
			coords[2] = sum_z[time];
			projectModel.getModel().getGraph().addVertex(auxSpot)
					.init(time, coords, MASTODON_SPOT_RADIUS);
			auxSpot.setLabel(MASTODON_CENTER_SPOT_NAME);
			if (time > timeFrom) {
				projectModel.getModel().getGraph().addEdge(prevCentreSpot, auxSpot).init();
			}
			prevCentreSpot.refTo(auxSpot);
		}

		projectModel.getModel().getGraph().releaseRef(prevCentreSpot);
	}

	public void populate(int numberOfCells, final int timePoint) {
		this.time = timePoint;
		RandomAccessibleInterval<?> pixelSource = projectModel.getSharedBdvData().getSources().get(0).getSpimSource().getSource(0, 0);
		final double dx = 0.5 * (pixelSource.min(0) + pixelSource.max(0));
		final double dy = 0.5 * (pixelSource.min(1) + pixelSource.max(1));
		final double dz = 0.5 * (pixelSource.min(2) + pixelSource.max(2));
		for (int i = 0; i < numberOfCells; i++) {
			Agent agent = new Agent(this, this.getNewId(), 0, String.valueOf(i + 1),
					dx+ i * 3, dy, dz, this.time);
			this.registerAgent(agent);
		}
		this.commitNewAndDeadAgents();
	}

	public void populate(final ProjectModel projectModel, final int timePoint) {
		this.time = timePoint;
		for (Spot s : projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(timePoint-1)) {
			if (s.getLabel().equals(Simulator.MASTODON_CENTER_SPOT_NAME)) continue;
			Agent agent = new Agent(this, this.getNewId(), 0, s.getLabel()+"-",
					s.getDoublePosition(0), s.getDoublePosition(1), s.getDoublePosition(2), this.time);
			agent.setMostRecentMastodonSpotRepre(s);
			this.registerAgent(agent);
		}
		this.commitNewAndDeadAgents();
	}

	class ModelGraphListeners extends ModelGraph {
		public void pauseListeners() {
			super.pauseListeners();
		}
		public void resumeListeners() {
			super.resumeListeners();
		}
	}

	public void open() {
		new ModelGraphListeners().pauseListeners();
		lock.writeLock().lock();
		auxSpot = projectModel.getModel().getGraph().vertexRef();
	}
	public void close() {
		if (auxSpot != null) projectModel.getModel().getGraph().releaseRef(auxSpot);
		lock.writeLock().unlock();
		new ModelGraphListeners().resumeListeners();
		projectModel.getModel().setUndoPoint();
		projectModel.getModel().getGraph().notifyGraphChanged();
	}
}
