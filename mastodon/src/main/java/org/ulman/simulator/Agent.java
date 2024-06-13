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
	private final String nameBuldozer;
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
	private final double daughtersDozeringDisplacement = Simulator.AGENT_DAUGHTERS_DOZERING_DISTANCE;
	private static final double EPSILON = 0.00005;
	//
	private final int daughtersInitialBuldozer = Simulator.AGENT_DAUGHTERS_DOZERING_TIME_PERIOD;
	private double divBuldozerDx=0, divBuldozerDy=0, divBuldozerDz=0;
	private int divBuldozerStopTP = -1; //-1 means not active

	private final int slowDownForDivisionPeriod;
	private int dontDivideBefore;
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
			this.nameBuldozer = ONE_AND_ONLY_NAME;
			break;
		case ENCODING_LABELS_AND_PREPENDING:
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = "B_" + label;
			this.nameWantDivide = "W_" + label;
			this.nameBlockedWantDivide = "BW_" + label;
			this.nameBuldozer = "DZ_" + label;
			break;
		case ENCODING_LABELS_AND_APPENDING:
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = label + "_B";
			this.nameWantDivide = label + "_W";
			this.nameBlockedWantDivide = label + "_BW";
			this.nameBuldozer = label + "_DZ";
			break;
		default: //NB: the same as ENCODING_LABELS
			this.name = label;
			this.nameClean = label;
			this.nameBlocked = label;
			this.nameWantDivide = label;
			this.nameBlockedWantDivide = label;
			this.nameBuldozer = label;
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
		this.dontDivideBefore = time + Math.max((int)(lifeSpanRndGenerator.nextGaussian() * sigma + meanLifePeriod),1);
		this.slowDownForDivisionPeriod = (int)Math.floor(0.15*meanLifePeriod);
		this.dontLiveBeyond = time + Math.max(Simulator.AGENT_MAX_LIFESPAN_AND_DIES_AFTER,1);
		//NB: make sure the lifespan is always at least one time point

		if (parentID == 0 && Simulator.COLLECT_INTERNAL_DATA) {
			this.reportStatus();
		}

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("NEW AGENT %d (%s), parent %d @ [%f,%f,%f] tp=%d, (slowPeriod=%d) divTime=%d, dieTime=%d%n",
				ID, label, parentID, x, y, z, time, this.slowDownForDivisionPeriod, this.dontDivideBefore, this.dontLiveBeyond);
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

		if ( doBuldozering(oldX,oldY,oldZ, oldR) ) return;

		final int neighborsMaxIdx = simulatorFrame.getListOfOccupiedCoords(this, lookAroundRadius, nearbySpheres);
		final int neighborsCnt = neighborsMaxIdx / nearbySpheresStride;

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("advancing agent id %d (%s) @ %d:%n", this.id, this.name, this.t);
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
			dist = Math.min(0.7 * dist, 0.5*oldR);
			//NB:
			//half (0.5) should be taken because the other agent will do the same move;
			//but since agents jump chaotically, we better displace a little more (0.7);
			//and yet don't move more than a quarter of agent's own size

			dx *= dist; //displacement vector now of the appropriate size
			dy *= dist;
			dz *= dist;

			dist = Math.min(dist,minDistanceToNeighbor); //NB: agents' overlap is not worse
			                                             //than just touching surfaces
			double weight = dist / minDistanceToNeighbor; //NB: [0:1] scale
			weight *= weight;                             //quadratic -> longer moves get more attention

			dispAwayX += weight * dx;
			dispAwayY += weight * dy;
			dispAwayZ += weight * dz;
			sumOfWeights += weight;

			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.printf("  detected away displacement = (%f,%f,%f) of weight = %f%n",dx,dy,dz,weight);
			}
		}
		if (sumOfWeights > 0) {
			dispAwayX /= sumOfWeights;
			dispAwayY /= sumOfWeights;
			dispAwayZ /= sumOfWeights;
		}

		//make sure the relevant hinting spheres are extracted and ready
		simulatorFrame.updateSphereCaches(this.t+1);

		//NB: if 'step' is a distance along one axis, the total length in the space is sqrt(spaceDim)-times larger
		final double stepSizeDimensionalityCompensation
				= Simulator.AGENT_DO_2D_MOVES_ONLY == Agent2dMovesRestriction.NO_RESTRICTION ? 1.73 : 1.41;
		final double stepSize = usualStepSize / stepSizeDimensionalityCompensation;
		double slowDownFactor = 1.0;
		final double slowDownFactor_division = 0.2 + Math.min( Math.max(0,dontDivideBefore-1 -this.t) / (double)slowDownForDivisionPeriod , 0.8);
		final double sumOfWeights_heavyCollisionThreshold = 0.7;

		int moveAttemptsCnt = 0;
		boolean tooClose = true;
		while (moveAttemptsCnt < Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE && tooClose) {
			slowDownFactor = 1.0 - (
					(double)moveAttemptsCnt / (double)Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE );
			//if, however, there is "a lot of 'collision'", we additionally
			//lower the contribution of the random step
			slowDownFactor *= slowDownFactor_division;
			if (sumOfWeights > sumOfWeights_heavyCollisionThreshold) slowDownFactor *= 0.5;
			moveAttemptsCnt += 1;

			//the random step is attenuated increasingly more with the increasing moveAttemptsCnt
			dispX = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			dispY = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			dispZ = moveRndGenerator.nextGaussian() * stepSize * slowDownFactor;
			//
			switch (Simulator.AGENT_DO_2D_MOVES_ONLY) {
			case NO_X_AXIS_MOVE:
				dispX = 0.0;
				break;
			case NO_Y_AXIS_MOVE:
				dispY = 0.0;
				break;
			case NO_Z_AXIS_MOVE:
				dispZ = 0.0;
				break;
			}

			newX = oldX + dispX + dispAwayX;
			newY = oldY + dispY + dispAwayY;
			newZ = oldZ + dispZ + dispAwayZ;

			//init:
			dispHintingSpheres[0] = 0.0;
			dispHintingSpheres[1] = 0.0;
			dispHintingSpheres[2] = 0.0;
			dispHintingCnt = 0;
			//
			//cumulate:
			suggestMoveBasedOnStayInsideSpheres( oldX,oldY,oldZ, newX,newY,newZ );
			suggestMoveBasedOnKeepOutSpheres( newX,newY,newZ );
			suggestMoveBasedOnHoldPositionSpheres( oldX,oldY,oldZ, newX,newY,newZ );
			//
			//finalize:
			if (dispHintingCnt > 0) {
				dispHintingSpheres[0] /= (double)dispHintingCnt;
				dispHintingSpheres[1] /= (double)dispHintingCnt;
				dispHintingSpheres[2] /= (double)dispHintingCnt;
				if (Simulator.VERBOSE_AGENT_DEBUG) {
					System.out.printf("  hinted displacement = (%f,%f,%f) from %d hinting spheres%n",
							dispHintingSpheres[0],dispHintingSpheres[1],dispHintingSpheres[2], dispHintingCnt);
				}
			}
			//
			//apply:
			newX += dispHintingSpheres[0];
			newY += dispHintingSpheres[1];
			newZ += dispHintingSpheres[2];

			tooClose = false;
			for (int off = 0; off < neighborsMaxIdx; off += nearbySpheresStride) {
				double dx = nearbySpheres[off+0] - newX;
				double dy = nearbySpheres[off+1] - newY;
				double dz = nearbySpheres[off+2] - newZ;
				double dist = Math.sqrt(dx*dx + dy*dy + dz*dz) - oldR - nearbySpheres[off+3];
				//NB: little more tolerant here...
				if (dist < (minDistanceToNeighbor-EPSILON)) {
					tooClose = true;
					break;
				}
			}

			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.printf("  away   displacement = (%f,%f,%f), heavy collision = %b, sumOfWeights=%f%n",
						dispAwayX, dispAwayY, dispAwayZ, sumOfWeights > sumOfWeights_heavyCollisionThreshold, sumOfWeights);
				System.out.printf("  random displacement = (%f,%f,%f), slowDownFactor = %f (slowDF_division = %f)%n",
						dispX, dispY, dispZ, slowDownFactor, slowDownFactor_division);
				System.out.printf("  trying pos [%f,%f,%f], too_close=%b%n", newX, newY, newZ, tooClose);
			}
		}

		//always move...
		this.nextX = newX;
		this.nextY = newY;
		this.nextZ = newZ;
		//...and indicate if that move is good or not
		if (!tooClose) {
			this.name = this.nameClean;
		} else {
			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.println("  collision move 'cause " + neighborsCnt + " neighbors are around");
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
				//
				final boolean managedToDivide = this.divideMe();
				//
				this.dontDivideBefore = this.t + 2;
				if (Simulator.VERBOSE_AGENT_DEBUG && !managedToDivide) {
					System.out.println("  FAILED dividing! will try again at time point "+(dontDivideBefore+1));
				}
			} else {
				if (Simulator.VERBOSE_AGENT_DEBUG) {
					System.out.printf("  should divide but space seems to be full... (%d neighbors, too_close=%b)%n", neighborsCnt, tooClose);
				}
			}
			this.name = tooClose ? this.nameBlockedWantDivide : this.nameWantDivide;
		}
	}

	protected boolean divideMe() {
		final double d1Radius = this.R;
		final double d2Radius = this.R;
		final double daughtersCentresHalfDistance = 0.5*(d1Radius + daughtersInitialDisplacement + d2Radius);

		//look just enough (and often further than normally) around to see enough to host two daughters side-by-side;
		//so, the furtherest surface of the bigger daughter from mother's centre, minus mother's radius:
		final double lookAroundDist = daughtersCentresHalfDistance + Math.max(d1Radius,d2Radius) - this.R;
		final int neighborsMaxIdx = simulatorFrame.getListOfOccupiedCoords(this, lookAroundDist, nearbySpheres);

		//NB: it is assumed that agent/cell is no longer buldozering when it reaches divideMe(), so we can modify the buldozering vector now
		//but, is there any valid/already-used buldozering vector at all?
		if (divBuldozerDx == 0.0 && divBuldozerDy == 0.0 && divBuldozerDz == 0.0) {
			//nope, let's create one
			divBuldozerDx = moveRndGenerator.nextDouble()*2.0 - 1.0; //interval: -1.0 <-> 1.0
			divBuldozerDy = moveRndGenerator.nextDouble()*2.0 - 1.0;
			divBuldozerDz = moveRndGenerator.nextDouble()*2.0 - 1.0;
		}
		//
		//normalize the last dozering travel vector
		//NB: could be that this is another divideMe() attempt and so this vector has been normalized already
		double divBuldozerLen = Math.sqrt(divBuldozerDx*divBuldozerDx + divBuldozerDy*divBuldozerDy + divBuldozerDz*divBuldozerDz);
		if (Math.abs(divBuldozerLen - 1.0) > EPSILON) {
			//wasn't already normalized, let's normalize now
			divBuldozerDx /= divBuldozerLen;
			divBuldozerDy /= divBuldozerLen;
			divBuldozerDz /= divBuldozerLen;
		}

		int remainingTries = 20;
		int proximityCounter = 9999;

		double dx = 0, dy = 0, dz = 0, azimuth;
		while (remainingTries > 0 && proximityCounter > 0) {
			--remainingTries;

			//division vector:
			switch (Simulator.AGENT_DO_2D_MOVES_ONLY) {
			case NO_X_AXIS_MOVE:
				azimuth = Math.atan2(divBuldozerDz, divBuldozerDy);
				azimuth += moveRndGenerator.nextGaussian() * Simulator.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES / 3.0;
				dx = 0.0;
				dy = Math.cos(azimuth);
				dz = Math.sin(azimuth);
				break;
			case NO_Y_AXIS_MOVE:
				azimuth = Math.atan2(divBuldozerDz, divBuldozerDx);
				azimuth += moveRndGenerator.nextGaussian() * Simulator.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES / 3.0;
				dx = Math.cos(azimuth);
				dy = 0.0;
				dz = Math.sin(azimuth);
				break;
			case NO_Z_AXIS_MOVE:
				azimuth = Math.atan2(divBuldozerDy, divBuldozerDx);
				azimuth += moveRndGenerator.nextGaussian() * Simulator.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES / 3.0;
				dx = Math.cos(azimuth);
				dy = Math.sin(azimuth);
				dz = 0.0;
				break;
			default: //full 3D case
				azimuth = Math.atan2(divBuldozerDy, divBuldozerDx);
				azimuth += moveRndGenerator.nextGaussian() * 0.8 * Simulator.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES / 3.0;
				//NB: the azimuth changes in both "axes" can move within a square while we needed it move within a circle, so we
				//    reduce the size of the square to 80% to compensate... (as a square corner stretches far beyond the circle)
				double twoDlen = Math.sqrt(divBuldozerDy*divBuldozerDy + divBuldozerDx*divBuldozerDx);
				dx = twoDlen * Math.cos(azimuth);
				dy = twoDlen * Math.sin(azimuth);
				dz = divBuldozerDz;

				azimuth = Math.atan2(dz, dx);
				azimuth += moveRndGenerator.nextGaussian() * 0.8 * Simulator.AGENT_MAX_VARIABILITY_OF_DIVISION_PLANES / 3.0;
				twoDlen = Math.sqrt(dz*dz + dx*dx);
				dx = twoDlen * Math.cos(azimuth);
				dz = twoDlen * Math.sin(azimuth);
			}
			final double dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
			//NB: should be always ~ 1.0 !!
			if (Math.abs(dLen - 1.0) > EPSILON) {
				System.out.println("DIVISION CORRUPTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}

			//direction and distance for the initial placement of both daughters:
			//(since both will move, it is enough to move each only by half of the total needed displacement)
			final double nowStepLen = daughtersCentresHalfDistance / dLen;
			dx *= nowStepLen;
			dy *= nowStepLen;
			dz *= nowStepLen;

			//check now the future placement of both daughters:
			//  if bad, try again... if still bad, we don't divide now
			//  else we continue below...
			proximityCounter = 0;
			for (int off = 0; off < neighborsMaxIdx; off += nearbySpheresStride) {
				proximityCounter += isSphereTooCloseToNeigh(nextX-dx, nextY-dy, nextZ-dz, d1Radius, off) ? 1 : 0;
				proximityCounter += isSphereTooCloseToNeigh(nextX+dx, nextY+dy, nextZ+dz, d2Radius, off) ? 1 : 0;
			}
			if (Simulator.VERBOSE_AGENT_DEBUG && proximityCounter > 0) {
				System.out.println("  daughters placement found in "+proximityCounter+" collisions, trying again");
			}
		}
		if (proximityCounter > 0) return false;

		//memorize the direction and the full distance to travel for the "buldozering":
		//NB: the (dx,dy,dz) vector is now of the length 'daughtersCentresHalfDistance', which is guaranteed to never be zero!
		final double buldozeringLen = 0.5*(daughtersDozeringDisplacement - daughtersInitialDisplacement) / daughtersCentresHalfDistance;
		divBuldozerDx = buldozeringLen * dx;
		divBuldozerDy = buldozeringLen * dy;
		divBuldozerDz = buldozeringLen * dz;

		//all seems well incl. where to place the daughters, let's introduce them to the Simulator (and deregister this mother)
		final int d1Id = simulatorFrame.getNewId();
		final int d2Id = simulatorFrame.getNewId();
		final String d1Name = name + "a";
		final String d2Name = name + "b";

		Agent d1 = new Agent(simulatorFrame, d1Id, id, d1Name, nextX-dx, nextY-dy, nextZ-dz, d1Radius, t);
		Agent d2 = new Agent(simulatorFrame, d2Id, id, d2Name, nextX+dx, nextY+dy, nextZ+dz, d2Radius, t);
		//NB: mother must have existed for at least one time point, and thus must exist its Mastodon representation
		d1.setMostRecentMastodonSpotRepre(this.mostRecentMastodonSpotRepre);
		d2.setMostRecentMastodonSpotRepre(this.mostRecentMastodonSpotRepre);
		this.releaseMostRecentMastodonSpotRepre();

		simulatorFrame.deregisterAgent(this);
		simulatorFrame.registerAgent(d1);
		simulatorFrame.registerAgent(d2);

		//tell daughters the direction and the full distance to travel for the "buldozering":
		d1.divBuldozerDx = -this.divBuldozerDx;
		d1.divBuldozerDy = -this.divBuldozerDy;
		d1.divBuldozerDz = -this.divBuldozerDz;
		d2.divBuldozerDx =  this.divBuldozerDx;
		d2.divBuldozerDy =  this.divBuldozerDy;
		d2.divBuldozerDz =  this.divBuldozerDz;
		d1.divBuldozerStopTP = t+daughtersInitialBuldozer;
		d2.divBuldozerStopTP = t+daughtersInitialBuldozer;

		return true; //division has happened
	}

	/** given one agent explicitly as [posx,posy,posz,R] and another agent implicitly via offset [ nearbySpheres[neighOffset] ],
	 *  the method returns true if the two agents are surface-to-surface closer than Agent.daughtersInitialDisplacement */
	private boolean isSphereTooCloseToNeigh(double posx, double posy, double posz, double R, int neighOffset) {
			double dx = posx - nearbySpheres[neighOffset+0];
			double dy = posy - nearbySpheres[neighOffset+1];
			double dz = posz - nearbySpheres[neighOffset+2];
			double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
			dist -= R + nearbySpheres[neighOffset+3]; //the actual (surface) distance to the nearby agent
			return dist < daughtersInitialDisplacement;
	}


	protected boolean doBuldozering(final double fromHereX, final double fromHereY, final double fromHereZ, final double oldR) {
		final int remainingTimePoints = this.divBuldozerStopTP - (this.t+1); //NB: as if already in the now-creating (future) time point
		if (remainingTimePoints < 0) return false;

		//now, a combination of what is in divideMe() and dispAwayX,Y,Z from doOneTime()
		//NB: searching only for overlapping/colliding neighbors
		final int neighborsMaxIdx = simulatorFrame.getListOfOccupiedCoords(this, 0.0, nearbySpheres);
		//
		double dispAwayX = 0,dispAwayY = 0,dispAwayZ = 0;
		int dispAwayCnt = 0;
		for (int off = 0; off < neighborsMaxIdx; off += nearbySpheresStride) {
			double dx = fromHereX - nearbySpheres[off+0];
			double dy = fromHereY - nearbySpheres[off+1];
			double dz = fromHereZ - nearbySpheres[off+2];
			double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx /= dist; dy /= dist; dz /= dist;  //displacement vector is now normalized

			dist -= oldR + nearbySpheres[off+3]; //the actual (surface) distance to get outside the current overlapping constellation
			//NB: dist should be non-positive, but just in case....
			if (dist >= 0.0) continue;

			dist *= -0.7;
			//half (0.5) should be taken because the other agent will do the same move;
			//but since agents jump chaotically, we better displace a little more (0.7);
			dispAwayX += dist * dx;
			dispAwayY += dist * dy;
			dispAwayZ += dist * dz;
			dispAwayCnt++;
		}
		if (dispAwayCnt > 0) {
			dispAwayX /= (double)dispAwayCnt;
			dispAwayY /= (double)dispAwayCnt;
			dispAwayZ /= (double)dispAwayCnt;
		}

		//NB: steps(x) = 0.5 * (x + x*x) -- the sum of arithmetic sequence 1...to...x
		//when k-steps (where k = 0...N-1) is left from N-step plan, the current move shall be:
		//    steps(k+1)/steps(N) - steps(k)/steps(N)
		// which is massaged into:
		//    2*(1+k)/(N+N*N)
		//
		final double currentStepLen =
				(double)(2*(1+remainingTimePoints)) / (double)(daughtersInitialBuldozer*(1+daughtersInitialBuldozer));

		this.nextX = fromHereX + currentStepLen*divBuldozerDx + dispAwayX;
		this.nextY = fromHereY + currentStepLen*divBuldozerDy + dispAwayY;
		this.nextZ = fromHereZ + currentStepLen*divBuldozerDz + dispAwayZ;

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("advancing agent id %d (%s) @ %d in buldozer-mode:%n", this.id, this.name, this.t);
			System.out.printf("  from pos [%f,%f,%f] when overlapping neighs cnt %d%n", fromHereX, fromHereY, fromHereZ, neighborsMaxIdx/nearbySpheresStride);
			System.out.printf("  away displacement = (%f,%f,%f), sumOfWeights=%d%n", dispAwayX, dispAwayY, dispAwayZ, dispAwayCnt);
			System.out.printf("  in buldozer-mode  = (%f,%f,%f), phase (%d/%d)%n",
					currentStepLen*divBuldozerDx,currentStepLen*divBuldozerDy,currentStepLen*divBuldozerDz, remainingTimePoints,daughtersInitialBuldozer);
			System.out.printf("  established coords [%f,%f,%f]%n", this.nextX,this.nextY,this.nextZ);
		}

		this.t += 1;
		this.name = this.nameBuldozer;

		return true;
	}


	protected final double[] dispHintingSpheres = new double[3];
	protected int dispHintingCnt = 0;

	protected void suggestMoveBasedOnStayInsideSpheres(
	                           final double oldX,
	                           final double oldY,
	                           final double oldZ,
	                           final double newX,
	                           final double newY,
	                           final double newZ) {
		for (int off = 0; off < simulatorFrame.stayInsideSpheresSharedArrayMaxUsedIdx; off += nearbySpheresStride) {
			double dx = oldX - simulatorFrame.stayInsideSpheresSharedArray[off+0]; //NB: direction doesn't matter now
			double dy = oldY - simulatorFrame.stayInsideSpheresSharedArray[off+1];
			double dz = oldZ - simulatorFrame.stayInsideSpheresSharedArray[off+2];
			double dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
			if (dLen < simulatorFrame.stayInsideSpheresSharedArray[off+3]) {
				//old pos was inside this hinting sphere, then: make sure the new pos is not outside
				dx = simulatorFrame.stayInsideSpheresSharedArray[off+0] - newX;
				dy = simulatorFrame.stayInsideSpheresSharedArray[off+1] - newY;
				dz = simulatorFrame.stayInsideSpheresSharedArray[off+2] - newZ;
				dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
				double dist = dLen - simulatorFrame.stayInsideSpheresSharedArray[off+3];
				if (dist > 0) {
					//new pos is outside
					dist = Math.min(1.1 * dist, this.usualStepSize);
					dispHintingSpheres[0] += dx * dist / dLen;
					dispHintingSpheres[1] += dy * dist / dLen;
					dispHintingSpheres[2] += dz * dist / dLen;
					dispHintingCnt++;
				}
			}
		}
	}

	protected void suggestMoveBasedOnKeepOutSpheres(
	                           final double newX,
	                           final double newY,
	                           final double newZ) {
		for (int off = 0; off < simulatorFrame.keepOutSpheresSharedArrayMaxUsedIdx; off += nearbySpheresStride) {
			double dx = newX - simulatorFrame.keepOutSpheresSharedArray[off+0];
			double dy = newY - simulatorFrame.keepOutSpheresSharedArray[off+1];
			double dz = newZ - simulatorFrame.keepOutSpheresSharedArray[off+2];
			double dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
			double dist = simulatorFrame.keepOutSpheresSharedArray[off+3] - dLen;
			if (dist > 0) {
				dist = Math.min(dist, this.usualStepSize);
				dispHintingSpheres[0] += dx * dist / dLen;
				dispHintingSpheres[1] += dy * dist / dLen;
				dispHintingSpheres[2] += dz * dist / dLen;
				dispHintingCnt++;
			}
		}
	}

	protected void suggestMoveBasedOnHoldPositionSpheres(
	                           final double oldX,
	                           final double oldY,
	                           final double oldZ,
	                           final double newX,
	                           final double newY,
	                           final double newZ) {
		/*
		for (int off = 0; off < simulatorFrame.holdPositionSpheresSharedArrayMaxUsedIdx; off += nearbySpheresStride) {
			double dx = oldX - simulatorFrame.holdPositionSpheresSharedArray[off+0]; //NB: direction doesn't matter now
			double dy = oldY - simulatorFrame.holdPositionSpheresSharedArray[off+1];
			double dz = oldZ - simulatorFrame.holdPositionSpheresSharedArray[off+2];
			double dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
			if (simulatorFrame.holdPositionSpheresSharedArray[off+3] > dLen) {
				//old pos was inside this hinting sphere, then: make sure the new pos is not outside
				dx = simulatorFrame.holdPositionSpheresSharedArray[off+0] - newX;
				dy = simulatorFrame.holdPositionSpheresSharedArray[off+1] - newY;
				dz = simulatorFrame.holdPositionSpheresSharedArray[off+2] - newZ;
				dLen = Math.sqrt(dx*dx + dy*dy + dz*dz);
				double dist = dLen - simulatorFrame.holdPositionSpheresSharedArray[off+3];
				if (dist > 0) {
					//new pos is outside
					dist = Math.min(dist, this.usualStepSize);
					dispHintingSpheres[0] += dx * dist / dLen;
					dispHintingSpheres[1] += dy * dist / dLen;
					dispHintingSpheres[2] += dz * dist / dLen;
					dispHintingCnt++;
				}
			}
		}
		*/
	}
}
