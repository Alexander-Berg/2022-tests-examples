import datetime
import random
import string
import time
import uuid


def uuid_str() -> str:
    return str(uuid.uuid4())


def date(lower_bound=None, upper_bound=None) -> datetime.datetime:
    lower_bound = lower_bound or time.time()
    upper_bound = upper_bound or lower_bound * 1.5
    return datetime.datetime.fromtimestamp(random.randint(int(lower_bound), int(upper_bound)))


def int_(lower_bound=None, upper_bound=None):
    return random.randint(lower_bound or 0, upper_bound or 999999999)


def float_(lower_bound=None, upper_bound=None):
    return random.uniform(lower_bound or 0, upper_bound or 999999999)


def str_(length=16):
    return "".join(random.choices(string.ascii_lowercase, k=length))
