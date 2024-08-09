#@ Context ctx
#@File initialMastodonProjectFile

#@int fill_from_this_timepoint = 0
#@int(value=7) spots_radius = 7

#@double x_step_size = 10
#@double y_step_size = 10
#@double z_step_size = 10

#@int(label="number of triaxial growth cycles", min="0") num_of_full_generations = 2
max_conducted_divisions = 3 * num_of_full_generations

#@boolean(label="Color generations using the FIRST listed tag set") use_colors = False
#@boolean(label="Mark intermediate divisions using the SECOND tag set") mark_intermediate = False
#@boolean add_centre_spot = True


from org.mastodon.mamut.io import ProjectLoader
from org.mastodon.mamut import MainWindow
import math

p = ProjectLoader.open(initialMastodonProjectFile.toString(), ctx)
print("started, please wait...")

pixel_data_dimensions = p.getSharedBdvData().getSpimData().getSequenceDescription().getViewSetups().get(0).getSize().dimensionsAsLongArray()
x_centre = pixel_data_dimensions[0] // 2
y_centre = pixel_data_dimensions[1] // 2
z_centre = pixel_data_dimensions[2] // 2 if len(pixel_data_dimensions) > 2 else 0
print("Starting from ["+str(x_centre)+","+str(y_centre)+","+str(z_centre)+"] from TP="+str(fill_from_this_timepoint))


tagMap = None
tags = None
if use_colors:
    tagSet = p.getModel().getTagSetModel().getTagSetStructure().getTagSets().get(0)
    tagMap = p.getModel().getTagSetModel().getVertexTags().tags(tagSet)
    tags = tagSet.getTags()

tagIMap = None
tagsI = None
if mark_intermediate:
    tagSet = p.getModel().getTagSetModel().getTagSetStructure().getTagSets().get(1)
    tagIMap = p.getModel().getTagSetModel().getVertexTags().tags(tagSet)
    tagsI = tagSet.getTags()


direction_masks = [ [1,0,0], [0,1,0], [0,0,1] ]

def divide_spot(mother_spot, current_position, current_age, division_direction):
    remaining_generations = max_conducted_divisions - current_age
    if remaining_generations == 0:
        return

    # compensate for the fact that the division directions are alternating in x,y,z axes
    remaining_generations = int( math.ceil(remaining_generations / 3.0) )

    # how many ancestors will have each of my daughters
    grid_positions_needed = 1 << (remaining_generations-1)
    # and position my daughter into the middle
    grid_positions_needed /= 2.0

    current_mask = direction_masks[division_direction]
    #print(current_mask, current_position)
    pos = [ current_position[0] + grid_positions_needed * x_step_size * current_mask[0], \
            current_position[1] + grid_positions_needed * y_step_size * current_mask[1], \
            current_position[2] + grid_positions_needed * z_step_size * current_mask[2] ]
    spot = p.getModel().getGraph().addVertex()
    spot.init(fill_from_this_timepoint + current_age+1, pos, spots_radius)
    # link to mother
    p.getModel().getGraph().addEdge(mother_spot,spot).init()
    if use_colors:
        layer = ((current_age//3)+1) % len(tags)
        tagMap.set(spot, tags[layer])
    if mark_intermediate and division_direction < 2:
        tagIMap.set(spot, tagsI[0])
    #
    divide_spot(spot, pos, current_age+1, (division_direction+1)%3)


    pos = [ current_position[0] - grid_positions_needed * x_step_size * current_mask[0], \
            current_position[1] - grid_positions_needed * y_step_size * current_mask[1], \
            current_position[2] - grid_positions_needed * z_step_size * current_mask[2] ]
    spot = p.getModel().getGraph().addVertex()
    spot.init(fill_from_this_timepoint + current_age+1, pos, spots_radius)
    # link to mother
    p.getModel().getGraph().addEdge(mother_spot,spot).init()
    if use_colors:
        layer = ((current_age//3)+1) % len(tags)
        tagMap.set(spot, tags[layer])
    if mark_intermediate and division_direction < 2:
        tagIMap.set(spot, tagsI[0])
    #
    divide_spot(spot, pos, current_age+1, (division_direction+1)%3)


# introduce the very first spot, start "growing"
pos = [x_centre, y_centre, z_centre]
spot = p.getModel().getGraph().addVertex()
spot.init(fill_from_this_timepoint, pos, spots_radius)
if use_colors:
    tagMap.set(spot, tags[0])
divide_spot(spot, pos, 0, 0)

print("done adding spots")

if add_centre_spot:
    pos = [x_centre, y_centre, z_centre]
    prev_spot = None

    for t in range(fill_from_this_timepoint, fill_from_this_timepoint+max_conducted_divisions+1):
        spot = p.getModel().getGraph().addVertex()
        spot.init(t, pos, spots_radius)
        spot.setLabel("centre")

        if prev_spot is not None:
            p.getModel().getGraph().addEdge(prev_spot,spot).init()
        prev_spot = spot

    print("done adding centre(s)")

MainWindow(p).setVisible(True)
