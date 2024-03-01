package org.ulman.simulator;

import org.mastodon.mamut.model.Spot;

import java.util.*;

public class Agent {
	private final Simulator simulatorFrame;

	private final String nameClean;
	private final String nameBlocked;
	private final String nameWantDivide;
	private final String nameBlockedWantDivide;
	private String name;
	public String getName() {
		return name;
	}

	private final int id;
	public int getId() {
		return id;
	}

	private final int parentId;

	private int t;
	private double x,y,z;
	private double nextX,nextY,nextZ;
	private final double interestRadius = Simulator.AGENT_SEARCH_RADIUS;

	public double getX() { return x; }
	public double getY() { return y; }
	public double getZ() { return z; }

	private final double usualStepSize = Simulator.AGENT_USUAL_STEP_SIZE;
	private final double minDistanceToNeighbor = Simulator.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT;

	private final int dontDivideBefore;
	private final int dontLiveBeyond;
	private final int maxNeighborsForDivide = Simulator.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION;

	private final List<String> reportLog = new ArrayList<>(100);
	public List<String> getReportLog() {
		return reportLog;
	}
	public void reportStatus() {
		reportLog.add(String.format("%d\t%f\t%f\t%f\t%d\t%d\t%s", this.t, this.x, this.y, this.z, this.id, this.parentId, this.name));
	}

	private Spot previousSpot = null;
	public Spot getPreviousSpot() {
		return previousSpot;
	}
	public void setPreviousSpot(final Spot spot) {
		previousSpot = spot;
	}

	//one generator for all agents
	static private final Random lifeSpanRndGenerator = new Random();
	//per-agent generator of its own movements
	private final Random moveRndGenerator = new Random();

	public Agent(Simulator simulator,
	             int ID, int parentID, String label,
	             double x, double y, double z, int time) {
		this.simulatorFrame = simulator;

		this.name = label;
		this.nameClean = label;
		if (Simulator.PREPEND_HINT_LABELS) {
			this.nameBlocked = "B_" + label;
			this.nameWantDivide = "W_" + label;
			this.nameBlockedWantDivide = "BW_" + label;
		} else {
			this.nameBlocked = label + "_B";
			this.nameWantDivide = label + "_W";
			this.nameBlockedWantDivide = label + "_BW";
		}

		this.id = ID;
		this.parentId = parentID;
		this.t = time;
		this.x = x;
		this.y = y;
		this.z = z;
		this.nextX = x;
		this.nextY = y;
		this.nextZ = z;

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
		if (Simulator.COLLECT_INTERNAL_DATA) this.reportStatus();
	}

	final double[] nearbyCoordinates = new double[300];

	public void doOneTime(boolean fromCurrentPos) {
		final double oldX = fromCurrentPos ? this.x : this.nextX;
		final double oldY = fromCurrentPos ? this.y : this.nextY;
		final double oldZ = fromCurrentPos ? this.z : this.nextZ;

		final int neighborsMaxIdx = simulatorFrame.getListOfOccupiedCoords(this, interestRadius, nearbyCoordinates);
		final int neighborsCnt = neighborsMaxIdx / 3;

		if (Simulator.VERBOSE_AGENT_DEBUG) {
			System.out.printf("advancing agent id %d (%s):%n", this.id, this.name);
			System.out.printf("  from pos [%f,%f,%f] (from_current_pos=%b)%n", oldX, oldY, oldZ, fromCurrentPos);
			System.out.println("  neighs cnt: " + neighborsCnt);
		}

		final double minDistanceSquared = minDistanceToNeighbor * minDistanceToNeighbor;
		double dispX = 0,dispY = 0,dispZ = 0;
		double newX = 0,newY = 0,newZ = 0;

		int doneAttempts = 0;
		boolean tooClose = true;
		while (doneAttempts < Simulator.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE && tooClose) {
			doneAttempts += 1;

			boolean isOdd = (doneAttempts & 1) == 1;
			if (isOdd) {
				dispX = moveRndGenerator.nextGaussian() * (this.usualStepSize / 2.0);
				dispY = moveRndGenerator.nextGaussian() * (this.usualStepSize / 2.0);
				dispZ = moveRndGenerator.nextGaussian() * (this.usualStepSize / 2.0);
			} else {
				dispX /= 2.0;
				dispY /= 2.0;
				dispZ /= 2.0;
			}
			if (Simulator.AGENT_DO_2D_MOVES_ONLY) dispZ = 0.0;

			newX = oldX + dispX;
			newY = oldY + dispY;
			newZ = oldZ + dispZ;

			tooClose = false;
			for (int off = 0; off < neighborsMaxIdx; off += 3) {
				double dx = nearbyCoordinates[off+0] - newX;
				double dy = nearbyCoordinates[off+1] - newY;
				double dz = nearbyCoordinates[off+2] - newZ;
				double dist = dx * dx + dy * dy + dz * dz;
				if (dist < minDistanceSquared) {
					tooClose = true;
					break;
				}
			}

			if (Simulator.VERBOSE_AGENT_DEBUG) {
				System.out.printf("  displacement = (%f,%f), isOdd=%b%n", dispX, dispY, isOdd);
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
			System.out.printf("  established coords [%f,%f,%f] (required %d attempts)%n", this.nextX, this.nextY, this.nextZ, doneAttempts);
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

	public void divideMe() {
		final int d1Id = simulatorFrame.getNewId();
		final int d2Id = simulatorFrame.getNewId();
		final String d1Name = name + "a";
		final String d2Name = name + "b";

		double azimuth = Math.atan2(nextY-y, nextX-x);
		azimuth += Math.PI / 2.0;
		azimuth += moveRndGenerator.nextGaussian() * Simulator.AGENT_MAX_VARIABLITY_FROM_A_PERPENDICULAR_DIVISION_PLANE / 3.0;
		double dx = 0.5 * minDistanceToNeighbor * Math.cos(azimuth);
		double dy = 0.5 * minDistanceToNeighbor * Math.sin(azimuth);
		double dz_a = 0.5 * minDistanceToNeighbor * moveRndGenerator.nextDouble();
		double dz_b = 1.0 - dz_a;
		if (Simulator.AGENT_DO_2D_MOVES_ONLY) {
			dz_a = 0.0;
			dz_b = 0.0;
		}

		Agent d1 = new Agent(simulatorFrame, d1Id, id, d1Name, nextX-dx, nextY-dy, nextZ-dz_a, t + 1);
		Agent d2 = new Agent(simulatorFrame, d2Id, id, d2Name, nextX+dx, nextY+dy, nextZ+dz_b, t + 1);

		d1.previousSpot = this.previousSpot;
		d2.previousSpot = this.previousSpot;

		simulatorFrame.deregisterAgent(this);
		simulatorFrame.registerAgent(d1);
		simulatorFrame.registerAgent(d2);
	}
}
