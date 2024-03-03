#@File outputResultMastodonProjectFile
#@int noOfCells
#@int noOfTimepoints

from org.ulman.simulator.ui import Runner

# brand new project, starts from scratch & saves, doesn't need any Mastodon app to be around
r = Runner(outputResultMastodonProjectFile.toString(),noOfCells,noOfTimepoints)
r.run()
