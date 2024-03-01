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
	public static double AGENT_MAX_VARIABLITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = 3.14;

	/** Using this radius the new spots are introduced into Mastodon. */
	public static double MASTODON_SPOT_RADIUS = 2.0;
	/** Produce a \"lineage\" that stays in the geometric centre of the generated data. */
	public static boolean MASTODON_CENTER_SPOT = false;


	private int assignedIds = 0;
	private int time = 0;
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

		final Spot thisSpot = fromThisSpot.getPreviousSpot();
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
		System.out.println("========== SIM: creating time point " + time + " from " + agentsContainer.size() + " agents");
		agentsContainer.parallelStream().forEach(s -> s.progress(time));
		agentsContainer.parallelStream().forEach(Agent::progressFinish);
		commitNewAndDeadAgents();
	}

	final double[] coords = new double[3];
	double sum_x,sum_y,sum_z;
	Spot prevCentreSpot = null;
	public void pushToMastodonGraph() {
		sum_x = 0;
		sum_y = 0;
		sum_z = 0;

		agentsContainer.forEach( spot -> {
			coords[0] = spot.getX();
			coords[1] = spot.getY();
			coords[2] = spot.getZ();
			sum_x += coords[0];
			sum_y += coords[1];
			sum_z += coords[2];
			Spot targetSpot = projectModel.getModel().getGraph().addVertex()
					.init(time, coords, MASTODON_SPOT_RADIUS);
			targetSpot.setLabel(spot.getName());

			Spot sourceSpot = spot.getPreviousSpot();
			if (sourceSpot != null) {
				projectModel.getModel().getGraph().addEdge(sourceSpot, targetSpot).init();
			}
			spot.setPreviousSpot(targetSpot);
		});

		if (Simulator.MASTODON_CENTER_SPOT) {
			coords[0] = sum_x / agentsContainer.size();
			coords[1] = sum_y / agentsContainer.size();
			coords[2] = sum_z / agentsContainer.size();
			Spot targetSpot = projectModel.getModel().getGraph().addVertex()
					.init(time, coords, MASTODON_SPOT_RADIUS);
			targetSpot.setLabel("centre");

			if (prevCentreSpot == null) {
				prevCentreSpot = projectModel.getModel().getGraph().vertexRef();
			} else {
				projectModel.getModel().getGraph().addEdge(prevCentreSpot, targetSpot).init();
			}
			prevCentreSpot.refTo(targetSpot);
		}
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
			Agent agent = new Agent(this, this.getNewId(), 0, s.getLabel()+"-",
					s.getDoublePosition(0), s.getDoublePosition(1), s.getDoublePosition(2), this.time);
			agent.setPreviousSpot( projectModel.getModel().getGraph().vertexRef().refTo(s) );
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
	}
	public void close() {
		lock.writeLock().unlock();
		new ModelGraphListeners().resumeListeners();
		projectModel.getModel().setUndoPoint();
		projectModel.getModel().getGraph().notifyGraphChanged();
	}
}
