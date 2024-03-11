#@File initialMastodonProjectFile
#@Context ctx
#@int numberOfTimepoints
#@int numberOfSpotsPerTimepoint
#@boolean doLinkSpots

from org.mastodon.mamut import MainWindow
from org.mastodon.mamut.io import ProjectLoader
from org.ulman.simulator.ui import NonSenseDataGenerator

# loads the Mastodon project and shows the Mastodon app
projectModel = ProjectLoader.open(initialMastodonProjectFile.toString(),ctx,True,True)
MainWindow(projectModel).setVisible(True)
NonSenseDataGenerator(projectModel, numberOfTimepoints, numberOfSpotsPerTimepoint, doLinkSpots)
