import os
import json
import math
import time
import logging
import subprocess


class Apt(object):
    def __init__(self):
        self._update = False

    def update(self, force=False):
        if force or not self._update:
            run(['apt-get', 'update'])
            self._update = True

    def install(self, *args, check=True):
        self.update()
        env = os.environ.copy()
        env['DEBIAN_FRONTEND'] = "noninteractive"
        return run(['apt-get', 'install', '--yes', '--no-install-recommends'] + list(args), env=env, check=check)

    def remove(self, *args, check=True):
        env = os.environ.copy()
        env['DEBIAN_FRONTEND'] = "noninteractive"
        return run(['apt-get', 'remove', '--yes'] + list(args), env=env, check=check)

    def policy(self, *args, check=True):
        self.update()
        return run(['apt-cache', 'policy'] + list(args), check=check)


class Timer(object):
    def __init__(self, min_time=None, max_time=None):
        self.min_time = min_time
        self.max_time = max_time

    def __enter__(self):
        self.start = time.monotonic()
        return self

    def __exit__(self, exc, value, tb):
        time_spent = time.monotonic() - self.start
        logging.debug("time_spent %s", time_spent)
        if self.min_time is not None and time_spent < self.min_time:
            raise Exception("time_spent {} less than expected min_time {}".format(time_spent, self.min_time))
        if self.max_time is not None and time_spent > self.max_time:
            raise Exception("time_spent {} more than expected max_time {}".format(time_spent, self.max_time))


def run(args, logger=None, check=True, text=False, stdin=subprocess.DEVNULL, **kwargs):
    logger = logger or logging.getLogger()
    logger.info("run '%s'", "' '".join(args))
    # should be compatible with python 3.5 from xenial
    ret = subprocess.run(args, check=check, universal_newlines=text, stdin=stdin, **kwargs)
    return ret


def run_output(args, text=True, **kwargs):
    return run(args, stdout=subprocess.PIPE, text=text, **kwargs).stdout


def read_str(fn, default=None):
    if default is not None and not os.path.exists(fn):
        return default
    with open(fn) as f:
        return f.read().strip()


def read_str_list(fn, default=None):
    s = read_str(fn, default)
    return s.split()


def write_str(fn, val):
    with open(fn, 'w') as f:
        return f.write(str(val))


def read_int(fn, default=None):
    return int(read_str(fn, default))


def read_int_list(fn, default=None):
    return list(map(int, read_str_list(fn, default)))


def parse_size(val):
    if isinstance(val, str):
        u = 'bkmgtpe'.find(val[-1:].lower())
        if u >= 0:
            return int(val[:-1]) * (1024 ** u)
    return int(val)


def parse_bitmap_list(bitmap):
    res = []
    for word in bitmap.split(','):
        w = word.split('-')
        if len(w) > 1:
            res += range(int(w[0]), int(w[1])+1)
        else:
            res += [int(w[0])]
    return res


def pop_unit(cfg, name, unit=None, default=None):
    """
        unit=[<>]K|M|G|T[i]
        <  - round down
        >  - round up
        K  - 1000
        Ki - 1024
    """
    units = {'K': 10 ** 3,
             'M': 10 ** 6,
             'G': 10 ** 9,
             'T': 10 ** 12,
             'Ki': 2 ** 10,
             'Mi': 2 ** 20,
             'Gi': 2 ** 30,
             'Ti': 3 ** 40,
             'KiB': 2 ** 10,
             'MiB': 2 ** 20,
             'GiB': 2 ** 30,
             'TiB': 3 ** 40,
             'm' : 10e-3,
             'u' : 10e-6,
             'n' : 10e-9,
             'ms' : 10e-3,
             'us' : 10e-6,
             'ns' : 10e-9}

    # name = 2 ** 20
    val = cfg.pop(name, None)

    # name = "1Mi"
    if isinstance(val, str):
        for n, u in units.items():
            if val.endswith(n):
                val = int(val[:-len(n)]) * u
                break

    # name_Mi = 1
    for n, u in units.items():
        v = cfg.pop(name + '_' + n, None)
        if v is not None:
            val = v * u

    if val is None:
        val = default
    elif unit:
        if unit[0] == '<':
            val = math.floor(val / units[unit[1:]])
        elif unit[0] == '>':
            val = math.ceil(val / units[unit[1:]])
        else:
            val = val / units[unit]

    return val


def pretty_json(obj):
    return json.dumps(obj, default=lambda obj: obj.__dict__,
                      sort_keys=True, indent=4, separators=(',', ': '))
