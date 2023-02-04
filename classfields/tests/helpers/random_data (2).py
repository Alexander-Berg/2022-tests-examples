import random
import string


def random_lower_string(length: int = 32) -> str:
    return "".join(random.choices(string.ascii_lowercase, k=length))


def random_small_integer() -> int:
    return random_integer(0, 99)


def random_integer(lower: int = 0, upper: int = 10000000) -> int:
    return random.randint(lower, upper)
