#@File outputResultMastodonProjectFile
#@int noOfCells
#@int noOfTimepoints

from org.ulman.simulator.ui import Runner
from org.ulman.simulator import SimulationConfig

# brand new project, starts from scratch & saves, doesn't need any Mastodon app to be around;
# adjusts the simulation configuration to the default config (that's an optional step)
r = Runner(outputResultMastodonProjectFile.toString(),noOfCells,noOfTimepoints)
r.changeConfigTo( SimulationConfig() )
r.run()
