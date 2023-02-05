#!/usr/bin/python
# coding: utf-8

from itertools import groupby
from operator import itemgetter
import sys

import bottle
from nose import main as runner
import stubout
import opster
from pumper import Event

import yandex.maps.wikimap.pumper_http

#local imports
import tools


class MockPumper(object):
    """ Replace for PumperFirer (for unittesting).
    """
    def __init__(self, *args):
        self.events = []

    def fireEvent(self, event_type, **params):
        e = Event(event_type, **params)
        self.events.append(e)
        return e.get_id()


@opster.command()
def main():
    bottle.debug(True)
    argv = sys.argv[:]
    argv.append("-s")
    runner(argv=argv)


if __name__ == '__main__':
    import warnings
    warnings.simplefilter('error')  # Чтобы видеть варнинги SQL-алхимии

    from WikimapPy.db import set_unittest_mode, init_pool
    from ytools.logging import setup_logging
    import utils
    setup_logging()
    stubs = stubout.StubOutForTesting()
    stubs.Set(yandex.maps.wikimap.pumper_http, 'EventFirer', MockPumper)
    init_pool('social', False)
    set_unittest_mode()
    main()
