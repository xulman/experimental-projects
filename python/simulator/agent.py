class Agent:
    def __init__(self, simulator, ID:int, parentID:int, label:str, x:float, y:float, z:float, time:int):
        self.simulator_frame = simulator

        self.name = label
        self.id = ID
        self.parent_id = parentID

        self.x = x
        self.y = y
        self.z = z
        self.t = time

        self.next_x = x
        self.next_y = y
        self.next_z = z

        # scan around up to this range
        self.interest_radius = 5.0

        # don't get closer to any neighbor than this distance
        self.min_distance_to_neighbor = 3.0
        self.min_distance_squared = self.min_distance_to_neighbor * self.min_distance_to_neighbor

        # how much is a dense environment; doesn't divide if there are
        # more neighbors than 'max_neighbors' in the 'interest_radius'
        self.max_neighbors = 6

        # TODO randomize!
        self.dontDivideBefore = time + 10
        self.dontLiveBeyond = time+50

        # for creating the outcome plain text file
        self.report_log = []
        self.report_status()

        print(f"NEW AGENT {ID} ({label}), parent {parentID} @ [{x},{y},{z}] tp={time}, divTime={self.dontDivideBefore}, dieTime={self.dontLiveBeyond}")


    def report_status(self):
        # TIME	X	Y	Z	TRACK_ID	PARENT_TRACK_ID	SPOT LABEL	SPOT RADIUS
        self.report_log.append(f"{self.t}\t{self.x}\t{self.y}\t{self.z}\t{self.id}\t{self.parent_id}\t{self.name}")


    def progress(self, till_this_time:int):
        first_go = True
        while self.t < till_this_time:
            self.do_one_time(first_go)
            first_go = False

    def progress_finish(self):
        self.x = self.next_x
        self.y = self.next_y
        self.z = self.next_z
        self.report_status()


    def do_one_time(self, from_current_pos = True):
        neighbors = self.simulator_frame.get_list_of_occupied_coords( self )
        print(f"advancing agent id {self.id} ({self.name}):")

        old_x = self.x if from_current_pos else self.next_x
        old_y = self.y if from_current_pos else self.next_y
        old_z = self.z if from_current_pos else self.next_z

        remaining_attempts = 5
        too_close = True
        while remaining_attempts > 0 and too_close:
            remaining_attempts -= 1

            # new potential position of this spot
            #TODO randomize
            new_x = old_x + 5
            new_y = old_y + 4
            new_z = old_z

            # check if not close to any neighbor
            too_close = False
            for nx,ny,nz in neighbors:
                dx = nx-new_x
                dy = ny-new_y
                dz = nz-new_z
                dist = dx*dx + dy*dy + dz*dz
                if dist < self.min_distance_squared:
                    too_close = True

        if not too_close:
            # great, found new acceptable position, let's use it
            self.next_x = new_x
            self.next_y = new_y
            self.next_z = new_z
        # else we stay where we are (which should not break things, provided other agents follow the same protocol)
        self.t += 1 

        print(f"  established coords [{self.next_x},{self.next_y},{self.next_z}] ({remaining_attempts} tries left)")
        print(f"  when {len(neighbors)} neighbors around, too_close={too_close}")

        # soo, we might have moved somewhere,
        # but isn't it a time to divide or die?
        if self.t > self.dontDivideBefore:
            # time to divide if it is not too crowded around
            no_of_neighbors = len(neighbors)
            if no_of_neighbors <= self.max_neighbors:
                # time to divide!
                print("  dividing!")
                self.divide_me()

        elif self.t > self.dontLiveBeyond:
            print("  dying!")
            self.simulator_frame.deregister_agent(self)


    def divide_me(self):
        d1_id = self.simulator_frame.get_new_id()
        d2_id = self.simulator_frame.get_new_id()

        d1_name = self.name+"a"
        d2_name = self.name+"b"

        # TODO randomize
        d1 = Agent(self.simulator_frame, d1_id,self.id, d1_name, self.x-0.5,self.y,self.z, self.t+1)
        d2 = Agent(self.simulator_frame, d2_id,self.id, d2_name, self.x+0.5,self.y,self.z, self.t+1)

        self.simulator_frame.deregister_agent(self)
        self.simulator_frame.register_agent(d1)
        self.simulator_frame.register_agent(d2)
