#@File initialMastodonProjectFile
#@int noOfCells
#@int noOfTimepoints
#@Context ctx

from org.mastodon.mamut import MainWindow
from org.mastodon.mamut.io import ProjectLoader
from org.ulman.simulator.ui import Runner

# loads the Mastodon project and shows the Mastodon app
projectModel = ProjectLoader.open(initialMastodonProjectFile.toString(),ctx,True,True)
MainWindow(projectModel).setVisible(True)

# simulates into this existing Mastodon app
r = Runner(projectModel,noOfCells,noOfTimepoints)
r.run()
