#@File initialMastodonProjectFile
#@Context ctx

from org.mastodon.mamut import MainWindow
from org.mastodon.mamut.io import ProjectLoader
from org.ulman.simulator.ui import Runner
from org.ulman.simulator import SimulationConfig

# loads the Mastodon project and shows the Mastodon app
projectModel = ProjectLoader.open(initialMastodonProjectFile.toString(),ctx,True,True)
MainWindow(projectModel).setVisible(True)


# adjusts the simulation configuration (that's an optional step)
simCfg = SimulationConfig()

# docs and default parameters:
# ============================

# If the _B,_W,_BW indicators should be prepended or appended to the spot label.
simCfg.PREPEND_HINT_LABELS = true;

# Collect internal status info per every Agent. If not, may speed up the simulation as no extra data will be stored.
simCfg.COLLECT_INTERNAL_DATA = false;

# Prints a lot of data to understand decisions making of the agents.
simCfg.VERBOSE_AGENT_DEBUG = false;

# Prints relative little reports about what the simulation framework was asked to do.
simCfg.VERBOSE_SIMULATOR_DEBUG = false;

# How far around shall an agent look for \"nearby\" agents to consider them for overlaps.
simCfg.AGENT_SEARCH_RADIUS = 5.0;

# How close two agents can come before they are considered overlapping.
simCfg.AGENT_MIN_DISTANCE_TO_ANOTHER_AGENT = 3.0;

# How far an agent can move between time points.
simCfg.AGENT_USUAL_STEP_SIZE = 1.0;

# How many attempts is an agent (cell) allowed to try to move randomly until it finds an non-colliding position.
simCfg.AGENT_NUMBER_OF_ATTEMPTS_TO_MAKE_A_MOVE = 6;

# The mean life span of an agent (cell). Shorted means divisions occurs more often.
simCfg.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 7;

# Hard limit on the life span of an agent (cell). The cell dies, is removed from the simulation, whenever it's life exceeded this value.
simCfg.AGENT_MAX_LIFESPAN_AND_DIES_AFTER = 30;

# The maximum number of neighbors tolerated for a division to occur; if more neighbors are around, the system believes the space is too condensed and doesn't permit agents (cells) to divide.
simCfg.AGENT_MAX_DENSITY_TO_ENABLE_DIVISION = 4;

# Given the last move of a mother cell, project it onto an xy-plane, one can then imagine a perpendicular line in the xy-plane. A division line in the xy-plane is randomly picked such that it does not coincide by larger angle with that perpendicular line, and this random line would be a \"division\" orientation for the x,y coords, the z-coord is randomized.
simCfg.AGENT_MAX_VARIABILITY_FROM_A_PERPENDICULAR_DIVISION_PLANE = 3.14;

# Freshly \"born\" daughters are placed exactly this distance apart from one another.
simCfg.AGENT_DAUGHTERS_INITIAL_DISTANCE = 1.6;

# Using this radius the new spots are introduced into Mastodon.
simCfg.MASTODON_SPOT_RADIUS = 2.0;

# Produce a \"lineage\" that stays in the geometric centre of the generated data.
simCfg.MASTODON_CENTER_SPOT = false;

# Controls if the agents are allowed to move in z-axis at all.
simCfg.AGENT_DO_2D_MOVES_ONLY = True



# now override some of the params with my own
# ============================
simCfg.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 10


# simulates into this existing Mastodon app,
# assumes empty project and in any case starts from timepoint = 0

# provide own number of cells and length of this simulation run
noOfCells = 10
noOfTimepoints = 80

r = Runner(projectModel,noOfCells,noOfTimepoints)
r.changeConfigTo(simCfg)
r.run()

# simulates again into the very same existing Mastodon app,
# but here the Runner will run simulation from the last non-empty timpoint,
# thus continuing on where the last run ended;
# continues with the same config, which has been further adjusted

# provide own number of cells and length of this simulation run
noOfTimepoints = 40

r = Runner(projectModel,noOfTimepoints)
simCfg.AGENT_DO_2D_MOVES_ONLY = False
r.changeConfigTo(simCfg)
r.run()


# if one wants a third stage of the simulation, the last pattern shall be put here, e.g.:
# noOfTimepoints = 20
# r = Runner(projectModel,noOfTimepoints)
# simCfg.AGENT_DO_2D_MOVES_ONLY = False
# r.changeConfigTo(simCfg)
# r.run()
