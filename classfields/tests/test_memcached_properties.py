# -*- coding: utf-8 -*-
import os

import pytest

from tests import TestAPI, get_testdata_config
from vertis.memcached.multiple import MemcachedProperties
from vertis.memcached.multiple.scripts.generator import expand_group, main


class TestProperties(TestAPI):

    def test_memcached_properties(self):
        memconfig = self.build_config('standart.cfg')

        memproperty = MemcachedProperties(
            memconfig.port, outfile=memconfig.outfile
        )

    def test_properties_flush(self):
        cfgdir = os.path.dirname(get_testdata_config('standart.cfg'))

        with pytest.raises(SystemExit) as e:
            main(cfgdir=cfgdir)
