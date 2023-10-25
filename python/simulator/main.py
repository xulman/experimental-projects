from simulator import Simulator


sim = Simulator("/temp/mastodon_plain_text_log1.txt")

no_of_cells = 2
sim.populate(no_of_cells)

for cycle in range(200):
    sim.do_one_time()

sim.close()

