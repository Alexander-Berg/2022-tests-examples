import threading


class AtomicCounter:
    """ Counter with internal lock, allowing it to use it in many threads.
    """
    def __init__(self, start=0):
        self._value = start
        self._lock = threading.Lock()

    def __iadd__(self, value):
        with self._lock:
            self._value += value

    def inc(self):
        """ Increment the counter by 1. """
        self += 1

    def increment_if(self, condition, value=1):
        """ condition is a unary predicate, which takes current value
            and tells if increment is needed.
            Returns if the counter has been incremented.
        """
        with self._lock:
            has_incremented = condition(self._value)
            if has_incremented:
                self._value += value
        return has_incremented

    def dec(self):
        """ Decrement the counter by 1. """
        self += (-1)

    def value(self):
        """ Current counter value. """
        with self._lock:
            return self._value

    def __int__(self):
        """ Synomym for self.value. Used like int(counter).
            Could not find standard mentioning for this but it works. """
        return self.value()
