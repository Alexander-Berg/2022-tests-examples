from datetime import datetime, timedelta
import random


def get_random_date():
    start = datetime(2000, 1, 1)
    end = datetime(2020, 12, 31)
    delta = end - start
    int_delta = (delta.days * 24 * 60 * 60) + delta.seconds
    random_second = random.randrange(int_delta)
    return start + timedelta(seconds=random_second)
