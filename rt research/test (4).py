import pytest

from irt.init_subclass import suppress_unused


class NoSuperClass:
    subclasses = []

    def __init__(self):
        pass

    @classmethod
    def __init_subclass__(cls, **kwargs):
        NoSuperClass.subclasses.append(cls.__name__)


class SubNoSuperClass(NoSuperClass):
    pass


class NotCallable(object):
    __init_subclass__ = None


with pytest.raises(TypeError):
    class ChildNotCallable(NotCallable):
        pass


class RaiseResult(object):
    @classmethod
    def __init_subclass__(cls, **kwargs):
        raise ValueError('Just test')


with pytest.raises(ValueError):
    class ChildRaiseResult(RaiseResult):
        pass


class Philosopher(object):
    subclasses = []

    def __init_subclass__(cls, **kwargs):
        Philosopher.subclasses.append(cls.__name__)


class Mathematicians(object):
    subclasses = []

    @classmethod
    def __init_subclass__(cls, **kwargs):
        Mathematicians.subclasses.append(cls.__name__)


class Socrates(Philosopher):
    pass


class Euclid(Mathematicians):
    pass


def test_basic():
    suppress_unused()

    NoSuperClass()
    SubNoSuperClass()

    assert Philosopher.subclasses == ['Socrates']
    assert Mathematicians.subclasses == ['Euclid']

    class Plato(Socrates):
        pass

    class Kolmogorov(Mathematicians):
        pass

    assert Philosopher.subclasses == ['Socrates', 'Plato']
    assert Mathematicians.subclasses == ['Euclid', 'Kolmogorov']

    locke = type(
        'Locke',
        (Philosopher,),
        {}
    )

    args = ('Arnold', (Mathematicians,), {})
    kwargs = {}

    arnold = type(*args, **kwargs)

    assert Philosopher.subclasses == ['Socrates', 'Plato', locke.__name__]
    assert Mathematicians.subclasses == ['Euclid', 'Kolmogorov', arnold.__name__]

    class Pythagoras(Mathematicians):
        subclasses = []

        @classmethod
        def __init_subclass__(cls, **kwargs):
            Pythagoras.subclasses.append(cls.__name__)

    assert Mathematicians.subclasses == ['Euclid', 'Kolmogorov', 'Arnold', 'Pythagoras']
    assert Pythagoras.subclasses == []

    class Aristotle(Pythagoras):
        pass

    assert Mathematicians.subclasses == ['Euclid', 'Kolmogorov', 'Arnold', 'Pythagoras']
    assert Pythagoras.subclasses == ['Aristotle']

    class Heraclides(Aristotle):
        pass

    class Eudemus(Aristotle):
        pass

    class Dicaearchus(Aristotle):
        pass

    assert Mathematicians.subclasses == ['Euclid', 'Kolmogorov', 'Arnold', 'Pythagoras']
    assert Pythagoras.subclasses == ['Aristotle', 'Heraclides', 'Eudemus', 'Dicaearchus']
