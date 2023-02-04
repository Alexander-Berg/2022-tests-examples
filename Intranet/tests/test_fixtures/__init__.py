import random


def random_name():
    return ('test-%s' % random.random()).replace('.', '')

