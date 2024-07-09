#@ Context ctx
#@File initialMastodonProjectFile

#@int fill_this_timepoint = 0
#@int(value=7) spots_radius = 7

#@double x_centre = 0
#@double y_centre = 0
#@double z_centre = 0

#@double x_step_size = 10
#@double y_step_size = 10
#@double z_step_size = 10

#@int x_num_steps = 13
#@int y_num_steps = 13
#@int z_num_steps = 13

#@boolean fill_cube = False


from org.mastodon.mamut.io import ProjectLoader
from org.mastodon.mamut import MainWindow

p = ProjectLoader.open(initialMastodonProjectFile.toString(), ctx)
MainWindow(p).setVisible(True)

for x in range(-x_num_steps, x_num_steps+1):
    for y in range(-y_num_steps, y_num_steps+1):
        for z in range(-z_num_steps, z_num_steps+1):
            # if sufficiently close to the cube border (or just filling everywhere), we place a spot:
            if fill_cube \
                or x == -x_num_steps \
                or y == -y_num_steps \
                or z == -z_num_steps \
                or x ==  x_num_steps \
                or y ==  y_num_steps \
                or z ==  z_num_steps:
                pos = [x*x_step_size + x_centre,  y*y_step_size + y_centre,  z*z_step_size + z_centre]
                p.getModel().getGraph().addVertex().init(fill_this_timepoint, pos, spots_radius)


print("done adding spots")
