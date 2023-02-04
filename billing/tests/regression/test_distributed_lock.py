from dwh.grocery.targets.distributed_lock_target import DistributedLock


class TestLock:

    def setup(self):
        self.lock_a = DistributedLock("//home/balance-test/dev/A", 'A')
        self.lock_b = DistributedLock("//home/balance-test/dev/A", 'B')

    def teardown(self):
        self.lock_a.release()
        self.lock_b.release()

    def test_free(self):
        assert self.lock_a.exists()

    def test_acquire(self):
        self.lock_a.acquire()
        assert self.lock_a.exists()

    def test_conflict(self):
        self.lock_a.acquire()
        assert not self.lock_b.exists()

    def test_release(self):
        self.lock_a.acquire()
        self.lock_a.release()
        assert self.lock_b.exists()
