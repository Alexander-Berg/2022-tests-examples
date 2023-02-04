# -*- coding: utf-8 -*-
import datetime as dt
import pprint

import time
import contextlib

SENTINEL = object()


def clean_dict(in_dict):
    """
    Delete pairs key-value if value is None
    """
    return {k: v for k, v in in_dict.iteritems() if v is not None}


def print_res(res):
    print dt.datetime.now().strftime('%H:%M:%S.%f'), ': ', pprint.pformat(res).decode('unicode_escape'), '\n'


@contextlib.contextmanager
def timer_ctx(name=''):
    t1 = time.clock()
    yield

    t2 = time.clock()
    print '--- --- ---'
    print 'POINT. Duration %s: %s sec.' % (name, t2 - t1)
    print '--- --- ---'


def timer(name=''):
    def timer_deco(fun):
        def wrapper(*args, **kwargs):
            with timer_ctx(name):
                res = fun(*args, **kwargs)
            return res
        return wrapper
    return timer_deco
