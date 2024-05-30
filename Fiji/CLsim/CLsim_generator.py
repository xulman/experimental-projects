#@File initialMastodonProjectFile
#@Context ctx
#@int numberOfTimepoints
#@int numberOfSpotsPerTimepoint
#@boolean doLinkSpots

#@int reportFromRound
#@int reportEveryNthRound

from org.mastodon.mamut import MainWindow
from org.mastodon.mamut.io import ProjectLoader
from org.ulman.simulator import NonSenseDataGenerator

# loads the Mastodon project and shows the Mastodon app
projectModel = ProjectLoader.open(initialMastodonProjectFile.toString(),ctx,True,True)
MainWindow(projectModel).setVisible(True)
NonSenseDataGenerator(projectModel, numberOfTimepoints, numberOfSpotsPerTimepoint, doLinkSpots, reportFromRound, reportEveryNthRound)
