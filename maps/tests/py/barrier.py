import threading


class Barrier:
    class Timeout(Exception):
        pass

    def __init__(self, parties=2, timeout=None):
        self.parties = parties
        self._count = parties
        self._generation = 0
        self._condition = threading.Condition()
        self._timeout = timeout

    def wait(self, timeout=None):
        with self._condition:
            generation = self._generation

            self._count -= 1
            if (self._count == 0):
                self._generation += 1
                self._count = self.parties
                self._condition.notify_all()
            else:
                self._condition.wait(
                    timeout if timeout is not None else self._timeout)
                if generation == self._generation:
                    self._count += 1
                    raise Barrier.Timeout()
