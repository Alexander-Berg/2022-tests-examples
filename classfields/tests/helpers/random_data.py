import os
import random
import string

_rnd = random.SystemRandom()


def random_lower_string(length=32) -> str:
    return "".join(_rnd.choices(string.ascii_lowercase, k=length))


def random_small_integer() -> int:
    return random_integer(0, 99)


def random_integer(lower=0, upper=10000000) -> int:
    return _rnd.randint(lower, upper)


def random_float(lower=0, upper=10000000) -> float:
    assert lower > 0
    return lower + _rnd.random() * (upper - lower)


def md5() -> str:
    return "".join(map("{:02x}".format, os.urandom(16)))
