from agent import Agent

class Simulator:
    def __init__(self, log_file_path:str):
        self.assigned_ids = 0
        self.time = 0

        # for now: a naive simple version with one map
        self.agents_container = dict()

        # temporary buffers for adding and removing agents
        self.new_agents_container = []
        self.dead_agents_container = []

        self.report_file = open(log_file_path,"w")

    def get_new_id(self):
        self.assigned_ids += 1
        return self.assigned_ids


    def register_agent(self, spot:Agent):
        if spot.id in self.agents_container.keys():
            print("========== SIM: ERROR with registering")
            pass
        print(f"========== SIM: registering agent {spot.id}")
        self.new_agents_container.append(spot)

    def deregister_agent(self, spot:Agent):
        if not spot.id in self.agents_container.keys():
            print("========== SIM: ERROR with deregistering")
            pass
        print(f"========== SIM: DEregistering agent {spot.id}")
        self.dead_agents_container.append(spot)

    def commit_new_and_dead_agents(self):
        for spot in self.dead_agents_container:
            self.agents_container.pop(spot.id)
            self.report_agent_log(spot)

        for spot in self.new_agents_container:
            self.agents_container[spot.id] = spot


    def report_agent_log(self, spot:Agent):
        for line in spot.report_log:
            self.report_file.write(line)
            self.report_file.write("\n")
        self.report_file.write("\n\n")


    def get_list_of_occupied_coords(self, from_this_spot:Agent):
        min_x = from_this_spot.x - from_this_spot.interest_radius
        min_y = from_this_spot.y - from_this_spot.interest_radius
        min_z = from_this_spot.z - from_this_spot.interest_radius

        max_x = from_this_spot.x + from_this_spot.interest_radius
        max_y = from_this_spot.y + from_this_spot.interest_radius
        max_z = from_this_spot.z + from_this_spot.interest_radius

        ret_coords = []
        for spot in self.agents_container.values():
            if spot.id == from_this_spot.id:
                continue
            if min_x < spot.x < max_x and min_y < spot.y < max_y and min_z < spot.z < max_z:
                ret_coords.append([spot.x,spot.y,spot.z])

        return ret_coords


    def do_one_time(self):
        self.new_agents_container.clear()
        self.dead_agents_container.clear()

        self.time += 1
        print(f"========== SIM: creating time point {self.time} from {len(self.agents_container)} agents")
        for spot in self.agents_container.values():
            spot.progress(self.time)

        for spot in self.agents_container.values():
            spot.progress_finish()

        self.commit_new_and_dead_agents()


    def populate(self, number_of_cells:int):
        for i in range(number_of_cells):
            spot = Agent(self, self.get_new_id(),0, f"{i+1}", i*3,0,0,self.time)
            self.register_agent(spot)
        self.commit_new_and_dead_agents()


    def close(self):
        for spot in self.agents_container.values():
            self.report_agent_log(spot)
        self.report_file.close()
