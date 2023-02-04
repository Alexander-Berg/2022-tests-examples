import irt.init_subclass


class Philosopher(object):
    def __init_subclass__(cls, default_name, **kwargs):
        super(Philosopher, cls).__init_subclass__(**kwargs)
        cls.default_name = default_name


class AustralianPhilosopher(Philosopher, default_name="Bruce"):
    pass


def test_basic():
    irt.init_subclass.suppress_unused()
