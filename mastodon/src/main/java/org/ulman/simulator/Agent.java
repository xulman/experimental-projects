package org.ulman.simulator;

import org.mastodon.mamut.model.Spot;

import java.util.*;

public class Agent {
	private final Simulator simulatorFrame;

	// ============= names and IDs (incl. parentID) =============
	private final String nameClean;
	private final String nameBlocked;
	private final String nameWantDivide;
	private final String nameBlockedWantDivide;
	private String name;
	public String getName() {
		return name;
	}

	public static final String ONE_AND_ONLY_NAME = "M";

	private final int id;
	public int getId() {
		return id;
	}

	private final int parentId;


	// ============= geometry (shape and position) =============
	private int t;
	private double x,y,z,R;
	private double nextX,nextY,nextZ,nextR;

	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }
	public double getR() { return R; }


	// ============= agents behaviour aka simulation parameters =============
	//this is the distance _outside_ the agent's outer boundary
	//that the agent cares about (where it looks for another agents)
	private final double lookAroundRadius = Simulator.AGENT_LOOK_AROUND_DISTANCE;

	private final double minDistanceToNeighbor = Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;
	private final double usualStepSize = Simulator.AGENT_USUAL_STEP_SIZE;
	private final double daughtersInitialDisplacement = Simulator.AGENT_DAUGHTERS_INITIAL_DISTANCE;

	private final int dontDivideBefore;
	private final int dontLiveBeyond;
	private final int maxNeighborsForDivide = Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;

	//one generator for all agents
	static private final Random lifeSpanRndGenerator = new Random();
	//per-agent generator of its own movements
	private final Random moveRndGenerator = new Random();


	// ============= reporting =============
	private final List<String> reportLog = new ArrayList<>(100);
	public List<String> getReportLog() {
		return reportLog;
	}
	public void reportStatus() {
		reportLog.add(String.format("%d\t%f\t%f\t%f\t%d\t%d\t%s", this.t, this.x, this.y, this.z, this.id, this.parentId, this.name));
	}


	// ============= interaction with Mastodon =============
	private Spot mostRecentMastodonSpotRepre = null;
	public boolean isMostRecentMastodonSpotValid() {
		return mostRecentMastodonSpotRepre != null;
	}
	public Spot getMostRecentMastodonSpotRepre() {
		return mostRecentMastodonSpotRepre;
	}
	public void setMostRecentMastodonSpotRepre(final Spot initToThis) {
		if (initToThis == null) return;
		if (mostRecentMastodonSpotRepre == null) {
			mostRecentMastodonSpotRepre = initToThis.getModelGraph().vertexRef();
		}
		mostRecentMastodonSpotRepre.refTo(initToThis);
	}
	private void releaseMostRecentMastodonSpotRepre() {
		if (mostRecentMastodonSpotRepre != null) {
			mostRecentMastodonSpotRepre.getModelGraph().releaseRef( mostRecentMastodonSpotRepre );
			mostRecentMastodonSpotRepre = null;
		}
	}


	// ============= "external" API =============
	public Agent(Simulator simulator,
	             int ID, int parentID, String label,
	             double x, double y, double z, double radius, int time) {
		this.simulatorFrame = simulator;

		switch (Simulator.LABELS_NAMING_POLICY) {
		case USE_ALWAYS_M:
			this.name = ONE_AND_ONLY_NAME;
			this.nameClean = ONE_AND_ONLY_NAME;
			this.nameBlocked = ONE_AND_ONLY_NAME;
			this.nameWantDivide = ONE_AND_ONLY_NAME;
			this.nameBlockedWantDivide = ONE_AND_ONLY_NAME;
			break;
		case ENCODING_LABELS_AND_PREPENDING:
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = "B_" + label;
			this.nameWantDivide = "W_" + label;
			this.nameBlockedWantDivide = "BW_" + label;
			break;
		case ENCODING_LABELS_AND_APPENDING:
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = label + "_B";
			this.nameWantDivide = label + "_W";
			this.nameBlockedWantDivide = label + "_BW";
			break;
		default: //NB: the same as ENCODING_LABELS
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = label;
			this.nameWantDivide = label;
			this.nameBlockedWantDivide = label;
		}

		this.id = ID;
		this.parentId = parentID;
		this.t = time;
		this.x = x;
		this.y = y;
		this.z = z;
		this.R = radius;
		this.nextX = x;
		this.nextY = y;
		this.nextZ = z;
		this.nextR = radius;

		double meanLifePeriod = Simulator.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION;
		double sigma = (0.6 * meanLifePeriod) / 3.0;
		this.dontDivideBefore = time + (int)(lifeSpanRndGenerator.nextGaussian() * sigma + meanLifePeriod);
		this.dontLiveBeyond = time + Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER;

		if (parentID == 0 && Simulator.COLLECT_INTERNAL_DATA) {
			this.reportStatus();
		}

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("NEW AGENT %d (%s), parent %d @ [%f,%f,%f] tp=%d, divTime=%d, dieTime=%d%n", ID, label, parentID, x, y, z, time, this.dontDivideBefore, this.dontLiveBeyond);
		}
	}

	public void progress(int tillThisTime) {
		boolean firstGo = true;
		while (this.t < tillThisTime) {
			this.doOneTime(firstGo);
			firstGo = false;
		}
	}

	public void progressFinish() {
		this.x = this.nextX;
		this.y = this.nextY;
		this.z = this.nextZ;
		this.R = this.nextR;
		if (Simulator.COLLECT_INTERNAL_DATA) this.reportStatus();
	}


	// ============= "internal" API =============
	//100-times: x,y,z,R
	final double[] nearbySpheres = new double[400];
	final int nearbySpheresStride = 4;

	protected void doOneTime(boolean fromCurrentPos) {
		final double oldX = fromCurrentPos ? this.x : this.nextX;
		final double oldY = fromCurrentPos ? this.y : this.nextY;
		final double oldZ = fromCurrentPos ? this.z : this.nextZ;
		final double oldR = fromCurrentPos ? this.R : this.nextR;

		final int neighborsMaxIdx = simulatorFrame.getListOfOccupiedCoords(this, lookAroundRadius, nearbySpheres);
		final int neighborsCnt = neighborsMaxIdx / nearbySpheresStride;

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("advancing agent id %d (%s):%n", this.id, this.name);
			System.out.printf("  from pos [%f,%f,%f] (from_current_pos=%b)%n", oldX, oldY, oldZ, fromCurrentPos);
			System.out.println("  neighs cnt: " + neighborsCnt);
		}

		double dispX = 0,dispY = 0,dispZ = 0;
		double newX = 0,newY = 0,newZ = 0;

		//calculate displacement step that finds "dominant" way to get away from the nearby agents
		double dispAwayX = 0,dispAwayY = 0,dispAwayZ = 0;
		double sumOfWeights = 0;
		for (int off = 0; off < neighborsMaxIdx; off += nearbySpheresStride) {
			double dx = oldX - nearbySpheres[off+0];
			double dy = oldY - nearbySpheres[off+1];
			double dz = oldZ - nearbySpheres[off+2];
			double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx /= dist; dy /= dist; dz /= dist;  //displacement vector is now normalized

			dist -= oldR + nearbySpheres[off+3]; //the actual (surface) distance to the nearby agent
			if (dist > minDistanceToNeighbor) continue; //too far to care...

			//how much to move to get surfaces exactly "minDistance" far from each other
			dist = minDistanceToNeighbor - dist;

			dx *= 0.5 * dist; //displacement vector of the appropriate size
			dy *= 0.5 * dist; //half is taken because the other agent will do the same move
			dz *= 0.5 * dist;

			dist = Math.min(dist,minDistanceToNeighbor); //NB: agents' overlap is not worse
			                                             //than just touching surfaces
			double weight = dist / minDistanceToNeighbor; //NB: [0:1] scale
			weight *= weight;                             //quadratic -> longer moves get more attention

			dispAwayX += weight * dx;
			dispAwayY += weight * dy;
			dispAwayZ += weight * dz;
			sumOfWeights += weight;
		}
		dispAwayX /= sumOfWeights;
		dispAwayY /= sumOfWeights;
		dispAwayZ /= sumOfWeights;

		//NB: if 'step' is a distance along one axis, the total length in the space is sqrt(spaceDim)-times larger
		final double stepSizeDimensionalityCompensation = Simulator.AGENT_DO_2D_MOVES_ONLY ? 1.41 : 1.73;
		final double stepSize = usualStepSize / stepSizeDimensionalityCompensation;
		double slowDownFactor = 1.0;
		final double sumOfWeights_heavyCollisionThreshold = 0.7;

		int moveAttemptsCnt = 0;
		boolean tooClose = true;
		while (moveAttemptsCnt < Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE && tooClose) {
			slowDownFactor = 1.0 - (
					(double)moveAttemptsCnt / (double)Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE );
			//if, however, there is "a lot of 'collision'", we additionally
			//lower the contribution of the random step
			if (sumOfWeights > sumOfWeights_heavyCollisionThreshold) slowDownFactor *= 0.5;
			moveAttemptsCnt += 1;

			//the random step is attenuated increasingly more with the increasing moveAttemptsCnt
			dispX = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			dispY = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			dispZ = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			if (Simulator.AGENT_DO_2D_MOVES_ONLY) dispZ = 0.0;

			newX = oldX + dispX + dispAwayX;
			newY = oldY + dispY + dispAwayY;
			newZ = oldZ + dispZ + dispAwayZ;

			tooClose = false;
			for (int off = 0; off < neighborsMaxIdx; off += nearbySpheresStride) {
				double dx = nearbySpheres[off+0] - newX;
				double dy = nearbySpheres[off+1] - newY;
				double dz = nearbySpheres[off+2] - newZ;
				double dist = Math.sqrt(dx*dx + dy*dy + dz*dz) - oldR - nearbySpheres[off+3];
				if (dist < minDistanceToNeighbor) {
					tooClose = true;
					break;
				}
			}

			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.printf("  random displacement = (%f,%f,%f), slowDownFactor = %f%n",
						dispX, dispY, dispZ, slowDownFactor);
				System.out.printf("   away  displacement = (%f,%f,%f), heavy collision = %b, sumOfWeights=%f%n",
						dispAwayX, dispAwayY, dispAwayZ, sumOfWeights > sumOfWeights_heavyCollisionThreshold, sumOfWeights);
				System.out.printf("  trying pos [%f,%f,%f], too_close=%b%n", newX, newY, newZ, tooClose);
			}
		}

		if (!tooClose) {
			this.nextX = newX;
			this.nextY = newY;
			this.nextZ = newZ;
			this.name = this.nameClean;
		} else {
			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.println("  couldn't move when " + neighborsCnt + " neighbors are around");
			}
			this.name = this.nameBlocked;
		}
		this.t += 1;

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("  established coords [%f,%f,%f] (required %d attempts)%n", this.nextX, this.nextY, this.nextZ, moveAttemptsCnt);
			System.out.printf("  when %d neighbors around, too_close=%b%n", neighborsCnt, tooClose);
		}

		if (this.t > this.dontLiveBeyond) {
			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.println("  dying now!");
			}
			this.simulatorFrame.deregisterAgent(this);
		} else if (this.t > this.dontDivideBefore) {
			if (neighborsCnt <= this.maxNeighborsForDivide && !tooClose) {
				if (Simulator.VERBOSE_AGENT_DEBUG) {
					System.out.println("  dividing!");
				}
				this.divideMe();
			} else {
				if (Simulator.VERBOSE_AGENT_DEBUG) {
					System.out.printf("  should divide but space seems to be full... (%d neighbors, too_close=%b)%n", neighborsCnt, tooClose);
				}
				this.name = tooClose ? this.nameBlockedWantDivide : this.nameWantDivide;
			}
		}
	}

	protected void divideMe() {
		final int d1Id = simulatorFrame.getNewId();
		final int d2Id = simulatorFrame.getNewId();
		final String d1Name = name + "a";
		final String d2Name = name + "b";

		double dz = moveRndGenerator.nextDouble();
		final double stepSize = Simulator.AGENT_DO_2D_MOVES_ONLY ?
				daughtersInitialDisplacement : (daughtersInitialDisplacement / Math.sqrt(1 + dz*dz));

		double azimuth = Math.atan2(nextY-y, nextX-x);
		azimuth += Math.PI / 2.0;
		azimuth += moveRndGenerator.nextGaussian() * Simulator.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE / 3.0;
		double dx = stepSize * Math.cos(azimuth);
		double dy = stepSize * Math.sin(azimuth);
		dz = Simulator.AGENT_DO_2D_MOVES_ONLY ? 0.0 : (dz*stepSize);

		Agent d1 = new Agent(simulatorFrame, d1Id, id, d1Name, nextX-dx, nextY-dy, nextZ-dz, nextR, t + 1);
		Agent d2 = new Agent(simulatorFrame, d2Id, id, d2Name, nextX+dx, nextY+dy, nextZ+dz, nextR, t + 1);
		//NB: mother must have existed for at least one time point, and thus must exist its Mastodon representation
		d1.setMostRecentMastodonSpotRepre(this.mostRecentMastodonSpotRepre);
		d2.setMostRecentMastodonSpotRepre(this.mostRecentMastodonSpotRepre);
		this.releaseMostRecentMastodonSpotRepre();

		simulatorFrame.deregisterAgent(this);
		simulatorFrame.registerAgent(d1);
		simulatorFrame.registerAgent(d2);
	}
}
