package org.ulman.simulator;

import java.io.*;
import java.util.*;


public class Simulator {
	private int assignedIds = 0;
	private int time = 0;
	private final Map<Integer, Agent> agentsContainer = new HashMap<>(10000);
	private final List<Agent> newAgentsContainer = new ArrayList<>(100);
	private final List<Agent> deadAgentsContainer = new ArrayList<>(100);
	private final PrintWriter reportFile;

	public Simulator(String logFilePath) throws FileNotFoundException {
		this.reportFile = new PrintWriter(logFilePath);
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
			this.reportAgentLog(spot);
		}

		for (Agent spot : this.newAgentsContainer) {
			this.agentsContainer.put(spot.getId(), spot);
		}
	}

	public void reportAgentLog(Agent spot) {
		for (String line : spot.getReportLog()) {
			reportFile.println(line);
		}
		reportFile.println();
		reportFile.println();
	}

	public List<double[]> getListOfOccupiedCoords(Agent fromThisSpot) {
		final double interestRadius = fromThisSpot.getInterestRadius();
		final double minX = fromThisSpot.getX() - interestRadius;
		final double minY = fromThisSpot.getY() - interestRadius;
		final double minZ = fromThisSpot.getZ() - interestRadius;

		final double maxX = fromThisSpot.getX() + interestRadius;
		final double maxY = fromThisSpot.getY() + interestRadius;
		final double maxZ = fromThisSpot.getZ() + interestRadius;

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

	public void populate(int numberOfCells) {
		for (int i = 0; i < numberOfCells; i++) {
			Agent spot = new Agent(this, this.getNewId(), 0, String.valueOf(i + 1), i * 3, 0, 0, this.time);
			this.registerAgent(spot);
		}
		this.commitNewAndDeadAgents();
	}

	public void close() {
		agentsContainer.forEach( (id,spot) -> reportAgentLog(spot) );
		reportFile.close();
	}


	public static void main(String[] args) {
		try {
			Simulator s = new Simulator("/temp/mastodon_plain_text_log1.txt");
			s.populate(2);
			for (int time = 0; time < 10; ++time) s.doOneTime();
			s.close();
		} catch (Exception e) {
			System.out.println("SOME ERROR: "+e.getMessage());
		}
	}
}
