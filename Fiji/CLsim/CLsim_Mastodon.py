#@File initialMastodonProjectFile
#@int noOfCells
#@int noOfTimepoints
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
simCfg.AGENT_DO_2D_MOVES_ONLY = True
simCfg.AGENT_AVERAGE_LIFESPAN_BEFORE_DIVISION = 10

# simulates into this existing Mastodon app,
# assumes empty project and in any case starts from timepoint = 0
r = Runner(projectModel,noOfCells,noOfTimepoints)
r.changeConfigTo(simCfg)
r.run()

# simulates again into the very same existing Mastodon app,
# but here the Runner will run simulation from the last non-empty timpoint,
# thus continuing on where the last run ended;
# continues with the same config, which has been further adjusted
r = Runner(projectModel,noOfTimepoints)
simCfg.AGENT_DO_2D_MOVES_ONLY = False
r.changeConfigTo(simCfg)
r.run()
