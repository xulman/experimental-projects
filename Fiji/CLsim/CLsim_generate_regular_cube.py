#@ Context ctx
#@File initialMastodonProjectFile

#@int start_in_this_timepoint = 0
#@int advance_timepoint_after_this_number_of_spots = 10000
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
#@boolean randomize_positions = False
#@boolean rotate_cube = False
#@boolean(label="Color layers using the FIRST listed tag set") use_colors = False


from org.mastodon.mamut.io import ProjectLoader
from org.mastodon.mamut import MainWindow
import random
import math

p = ProjectLoader.open(initialMastodonProjectFile.toString(), ctx)
print("started, please wait...")

tagMap = None
tags = None
if use_colors:
    tagSet = p.getModel().getTagSetModel().getTagSetStructure().getTagSets().get(0)
    tagMap = p.getModel().getTagSetModel().getVertexTags().tags(tagSet)
    tags = tagSet.getTags()


ang_rad = 3.14159 * 9.0 / 180.0
cos_ang = math.cos(ang_rad)
sin_ang = math.sin(ang_rad)

def rotate_vec(pos):
    x = pos[0]
    y = pos[1]
    pos[0] = x*cos_ang - y*sin_ang
    pos[1] = x*sin_ang + y*cos_ang


spots_cnt = 0

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
                if randomize_positions:
                    pos[0] += random.randint(1,20) / 40.0 * x_step_size
                    pos[1] += random.randint(1,20) / 40.0 * y_step_size
                    pos[2] += random.randint(1,20) / 40.0 * z_step_size
                if rotate_cube:
                    rotate_vec(pos)
                spot = p.getModel().getGraph().addVertex()
                spot.init(start_in_this_timepoint, pos, spots_radius)
                if use_colors:
                    layer = max(abs(x), max(abs(y),abs(z))) % len(tags)
                    tagMap.set(spot, tags[layer])
                spots_cnt += 1
                if spots_cnt == advance_timepoint_after_this_number_of_spots:
                    spots_cnt = 0
                    start_in_this_timepoint += 1


print("done adding spots")
MainWindow(p).setVisible(True)
