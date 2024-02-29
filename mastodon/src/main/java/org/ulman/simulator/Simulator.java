package org.ulman.simulator;

import net.imglib2.RandomAccessibleInterval;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.Spot;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Simulator {
	/** If the _B,_W,_BW indicators should be prepended or appended to the spot label */
	public static boolean PREPEND_HINT_LABELS = true;

	/** How far around shall an agent look for "nearby" agents to consider them for overlaps. */
	public static double AGENT_SEARCH_RADIUS = 5.0;
	/** How close two agents can come before they are considered overlapping. */
	public static double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = 3.0;
	/** How far an agent can move between time points. */
	public static double AGENT_USUAL_STEP_SIZE = 1.0;
	/** How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position. */
	public static int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = 6;

	/** The mean life span of an agent (cell). Shorted means dividions occurs more often. */
	public static int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 7;
	/** Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation,
	 *  whenever it's life exceeded this value. */
	public static int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = 30;
	/** The maximum number of neighbors (within the {@link Simulator#AGENT_SEARCH_RADIUS} distance)
	 *  tolerated for a division to occur; if more neighbors are around, the system believes the space
	 *  is too condensed and doesn't permint agents (cells) to divide. */
	public static int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = 4;
	/** Given the last move of a mother cell, project it onto an xy-plane, one can then imagine a perpendicular
	 *  line in the xy-plane. A division line in the xy-plane is randomly picked such that it does not coincide
	 *  by larger angle with that perpendicular line, and this random line would be a "division" orientation
	 *  for the x,y coords, the z-coord is randomized. */
	public static double AGENT_MAX_VARIABLITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = 3.14;


	private int assignedIds = 0;
	private int time = 0;
	private final Map<Integer, Agent> agentsContainer = new HashMap<>(10000);
	private final List<Agent> newAgentsContainer = new ArrayList<>(100);
	private final List<Agent> deadAgentsContainer = new ArrayList<>(100);

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
		if (this.agentsContainer.containsKey(spot.getId())) {
			System.out.println("========== SIM: ERROR with registering");
			return;
		}
		System.out.println("========== SIM: registering agent " + spot.getId());
		this.newAgentsContainer.add(spot);
	}

	synchronized
	public void deregisterAgent(Agent spot) {
		if (!this.agentsContainer.containsKey(spot.getId())) {
			System.out.println("========== SIM: ERROR with deregistering");
			return;
		}
		System.out.println("========== SIM: DEregistering agent " + spot.getId());
		this.deadAgentsContainer.add(spot);
	}

	public void commitNewAndDeadAgents() {
		for (Agent spot : this.deadAgentsContainer) {
			this.agentsContainer.remove(spot.getId());
		}

		for (Agent spot : this.newAgentsContainer) {
			this.agentsContainer.put(spot.getId(), spot);
		}
	}

	public List<double[]> getListOfOccupiedCoords(Agent fromThisSpot, final double searchDistance) {
		//do no searching if the agent actually doesn't care...
		if (searchDistance == 0) return Collections.emptyList();

		final double minX = fromThisSpot.getX() - searchDistance;
		final double minY = fromThisSpot.getY() - searchDistance;
		final double minZ = fromThisSpot.getZ() - searchDistance;

		final double maxX = fromThisSpot.getX() + searchDistance;
		final double maxY = fromThisSpot.getY() + searchDistance;
		final double maxZ = fromThisSpot.getZ() + searchDistance;

		final List<double[]> retCoords = new ArrayList<>(100);
		for (Agent spot : agentsContainer.values()) {
			if (spot.getId() == fromThisSpot.getId()) {
				continue;
			}
			if (minX < spot.getX() && spot.getX() < maxX
					&& minY < spot.getY() && spot.getY() < maxY
					&& minZ < spot.getZ() && spot.getZ() < maxZ) {
				retCoords.add(new double[]{spot.getX(), spot.getY(), spot.getZ()});
			}
		}

		return retCoords;
	}

	public void doOneTime() {
		newAgentsContainer.clear();
		deadAgentsContainer.clear();

		time += 1;
		System.out.println("========== SIM: creating time point " + time + " from " + agentsContainer.size() + " agents");
		agentsContainer.forEach( (id,spot) -> spot.progress(time) );
		agentsContainer.forEach( (id,spot) -> spot.progressFinish() );
		commitNewAndDeadAgents();
	}

	public void pushToMastodonGraph() {
		final double SPOT_RADIUS = 1.0;
		agentsContainer.forEach( (id,spot) -> {
			Spot targetSpot = projectModel.getModel().getGraph().addVertex().init(time,
					new double[] {spot.getX(),spot.getY(), spot.getZ()}, SPOT_RADIUS);
			targetSpot.setLabel(spot.getName());

			Spot sourceSpot = spot.getPreviousSpot();
			if (sourceSpot != null) {
				projectModel.getModel().getGraph().addEdge(sourceSpot, targetSpot);
			}
			spot.setPreviousSpot(targetSpot);
		});
	}

	public void populate(int numberOfCells) {
		RandomAccessibleInterval<?> pixelSource = projectModel.getSharedBdvData().getSources().get(0).getSpimSource().getSource(0, 0);
		final double dx = 0.5 * (pixelSource.min(0) + pixelSource.max(0));
		final double dy = 0.5 * (pixelSource.min(1) + pixelSource.max(1));
		final double dz = 0.5 * (pixelSource.min(2) + pixelSource.max(2));
		for (int i = 0; i < numberOfCells; i++) {
			Agent spot = new Agent(this, this.getNewId(), 0, String.valueOf(i + 1),
					dx+ i * 3, dy, dz, this.time);
			this.registerAgent(spot);
		}
		this.commitNewAndDeadAgents();
	}

	public void open() {
		lock.writeLock().lock();
	}
	public void close() {
		lock.writeLock().unlock();
		addThisMomentAsUndoPoint();
	}

	void addThisMomentAsUndoPoint() {
		projectModel.getModel().setUndoPoint();
		projectModel.getModel().getGraph().notifyGraphChanged();
	}
}
