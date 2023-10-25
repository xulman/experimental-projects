from simulator import Simulator


sim = Simulator("/temp/mastodon_plain_text_log1.txt")

no_of_cells = 1
sim.populate(no_of_cells)

sim.do_one_time()
sim.do_one_time()
sim.do_one_time()
sim.do_one_time()
sim.do_one_time()

sim.close()

