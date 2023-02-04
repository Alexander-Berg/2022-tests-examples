# -*- coding: utf-8 -*-
import os

from vertis.memcached.multiple import MemcachedConfig
from vertis.memcached.multiple.utils.misc import parse_cfg
from vertis.memcached.multiple.utils.logger import logger


class TestAPI(object):
    """
    Base class for testing application. Do checks to make sure database
    is clean and provide some helper methods for easy accessing API
    methods.
    """

    @classmethod
    def setup_class(cls):
        logger.disabled = True

    def build_config(self, cfgname):
        config = get_testdata_config(cfgname)
        assert os.path.exists(config)

        mapping = parse_cfg(config)
        memconfig = MemcachedConfig.create(mapping)

        #: Assert is not required, but explicit is better than implicit
        assert memconfig

        return memconfig

def get_testdata_config(cfgname):
    """
    Return absolute path for the example config which will be used
    also in tests (it guarantees that example config will be always
    in actual state).
    """
    basedir = os.path.dirname(__file__)
    config = os.path.join(basedir, 'testdata', cfgname)

    return os.path.abspath(config)