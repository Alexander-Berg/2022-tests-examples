class TorrentDatabase:

    def __init__(self, initial: dict, initial_mds_rbtorrent: dict, digests_mds: dict, write_error=None):
        self.digests_mds = digests_mds

        self.initial_mds = dict()
        if initial_mds_rbtorrent is not None:
            self.initial_mds.update(initial_mds_rbtorrent)

        self.digest_map = dict()
        if initial is not None:
            self.digest_map.update(initial)
        self.brewed = dict()
        self.dropped = set()
        self.pop_errors = 0
        self.write_error = write_error

    def pop_queue(self, lock_timeout=300, count=10) -> list:
        if self.write_error is not None:
            raise self.write_error
        if self.pop_errors > 0:
            self.pop_errors -= 1
            raise Exception("Pop Error")
        result = list()
        for digest in self.digest_map.keys():
            if self.digest_map[digest] is None:
                result.append(digest)
            if len(result) >= count:
                return result
        return result

    def check_role(self, repo: str, user: str, role: str) -> bool:
        if user == 'nobody':
            return False
        if user == 'error':
            raise Exception("Some Database error")
        if 'secret' in repo:
            return False
        return True

    def repo_has_role(self, repo: str, role: str) -> bool:
        if 'secret' in repo:
            return False
        return True

    def drop_mapping(self, digest: str) -> None:
        if self.write_error is not None:
            raise self.write_error
        self.digest_map.pop(digest)
        self.dropped.add(digest)

    def search_old_mds_key_mapping(self, mds_keys: list) -> dict:
        result = dict()
        for mds_key in mds_keys:
            if mds_key in self.initial_mds:
                result[mds_key] = self.initial_mds[mds_key]
        return result

    def search_digest_mapping(self, layers_digest: list) -> dict:
        result = dict()
        for digest in layers_digest:
            if digest in self.digest_map:
                result[digest] = self.digest_map[digest]
        return result

    def get_queued(self, offset: int = 0, limit: int = 1000) -> (list, int):
        pass

    def get_brewing(self, offset: int = 0, limit: int = 1000) -> (list, int):
        pass

    def put_queue_brew(self, digest: str) -> None:
        if self.write_error is not None:
            raise self.write_error
        if digest in self.digest_map:
            raise Exception('Dub key')
        self.digest_map[digest] = None

    def new_mapping(self, digest: str, rbtorrent_id: str) -> None:
        if self.write_error is not None:
            raise self.write_error
        if digest in self.digest_map:
            raise Exception('Dub key')
        self.digest_map[digest] = rbtorrent_id

    def update_mapping(self, digest: str, rbtorrent_id: str) -> None:
        if self.write_error is not None:
            raise self.write_error
        if digest in self.digest_map:
            self.digest_map[digest] = rbtorrent_id
            self.brewed[digest] = rbtorrent_id
        else:
            raise Exception('No key')

    def get_mds_keys(self, digests) -> dict:
        result = dict()
        for digest in digests:
            mds_key = self.digests_mds[digest]
            if mds_key is None:
                raise Exception('Empty value')
            result[digest] = mds_key
        return result
