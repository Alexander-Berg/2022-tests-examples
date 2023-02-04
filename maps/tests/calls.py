import functools


class Calls(object):
    def __init__(self):
        self.calls = {}

    def __getitem__(self, fn):
        return self.calls.get(fn.__name__, 0)

    def __setitem__(self, fn, value):
        self.calls[fn.__name__] = value

    def clear(self):
        self.calls.clear()


CALLS = Calls()


def register_call(fn):
    @functools.wraps(fn)
    def wrapped(*args, **kwargs):
        CALLS[fn] += 1
        return fn(*args, **kwargs)
    return wrapped
