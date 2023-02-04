from requests import Timeout


class MdsClient:

    def __init__(self, mds_map: dict, bad_set: set = None, timeouts: int = 0):
        self.mds_map = dict()
        if mds_map is not None:
            self.mds_map.update(mds_map)
        self.bad_set = set()
        if bad_set is not None:
            self.bad_set.update(bad_set)

        self.timeouts = dict()
        for mds_key in mds_map.keys():
            self.timeouts[mds_key] = timeouts

    def generate_rbtorrent(self, filename: str, mds_key: str) -> str:
        if mds_key in self.timeouts:
            if self.timeouts[mds_key] > 0:
                self.timeouts[mds_key] -= 1
                raise Timeout()
        if mds_key in self.bad_set:
            raise Exception()
        return self.mds_map[mds_key]
