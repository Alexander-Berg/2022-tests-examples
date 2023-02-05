#!/usr/bin/env python
import unittest
import sys
import yutil

from ytools.logging import setup_logging
from ytools.config import Config

import warnings
warnings.simplefilter('ignore', Warning)

if __name__ == '__main__':
    setup_logging()
    Config()

    from WikimapPy.db import set_unittest_mode, init_pool
    init_pool('social', False)
    set_unittest_mode()

    argv = sys.argv[1:]

    try:
        from nose import main as runner

        argv += ('--with-doctest', '-s')
        runner(argv=argv)
    except ImportError:
        sys.stderr.write("WARNING, I can't find python-nose, please, install this package.\n\n")
        sys.exit(1)

