import logging

import libraries.hardware as hardware
import libraries.hosts_renaming as hosts_renaming
import libraries.json_diff as json_diff
import libraries.online_state as online_state
import libraries.utils as utils
import libraries.topology as topology


def main():
    logging.basicConfig(level=logging.DEBUG)

    print dir(hardware)
    print dir(hosts_renaming)
    print dir(json_diff)
    print dir(online_state)
    print dir(utils)
    print dir(topology)

    hardware.load_singletons()
