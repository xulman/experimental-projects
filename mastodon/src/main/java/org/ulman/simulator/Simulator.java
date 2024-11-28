package org.ulman.simulator;

import net.imglib2.RandomAccessibleInterval;
import net.imglib2.util.Util;
import org.mastodon.kdtree.IncrementalNearestNeighborSearch;
import org.mastodon.mamut.ProjectModel;
import org.mastodon.mamut.model.ModelGraph;
import org.mastodon.mamut.model.Spot;
import org.mastodon.mamut.model.Link;
import org.mastodon.spatial.SpatialIndex;
import org.mastodon.model.SelectionModel;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class Simulator {
	/** Spots labels can be either 'M' or can be encoding the lineage history, also optionally with debug hints _B,_W,_BW. */
	public static AgentNamingPolicy LABELS_NAMING_POLICY = AgentNamingPolicy.ENCODING_LABELS;
	/** Collect internal status info per every Agent. If not, may speed up the simulation as no extra data will be stored. */
	public static boolean COLLECT_INTERNAL_DATA = false;
	/** Prints a lot of data to understand decisions making of the agents. */
	public static boolean VERBOSE_AGENT_DEBUG = false;
	/** Prints relative little reports about what the simulation framework was asked to do. */
	public static boolean VERBOSE_SIMULATOR_DEBUG = false;

	/** How far around shall an agent look for "nearby" agents to consider their positions for its own development. */
	public static double AGENT_LOOK_AROUND_DISTANCE = 4.2;
	/** How close two agents can come before they start repelling each other. */
	public static double AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = 2.6;
	/** How far an agent can move between time points. */
	public static double AGENT_USUAL_STEP_SIZE = 1.0;
	/** How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position. */
	public static int AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = 4;
	/** Prohibit any changes in the z-coordinate of agents (cells). */
	public static Agent2dMovesRestriction AGENT_DO_2D_MOVES_ONLY = Agent2dMovesRestriction.NO_RESTRICTION;

	/** The mean life span of an agent (cell). Shorted means divisions occurs more often. */
	public static int AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 10;
	/** Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation,
	 *  whenever it's life exceeded this value. */
	public static int AGENT_MAX_LIFESPAN_AND_DIES_AFTER = 30;
	/** The maximum number of neighbors (within the {@link Simulator#AGENT_LOOK_AROUND_DISTANCE} distance)
	 *  tolerated for a division to occur; if more neighbors are around, the system believes the space
	 *  is too condensed and doesn't permit agents (cells) to divide. */

	public static int AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = 2;
	/** Given the last division direction (dozering direction) of a mother cell, daughters will divide in a new,
	 *  random division direction such that the angle between the two division directions is not more than this. */
	public static double AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES = 1.40; //+- 80deg
	/** Freshly "born" daughters are placed exactly this distance apart from one another. */
	public static double AGENT_DAUGHTERS_INITIAL_DISTANCE = 0.4;
	/** After the two daughters are born, they translate away from each other from their INITIAL_DISTANCE
	 *  to AGENT_DAUGHTERS_DOZERING_DISTANCE for this number of time points. */
	public static double AGENT_DAUGHTERS_DOZERING_DISTANCE = 3.1;
	/** After the two daughters are born, they translate away from each other from their INITIAL_DISTANCE
	 *  to AGENT_DAUGHTERS_DOZERING_DISTANCE for this number of time points, during this the daughters are
	 *  influenced only by surrounding-and-overlapping agents, but the surrounding agents
	 *  are influenced by these daughters normally (so the influence is asymmetrical). */
	public static int AGENT_DAUGHTERS_DOZERING_TIME_PERIOD = 2;

	/** Using this radius the new spots are introduced into the simulation. */
	public static double AGENT_INITIAL_RADIUS = 1.5;
	/** Produce a \"lineage\" that stays in the geometric centre of the generated data. */
	public static boolean CREATE_MASTODON_CENTER_SPOT = false;

	public final static String MASTODON_CENTER_SPOT_NAME = "centre";

	public static void setParamsFromConfig(final SimulationConfig c) {
		LABELS_NAMING_POLICY = c.LABELS_NAMING_POLICY;
		COLLECT_INTERNAL_DATA = c.COLLECT_INTERNAL_DATA;
		VERBOSE_AGENT_DEBUG = c.VERBOSE_AGENT_DEBUG;
		VERBOSE_SIMULATOR_DEBUG = c.VERBOSE_SIMULATOR_DEBUG;
		AGENT_LOOK_AROUND_DISTANCE = c.AGENT_LOOK_AROUND_DISTANCE;
		AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = c.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
		AGENT_USUAL_STEP_SIZE = c.AGENT_USUAL_STEP_SIZE;
		AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = c.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE;
		AGENT_DO_2D_MOVES_ONLY = c.AGENT_DO_2D_MOVES_ONLY;
		AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = c.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
		AGENT_MAX_LIFESPAN_AND_DIES_AFTER = c.AGENT_MAX_LIFESPAN_AND_DIES_AFTER;
		AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = c.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;
		AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES = c.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES;
		AGENT_DAUGHTERS_INITIAL_DISTANCE = c.AGENT_DAUGHTERS_INITIAL_DISTANCE;
		AGENT_DAUGHTERS_DOZERING_DISTANCE = c.AGENT_DAUGHTERS_DOZERING_DISTANCE;
		AGENT_DAUGHTERS_DOZERING_TIME_PERIOD = c.AGENT_DAUGHTERS_DOZERING_TIME_PERIOD;
		AGENT_INITIAL_RADIUS = c.AGENT_INITIAL_RADIUS;
		CREATE_MASTODON_CENTER_SPOT = c.CREATE_MASTODON_CENTER_SPOT;
	}

	@Override
	public String toString() {
		return "Simulation parameters:\n" + "  LABELS_NAMING_POLICY: " + LABELS_NAMING_POLICY +
				"\n  AGENT_LOOK_AROUND_DISTANCE: " + AGENT_LOOK_AROUND_DISTANCE +
				"\n  AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT: " + AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT +
				"\n  AGENT_USUAL_STEP_SIZE: " + AGENT_USUAL_STEP_SIZE +
				"\n  AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE: " + AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE +
				"\n  AGENT_DO_2D_MOVES_ONLY: " + AGENT_DO_2D_MOVES_ONLY +
				"\n  AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION: " + AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION +
				"\n  AGENT_MAX_LIFESPAN_AND_DIES_AFTER: " + AGENT_MAX_LIFESPAN_AND_DIES_AFTER +
				"\n  AGENT_MAX_DENSITY_TO_ENABLE_DIVISION: " + AGENT_MAX_DENSITY_TO_ENABLE_DIVISION +
				"\n  AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES: " + AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES +
				"\n  AGENT_DAUGHTERS_INITIAL_DISTANCE: " + AGENT_DAUGHTERS_INITIAL_DISTANCE +
				"\n  AGENT_DAUGHTERS_DOZERING_DISTANCE: " + AGENT_DAUGHTERS_DOZERING_DISTANCE +
				"\n  AGENT_DAUGHTERS_DOZERING_TIME_PERIOD: " + AGENT_DAUGHTERS_DOZERING_TIME_PERIOD +
				"\n  AGENT_INITIAL_RADIUS: " + AGENT_INITIAL_RADIUS +
				"\n  CREATE_MASTODON_CENTER_SPOT: " + CREATE_MASTODON_CENTER_SPOT;
	}


	private int assignedIds = 0;
	private int time = 0;
	private long spotsInTotal = 0;
	private final Map<Integer,Agent> agentsContainer = new LinkedHashMap<>(5000000);
	private final List<Agent> newAgentsContainer = new ArrayList<>(2000000);
	private final List<Agent> deadAgentsContainer = new ArrayList<>(2000000);

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

	public int getTime() {
		return time;
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
		deadAgentsContainer.forEach(a -> agentsContainer.remove(a.getId(),a));
		newAgentsContainer.forEach(a -> agentsContainer.put(a.getId(),a));
		if (agentsContainer.size() != expectedSize) {
			System.out.println("========== SIM: ERROR with updating the main lists of agents");
		}
	}


	public static final double SKIP_NEIGHBOR_SEARCH_DISTANCE = -10240.0;

	/** returns how many coordinates it has put into the array 'nearbySpheres', and
	 *  returns the actual last offset (so divide by four to learn how many neighbors are there);
	 *  the actual surface-to-surface distance is measured (the 'searchDistance' parameter) and
	 *  can be even provided negative (e.g. to detect only overlapping neighbors);
	 *  if Simulator.SKIP_NEIGHBOR_SEARCH_DISTANCE is provided for 'searchDistance',
	 *  then this function quits immediately, without doing any search */
	int getListOfOccupiedCoords(final Agent fromThisAgent, final double searchDistance, final double[] nearbySpheres) {
		//do no searching if the agent actually doesn't care...
		if (searchDistance == SKIP_NEIGHBOR_SEARCH_DISTANCE) return 0;
		final double radiusPlusSearchDistance = fromThisAgent.getR() + searchDistance;

		//NB: should exist since this method is always called after this.pushToMastodonGraphAndUpdateStats()
		final Spot thisSpot = fromThisAgent.getMostRecentMastodonSpotRepre();
		final SpatialIndex< Spot > spatialIndex
				= projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex( thisSpot.getTimepoint() );
		final IncrementalNearestNeighborSearch< Spot > search = spatialIndex.getIncrementalNearestNeighborSearch();

		search.search( thisSpot );
		int off = 0;
		while ( search.hasNext() && off < nearbySpheres.length )
		{
			final Spot neighborSpot = search.next();
			if (neighborSpot.equals(thisSpot)) continue;
			if (isHintingSphere(neighborSpot)) continue;

			final double neighborSpotR = Math.sqrt(neighborSpot.getBoundingSphereRadiusSquared());
			if ( Util.distance(thisSpot,neighborSpot) >
					(radiusPlusSearchDistance+neighborSpotR) ) break;

			nearbySpheres[off++] = neighborSpot.getDoublePosition(0);
			nearbySpheres[off++] = neighborSpot.getDoublePosition(1);
			nearbySpheres[off++] = neighborSpot.getDoublePosition(2);
			nearbySpheres[off++] = neighborSpotR;
		}
		return off;
	}

	public static final String STAY_INSIDE_SPHERES_NAME = "stay_inside";
	public static final String KEEP_OUT_SPHERES_NAME = "keep_out";
	public static final String HOLD_POSITION_SPHERES_NAME = "hold_position";

	public static boolean isHintingSphere(final Spot s) {
			final String label = s.getLabel();
			if (label.startsWith(STAY_INSIDE_SPHERES_NAME)) return true;
			if (label.startsWith(KEEP_OUT_SPHERES_NAME)) return true;
			if (label.startsWith(HOLD_POSITION_SPHERES_NAME)) return true;
			return false;
	}


	//the shared hinting spheres are relevant for this time point
	int spheresCacheCurrentTimepoint = -1;
	//
	//NB: package protected... so, directly accessible from Agent... yeah, "da shortcut"
	final double[] stayInsideSpheresSharedArray = new double[8192];
	final double[] keepOutSpheresSharedArray = new double[8192];
	final double[] holdPositionSpheresSharedArray = new double[8192];
	int stayInsideSpheresSharedArrayMaxUsedIdx = -1;
	int keepOutSpheresSharedArrayMaxUsedIdx = -1;
	int holdPositionSpheresSharedArrayMaxUsedIdx = -1;

	synchronized
	protected void updateSphereCaches(final int forThisTimepoint) {
		//already valid/up-to-date?
		if (VERBOSE_SIMULATOR_DEBUG) {
			System.out.println("========== SIM: requested hinting spheres for TP " + forThisTimepoint);
		}
		if (spheresCacheCurrentTimepoint == forThisTimepoint) return;

		//else, reset cache and start filling it below....
		stayInsideSpheresSharedArrayMaxUsedIdx = 0;
		keepOutSpheresSharedArrayMaxUsedIdx = 0;
		holdPositionSpheresSharedArrayMaxUsedIdx = 0;

		final SpatialIndex< Spot > spatialIndex
				= projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex( forThisTimepoint );

		for (Spot s : spatialIndex) {
			if (s.getLabel().startsWith(STAY_INSIDE_SPHERES_NAME)) {
				stayInsideSpheresSharedArray[stayInsideSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(0);
				stayInsideSpheresSharedArray[stayInsideSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(1);
				stayInsideSpheresSharedArray[stayInsideSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(2);
				stayInsideSpheresSharedArray[stayInsideSpheresSharedArrayMaxUsedIdx++] = Math.sqrt(s.getBoundingSphereRadiusSquared());
			} else if (s.getLabel().startsWith(KEEP_OUT_SPHERES_NAME)) {
				keepOutSpheresSharedArray[keepOutSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(0);
				keepOutSpheresSharedArray[keepOutSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(1);
				keepOutSpheresSharedArray[keepOutSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(2);
				keepOutSpheresSharedArray[keepOutSpheresSharedArrayMaxUsedIdx++] = Math.sqrt(s.getBoundingSphereRadiusSquared());
			} else if (s.getLabel().startsWith(HOLD_POSITION_SPHERES_NAME)) {
				holdPositionSpheresSharedArray[holdPositionSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(0);
				holdPositionSpheresSharedArray[holdPositionSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(1);
				holdPositionSpheresSharedArray[holdPositionSpheresSharedArrayMaxUsedIdx++] = s.getDoublePosition(2);
				holdPositionSpheresSharedArray[holdPositionSpheresSharedArrayMaxUsedIdx++] = Math.sqrt(s.getBoundingSphereRadiusSquared());
			}
		}
		spheresCacheCurrentTimepoint = forThisTimepoint;

		if (VERBOSE_SIMULATOR_DEBUG) {
			System.out.println("========== SIM: found "+(stayInsideSpheresSharedArrayMaxUsedIdx/4)
				+", "+(keepOutSpheresSharedArrayMaxUsedIdx/4)+", "+(holdPositionSpheresSharedArrayMaxUsedIdx/4)
				+" stay,keep,hold hinting spheres");
		}
	}


	public void doOneTime() {
		System.out.println("========== SIM: clearing out...");
		newAgentsContainer.clear();
		deadAgentsContainer.clear();

		time += 1;
		System.out.println("========== SIM: creating time point " + time
				+ " from " + agentsContainer.size() + " agents ("
				+ spotsInTotal + " in total, time is "
				+ java.time.LocalTime.now() + ")");
		if (VERBOSE_AGENT_DEBUG) {
			agentsContainer.values().stream().forEach(s -> s.progress(time));
		} else {
			agentsContainer.values().parallelStream().forEach(s -> s.progress(time));
		}
		System.out.println("========== SIM: going for progressFinish...");
		agentsContainer.values().parallelStream().forEach(Agent::progressFinish);
		System.out.println("========== SIM: going to commitNewAndDeadAgents...");
		commitNewAndDeadAgents();
	}

	final double[] coords = new double[3];
	final double[] sum_x = new double[5000];
	final double[] sum_y = new double[5000];
	final double[] sum_z = new double[5000];
	Spot auxSpot = null;

	public void pushToMastodonGraphAndUpdateStats() {
		System.out.println("========== SIM: publishing to Mastodon " + agentsContainer.size() + " agents");
		sum_x[time] = 0;
		sum_y[time] = 0;
		sum_z[time] = 0;

		agentsContainer.values().forEach( agent -> {
			coords[0] = agent.getX();
			coords[1] = agent.getY();
			coords[2] = agent.getZ();
			sum_x[time] += coords[0];
			sum_y[time] += coords[1];
			sum_z[time] += coords[2];
			projectModel.getModel().getGraph().addVertex(auxSpot)
					.init(time, coords, agent.getR());
			auxSpot.setLabel(agent.getName());

			if (agent.isMostRecentMastodonSpotValid()) {
				projectModel.getModel().getGraph().addEdge(agent.getMostRecentMastodonSpotRepre(), auxSpot).init();
			}
			agent.setMostRecentMastodonSpotRepre(auxSpot);
		});
		final int addingCount = agentsContainer.size();
		spotsInTotal += addingCount;

		if (addingCount > 0) {
			sum_x[time] /= addingCount;
			sum_y[time] /= addingCount;
			sum_z[time] /= addingCount;
		} else {
			//NB: indicate there's no data for this time point
			sum_x[time] = Double.NaN;
		}
	}

	public void updateStats() {
		System.out.println("========== SIM: updateStats - calculating average coord from " + agentsContainer.size() + " agents");
		sum_x[time] = 0;
		sum_y[time] = 0;
		sum_z[time] = 0;

		agentsContainer.values().forEach( agent -> {
			sum_x[time] += agent.getX();
			sum_y[time] += agent.getY();
			sum_z[time] += agent.getZ();
		});
		final int addingCount = agentsContainer.size();
		spotsInTotal += addingCount;

		if (addingCount > 0) {
			sum_x[time] /= addingCount;
			sum_y[time] /= addingCount;
			sum_z[time] /= addingCount;
		} else {
			//NB: indicate there's no data for this time point
			sum_x[time] = Double.NaN;
		}
	}

	public void pushCenterSpotsToMastodonGraph(int timeFrom, int timeTill) {
		final Spot prevCentreSpot = projectModel.getModel().getGraph().vertexRef();
		boolean isPrevCentreValid = false;

		//try to find previous centre and connect/link to it
		if (timeFrom > 0) {
			Iterator<Spot> it = projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(timeFrom - 1).iterator();
			while (!isPrevCentreValid && it.hasNext()) {
				Spot s = it.next();
				if (s.getLabel().equals(Simulator.MASTODON_CENTER_SPOT_NAME)) {
					prevCentreSpot.refTo(s);
					isPrevCentreValid = true;
				}
			}
		}

		for (int time = timeFrom; time <= timeTill; ++time) {
			if (sum_x[time] == Double.NaN) continue; //skip over empty frame

			coords[0] = sum_x[time];
			coords[1] = sum_y[time];
			coords[2] = sum_z[time];
			projectModel.getModel().getGraph().addVertex(auxSpot)
					.init(time, coords, 1.5);
			auxSpot.setLabel(MASTODON_CENTER_SPOT_NAME);
			if (isPrevCentreValid) {
				projectModel.getModel().getGraph().addEdge(prevCentreSpot, auxSpot).init();
			}
			prevCentreSpot.refTo(auxSpot);
			isPrevCentreValid = true;
		}

		projectModel.getModel().getGraph().releaseRef(prevCentreSpot);
	}

	public void populate(int numberOfCells, final int timePoint) {
		//generate a within-xy-plane stripe of quazi-regularly placed agents, at fixed z coordinate and given time point
		this.time = timePoint;
		RandomAccessibleInterval<?> pixelSource = projectModel.getSharedBdvData().getSources().get(0).getSpimSource().getSource(0, 0);
		final double dx = 0.5 * (pixelSource.min(0) + pixelSource.max(0));
		final double dy = 0.5 * (pixelSource.min(1) + pixelSource.max(1));
		final double dz = 0.5 * (pixelSource.min(2) + pixelSource.max(2));
		final int iShift = numberOfCells/2;
		final double dxStep = Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT * 1.7;
		for (int i = 0; i < numberOfCells; i++) {
			Agent agent = new Agent(this, this.getNewId(), 0, String.valueOf(i + 1),
					dx + (i-iShift) * dxStep,
					dy + 1.8 * dxStep * (new Random().nextDouble() - 0.5),
					dz, AGENT_INITIAL_RADIUS, this.time);
			this.registerAgent(agent);
		}
		this.commitNewAndDeadAgents();
		System.out.println("========== SIM: initiated at time point " + timePoint
				+ " with " + agentsContainer.size() + " agents");
	}

	public void populate(final ProjectModel projectModel, final int timePoint) {
		//pickup (possibly selected only) agents from the Mastodon project at the given time point
		final SelectionModel<Spot,Link> currentSpotSelection = projectModel.getSelectionModel();
		final boolean someSpotsSelected = !currentSpotSelection.isEmpty();
		this.time = timePoint;
		for (Spot s : projectModel.getModel().getSpatioTemporalIndex().getSpatialIndex(timePoint)) {
			if (s.getLabel().equals(Simulator.MASTODON_CENTER_SPOT_NAME)) continue;
			if (isHintingSphere(s)) continue;
			if (someSpotsSelected && !currentSpotSelection.isSelected(s)) continue;
			Agent agent = new Agent(this, this.getNewId(), 0, s.getLabel()+"-",
					s.getDoublePosition(0), s.getDoublePosition(1), s.getDoublePosition(2),
					Math.sqrt(s.getBoundingSphereRadiusSquared()), this.time);
			agent.setMostRecentMastodonSpotRepre(s);
			this.registerAgent(agent);
		}
		this.commitNewAndDeadAgents();
		System.out.println("========== SIM: initiated at time point " + timePoint
				+ " with " + agentsContainer.size() + " agents");
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
